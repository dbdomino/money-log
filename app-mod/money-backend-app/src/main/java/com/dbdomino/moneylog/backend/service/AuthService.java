package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.FindPasswordRequest;
import com.dbdomino.moneylog.backend.dto.request.LoginRequest;
import com.dbdomino.moneylog.backend.dto.request.MemberFieldRules;
import com.dbdomino.moneylog.backend.dto.request.ResetPasswordRequest;
import com.dbdomino.moneylog.backend.dto.request.SignupRequest;
import com.dbdomino.moneylog.backend.dto.response.FindIdResponse;
import com.dbdomino.moneylog.backend.dto.response.FindPasswordResponse;
import com.dbdomino.moneylog.backend.dto.response.LoginResponse;
import com.dbdomino.moneylog.backend.dto.response.MessageResponse;
import com.dbdomino.moneylog.backend.dto.response.SignupResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenValidateResponse;
import com.dbdomino.moneylog.backend.mapper.MemberMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.MemberSessionService.IssuedTokens;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserSession;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 유스케이스 — 로그인(1.3)·검증(1.4)·갱신(1.5)·폐기(1.6).
 *
 * <p>이 클래스는 <b>트랜잭션 경계를 스스로 열지 않는다</b>. 세션 조작은
 * {@link MemberSessionService} 가 자기 트랜잭션에서 처리하고, 이력은
 * {@link LoginHistoryService} 가 따로 남긴다. 로그인이 통째로 한 트랜잭션이면
 * 유니크 위반을 잡아도 그 트랜잭션은 이미 롤백 표시가 붙어 재시도가 커밋되지 않는다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md</a>
 */
@Service
public class AuthService {

    /** 세션 삽입이 유니크 위반으로 실패했을 때의 재시도 횟수. */
    private static final int SESSION_ISSUE_RETRIES = 1;

    private final UserRepository userRepository;
    private final MemberSessionService sessionService;
    private final LoginHistoryService loginHistoryService;
    private final PasswordEncoder passwordEncoder;
    private final DefaultExpendGroupService defaultExpendGroupService;
    private final MemberMapper memberMapper;

    public AuthService(UserRepository userRepository,
                       MemberSessionService sessionService,
                       LoginHistoryService loginHistoryService,
                       PasswordEncoder passwordEncoder,
                       DefaultExpendGroupService defaultExpendGroupService,
                       MemberMapper memberMapper) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.loginHistoryService = loginHistoryService;
        this.passwordEncoder = passwordEncoder;
        this.defaultExpendGroupService = defaultExpendGroupService;
        this.memberMapper = memberMapper;
    }

    /**
     * 1.2 회원가입.
     *
     * <p>검증 순서가 곧 응답 코드다 — 확인 불일치 {@code 2005}, 비밀번호 규칙
     * {@code 2004}, 아이디 중복 {@code 2002}, 이메일 중복 {@code 2003}. 형식 오류는
     * Bean Validation 이 걸러 {@code 9001} 로 나간다.
     *
     * <p><b>중복은 선검사와 유니크 위반 처리를 양쪽 다 둔다.</b> 선검사만으로는 두 요청이
     * 같은 순간 "없음"을 보는 창을 닫지 못한다 — DB 의 유니크 제약이 최종 방어선이고,
     * 그 위반을 잡아 같은 코드로 바꿔 준다.
     *
     * <p>권한은 {@code 3} 고정이다. 요청에 {@code role} 필드가 없어 지정할 방법 자체가
     * 없다(FR-105).
     *
     * <p>만든 {@code tbl_user} 행의 {@code created_by}/{@code updated_by} 는
     * <b>{@code null}</b> 이다 — 가입은 자기 자신을 만드는 행위라 INSERT 시점에 자기
     * {@code id_key} 가 없고, 그래서 이 테이블만 두 컬럼이 nullable 이다(FR-121).
     *
     * <p>기본 지출유형 10종을 <b>같은 트랜잭션에서</b> 만든다. 갈라 두면 "회원은 생겼는데
     * 유형이 없는" 상태가 가능해진다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (!MemberFieldRules.isValidPassword(request.password())) {
            throw new BusinessException(ErrorCode.PASSWORD_RULE_VIOLATION);
        }
        if (userRepository.existsByUserId(request.memberId())) {
            throw new BusinessException(ErrorCode.MEMBER_ID_DUPLICATED);
        }
        String email = blankToNull(request.email());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        User user = new User();
        user.setUserId(request.memberId());
        user.setPw(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        user.setIntro(blankToNull(request.intro()));
        user.setRole(User.ROLE_MEMBER);
        user.setActive(true);

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 선검사를 통과한 뒤 다른 요청이 먼저 커밋한 경우다. 어느 제약이 걸렸는지는
            // 메시지로 가른다 — 사용자에게는 "중복"이라는 같은 사실이므로 코드도 같다.
            throw new BusinessException(duplicateCodeOf(e));
        }

        defaultExpendGroupService.createDefaults(saved);
        return memberMapper.toSignupResponse(saved);
    }

    /** 유니크 위반이 아이디 쪽인지 이메일 쪽인지 가린다. */
    private static ErrorCode duplicateCodeOf(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        if (message != null && message.contains("ux_user_email")) {
            return ErrorCode.EMAIL_DUPLICATED;
        }
        return ErrorCode.MEMBER_ID_DUPLICATED;
    }

    /** 빈 문자열은 {@code null} 로 저장한다 — 선택 항목의 "값 없음"을 한 가지로 통일한다. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 1.3 로그인. auth-pipeline.md §1 의 8단계를 그대로 따른다.
     *
     * <p>실패 코드가 갈리는 자리는 <b>이력을 남기는지</b>다. 아이디 자체가 없으면
     * {@code id_key} 를 채울 수 없어 행을 만들지 못하고(FR-127), 비밀번호 불일치와
     * 비활성 계정은 회원이 특정되므로 실패 이력을 남긴다.
     *
     * <p>아이디가 없을 때와 비밀번호가 틀렸을 때 <b>같은 {@code 1003}</b> 을 돌려준다.
     * 코드를 나누면 "그 아이디는 존재한다"를 알려 주는 셈이 된다.
     */
    public LoginResponse login(LoginRequest request, String loginIp) {
        Optional<User> found = userRepository.findByUserId(request.memberId());
        if (found.isEmpty()) {
            loginHistoryService.recordUnknownMember(request.memberId(), loginIp);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPw())) {
            loginHistoryService.record(user, loginIp, false);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            loginHistoryService.record(user, loginIp, false);
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        IssuedTokens tokens = issueWithRetry(user);
        loginHistoryService.record(user, loginIp, true);
        return LoginResponse.of(user, tokens);
    }

    /**
     * 세션을 발급하되, 동시 로그인 경합으로 유니크 위반이 나면 한 번 더 시도한다.
     *
     * <p>두 요청이 같은 순간 "활성 세션 없음"을 보면 하나가
     * {@code ux_user_session_active} 위반으로 실패한다. 그때 다시 시도하면 상대가 만든
     * 세션이 보이므로 폐기 후 삽입이 정상적으로 끝난다. 애플리케이션 검사만으로는 이 창을
     * 닫을 수 없어 DB 인덱스를 최종 방어선으로 삼는다(research.md §10).
     *
     * <p>재시도까지 실패하면 그대로 올려 보낸다 — {@code GlobalExceptionHandler} 가
     * {@code 9000} 으로 바꾼다. 로그인 실패({@code 1003})로 흘리면 사용자가 비밀번호를
     * 의심하게 되는데 원인은 그쪽이 아니다.
     */
    private IssuedTokens issueWithRetry(User user) {
        for (int attempt = 0; ; attempt++) {
            try {
                return sessionService.issue(user);
            } catch (DataIntegrityViolationException e) {
                if (attempt >= SESSION_ISSUE_RETRIES) {
                    throw e;
                }
            }
        }
    }

    /**
     * 1.4 토큰 검증.
     *
     * <p>여기 도달했다는 것은 필터의 10단계를 이미 통과했다는 뜻이다. 남은 일은 만료까지
     * 남은 시간을 세는 것뿐이며, 그 기준은 <b>DB 의 {@code access_expires_at}</b> 이다 —
     * JWT {@code exp} 와 다를 수 있고 둘 다 지나지 않아야 유효하다.
     */
    @Transactional(readOnly = true)
    public TokenValidateResponse validate(AuthPrincipal principal) {
        UserSession session = sessionService.findBySessionId(principal.sessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID));
        return TokenValidateResponse.of(principal.memberId(), principal.role(),
                secondsUntil(session.getAccessExpiresAt()));
    }

    /**
     * 1.6 로그아웃. 현재 세션의 두 해시를 비우고 {@code revoked} 를 세운다.
     *
     * <p>이미 폐기된 세션의 재로그아웃은 {@code 1006} 으로 거절한다. 성공으로 흘리면
     * "폐기됐다"와 "폐기한 적 없다"가 구분되지 않는다.
     */
    public MessageResponse revoke(AuthPrincipal principal) {
        UserSession session = sessionService.findBySessionId(principal.sessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID));
        if (Boolean.TRUE.equals(session.getRevoked())) {
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        sessionService.revoke(session);
        return new MessageResponse("로그아웃되었습니다");
    }

    /**
     * 1.5 토큰 갱신(Rotation). auth-pipeline.md §3 의 7단계를 따른다.
     *
     * <p>{@code 1005} 와 {@code 1006} 의 구분이 이 메서드의 핵심이다. 세션을 찾지
     * 못했거나(해시가 비었거나 값이 다르다) 기한이 지났으면 {@code 1005} 이고,
     * <b>다른 곳에서 로그인해 세션이 교체된 경우만</b> {@code 1006} 이다. 로그아웃 후
     * 갱신은 해시가 {@code NULL} 이라 조회 자체가 실패하므로 {@code 1005} 다.
     */
    public TokenResponse refresh(String refreshToken) {
        UserSession session = sessionService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (Boolean.TRUE.equals(session.getRevoked())) {
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        if (session.getRefreshExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (!Boolean.TRUE.equals(session.getUser().getActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        return TokenResponse.from(sessionService.rotate(session));
    }

    /**
     * 1.9 아이디 찾기. 가입 이메일로 회원을 찾아 <b>가린 아이디</b>를 돌려준다.
     *
     * <p>이메일 형식 오류는 Bean Validation 이 {@code 9001} 로, 일치하는 회원이 없으면
     * {@code 2001} 로 나간다. <b>계정 존재 여부를 감추는 통일 응답은 쓰지 않는다</b> —
     * 스펙이 "해당 이메일로 가입된 회원 없음"을 알려 주기로 정했다. 대신 찾아낸 아이디를
     * 가려서 이메일 하나로 남의 아이디를 온전히 얻지는 못하게 한다.
     */
    @Transactional(readOnly = true)
    public FindIdResponse findId(String email) {
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return new FindIdResponse(MemberIdMasker.mask(user.getUserId()), true);
    }

    /**
     * 1.10 비밀번호 찾기. 본인 확인만 하고 <b>아무것도 바꾸지 않는다</b>.
     *
     * <p>확인 수단은 {@code memberId}+{@code nickname} 대조 하나다. 통과해도 서버는
     * 상태를 남기지 않는다 — 재설정 토큰·인증코드·만료 시간이 존재하지 않으므로,
     * 이 호출은 화면이 다음 단계로 넘어가도 되는지 묻는 것에 가깝다.
     */
    @Transactional(readOnly = true)
    public FindPasswordResponse findPassword(FindPasswordRequest request) {
        User user = findByMemberIdAndNickname(request.memberId(), request.nickname());
        return new FindPasswordResponse(true, MemberIdMasker.mask(user.getUserId()));
    }

    /**
     * 1.11 비밀번호 재설정.
     *
     * <p><b>1.10 과 같은 두 값을 다시 검증한다.</b> 1.10 을 거쳤는지 서버는 알지 못하고
     * 알 필요도 없다 — 상태를 들고 다니지 않으므로 이 API 만 직접 불러도 판정이 같다.
     * 그래서 "찾기를 통과했다"는 사실이 재설정의 근거가 되지 않는다.
     *
     * <p>성공하면 <b>그 회원의 활성 세션을 폐기한다</b>. 비밀번호를 재설정하는 상황은
     * 대개 남이 알고 있다고 의심될 때인데, 세션을 남겨 두면 그 남이 계속 들어와 있다.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (!MemberFieldRules.isValidPassword(request.newPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_RULE_VIOLATION);
        }
        User user = findByMemberIdAndNickname(request.memberId(), request.nickname());

        user.setPw(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);
        sessionService.revokeActiveSession(user.getIdKey());
        return new MessageResponse("비밀번호가 변경되었습니다");
    }

    /**
     * 아이디·닉네임 대조. 1.10 과 1.11 이 같은 판정을 쓰도록 한 곳에 둔다.
     *
     * <p>불일치는 {@code 2001}, 비활성 계정은 {@code 1004} 다. 아이디가 없는 것과
     * 닉네임이 다른 것을 같은 코드로 묶는다 — 나누면 "그 아이디는 존재한다"를 알려 준다.
     */
    private User findByMemberIdAndNickname(String memberId, String nickname) {
        User user = userRepository.findByUserId(memberId.trim())
                .filter(found -> found.getNickname().equals(nickname.trim()))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return user;
    }

    private static long secondsUntil(OffsetDateTime at) {
        return Math.max(Duration.between(OffsetDateTime.now(), at).toSeconds(), 0L);
    }
}

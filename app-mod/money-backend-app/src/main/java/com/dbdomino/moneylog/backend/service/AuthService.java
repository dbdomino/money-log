package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.LoginRequest;
import com.dbdomino.moneylog.backend.dto.response.LoginResponse;
import com.dbdomino.moneylog.backend.dto.response.MessageResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenValidateResponse;
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

    public AuthService(UserRepository userRepository,
                       MemberSessionService sessionService,
                       LoginHistoryService loginHistoryService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.loginHistoryService = loginHistoryService;
        this.passwordEncoder = passwordEncoder;
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

    private static long secondsUntil(OffsetDateTime at) {
        return Math.max(Duration.between(OffsetDateTime.now(), at).toSeconds(), 0L);
    }
}

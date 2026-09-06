package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.MemberFieldRules;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.MemberResponse;
import com.dbdomino.moneylog.backend.mapper.MemberMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인 정보 조회(1.7)·수정(1.8).
 *
 * <p><b>대상은 토큰이 정한다.</b> 요청이 회원을 지정할 방법이 없다 — 경로에 식별자가 없고
 * 서비스도 {@link AuthPrincipal} 에서만 회원을 얻는다(FR-116). "본인 데이터만 접근한다"를
 * 검사로 지키면 한 곳만 빠뜨려도 뚫리므로, 아예 지정할 수 없게 만든다.
 */
@Service
public class MemberService {

    /** 1.8 이 수정할 수 있는 필드. 그 밖의 이름이 오면 {@code 9001} 이다. */
    private static final java.util.Set<String> UPDATABLE_FIELDS =
            java.util.Set.of("password", "nickname", "email", "phone", "intro");

    private final UserRepository userRepository;
    private final MemberSessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    public MemberService(UserRepository userRepository,
                         MemberSessionService sessionService,
                         PasswordEncoder passwordEncoder,
                         MemberMapper memberMapper) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.memberMapper = memberMapper;
    }

    /** 1.7 본인 정보 조회. 응답 타입에 비밀번호 필드가 없어 해시가 실릴 자리가 없다. */
    @Transactional(readOnly = true)
    public MemberResponse getMe(AuthPrincipal principal) {
        return memberMapper.toMemberResponse(findMember(principal));
    }

    /**
     * 1.8 본인 정보 수정. PATCH omit 규칙을 따른다.
     *
     * <p>보내지 않은 필드는 그대로 두고, {@code null} 을 보낸 선택 항목은 비운다
     * ({@link PatchFields}).
     *
     * <p>비밀번호를 바꾸면 <b>그 회원의 활성 세션을 폐기한다</b>(FR-119). 비밀번호를 바꾼
     * 이유가 "남이 알아낸 것 같다"인 경우가 많은데, 세션을 남겨 두면 그 남이 계속 들어와
     * 있을 수 있다. 본인도 새 비밀번호로 다시 로그인해야 한다.
     *
     * <p>{@code memberId}({@code user_id})는 바꿀 수 없다. 수정 가능 필드 목록에 없고
     * 컬럼도 {@code updatable=false} 다.
     */
    @Transactional
    public MemberResponse updateMe(AuthPrincipal principal, java.util.Map<String, Object> body) {
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);
        if (fields.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "수정할 필드가 없습니다.");
        }
        User user = findMember(principal);
        boolean passwordChanged = false;

        if (fields.has("nickname")) {
            String nickname = fields.string("nickname");
            if (!MemberFieldRules.isValidNickname(nickname)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "닉네임은 2~20자여야 합니다.");
            }
            user.setNickname(nickname.trim());
        }
        if (fields.has("email")) {
            String email = fields.string("email");
            // 자기 자신이 이미 쓰고 있는 값이면 중복이 아니다.
            if (email != null && !email.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
            }
            user.setEmail(email);
        }
        if (fields.has("phone")) {
            String phone = fields.string("phone");
            if (!MemberFieldRules.isValidPhone(phone)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "연락처는 하이픈 없이 숫자만 입력합니다.");
            }
            user.setPhone(phone);
        }
        if (fields.has("intro")) {
            user.setIntro(fields.string("intro"));
        }
        if (fields.has("password")) {
            String password = fields.string("password");
            if (!MemberFieldRules.isValidPassword(password)) {
                throw new BusinessException(ErrorCode.PASSWORD_RULE_VIOLATION);
            }
            user.setPw(passwordEncoder.encode(password));
            passwordChanged = true;
        }

        User saved = userRepository.saveAndFlush(user);
        if (passwordChanged) {
            sessionService.revokeActiveSession(saved.getIdKey());
        }
        return memberMapper.toMemberResponse(saved);
    }

    /**
     * 토큰의 회원을 읽는다.
     *
     * <p>토큰은 유효한데 회원 행이 사라진 경우가 {@code 2001} 이다 — 관리자가 지웠거나
     * 데이터가 어긋난 상태이며, 인증 실패({@code 1001})와는 원인이 다르다.
     */
    private User findMember(AuthPrincipal principal) {
        return userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.AdminMemberCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.AdminMemberListQuery;
import com.dbdomino.moneylog.backend.dto.request.MemberFieldRules;
import com.dbdomino.moneylog.backend.dto.request.PatchFields;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberDeactivateResponse;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberListResponse;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberResponse;
import com.dbdomino.moneylog.backend.mapper.MemberMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 관리 — 추가(1.12)·목록(1.13)·상세(1.14)·수정(1.15)·정지(1.16).
 *
 * <p>권한 검사는 여기 없다. {@code SecurityConfig} 가 {@code /api/v1/admin/**} 에
 * {@code hasRole('ADMIN')} 을 걸어 두었고, 통과하지 못한 요청은 서비스에 닿지 않는다 —
 * 규칙을 두 곳에 두면 한쪽만 고치는 순간 갈린다.
 *
 * <p>감사 컬럼은 {@code BackendAuditorAware} 가 채운다. 관리자 API 는 인증을 통과한
 * 요청이라 {@code SecurityContext} 에 관리자의 {@code id_key} 가 있고, 그 값이
 * {@code created_by}/{@code updated_by} 로 들어간다(FR-121). 본인 가입이 {@code null}
 * 인 것과 대비된다.
 */
@Service
public class AdminMemberService {

    /** 1.15 가 수정할 수 있는 필드. 본인 수정(1.8)에 {@code role} 이 더해진다. */
    private static final Set<String> UPDATABLE_FIELDS =
            Set.of("password", "nickname", "role", "email", "phone", "intro");

    private final UserRepository userRepository;
    private final MemberSessionService sessionService;
    private final DefaultExpendGroupService defaultExpendGroupService;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;

    public AdminMemberService(UserRepository userRepository,
                              MemberSessionService sessionService,
                              DefaultExpendGroupService defaultExpendGroupService,
                              PasswordEncoder passwordEncoder,
                              MemberMapper memberMapper) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
        this.defaultExpendGroupService = defaultExpendGroupService;
        this.passwordEncoder = passwordEncoder;
        this.memberMapper = memberMapper;
    }

    /**
     * 1.12 회원 추가.
     *
     * <p>가입(1.2)과 <b>똑같이 기본 지출유형 10종을 만든다</b>(FR-106). 관리자가 만든
     * 계정이라고 유형이 없으면 그 회원은 첫 지출을 등록할 수 없다.
     *
     * <p>{@code role} 은 요청이 정한다. 값이 {@code 1}·{@code 3} 밖이면 {@code 9001} 이고,
     * DB 의 {@code ck_user_role} 이 최종 방어선이다.
     */
    @Transactional
    public AdminMemberResponse create(AdminMemberCreateRequest request) {
        if (!MemberFieldRules.isValidPassword(request.password())) {
            throw new BusinessException(ErrorCode.PASSWORD_RULE_VIOLATION);
        }
        short role = validRole(request.role());
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
        user.setRole(role);
        user.setActive(true);

        User saved = userRepository.saveAndFlush(user);
        defaultExpendGroupService.createDefaults(saved);
        return toAdminResponse(saved);
    }

    /**
     * 1.13 회원 목록.
     *
     * <p>{@code totalCount} 는 <b>검색 조건에 걸린 전체 건수</b>다. 현재 페이지 건수를
     * 돌려주면 화면이 마지막 페이지를 계산할 수 없다.
     */
    @Transactional(readOnly = true)
    public AdminMemberListResponse list(AdminMemberListQuery query) {
        // 검색어 없음을 빈 문자열로 넘긴다. null 을 넘기면 PostgreSQL 이 파라미터 타입을
        // 정하지 못해 lower(bytea) 로 해석하고 쿼리가 통째로 실패한다.
        String memberId = nullToEmpty(query.memberId());
        String nickname = nullToEmpty(query.nickname());

        List<AdminMemberResponse> members = userRepository
                .search(memberId, nickname, PageRequest.of(query.pageNumber(), query.limit()))
                .stream()
                .map(this::toAdminResponse)
                .toList();
        long totalCount = userRepository.countSearch(memberId, nickname);
        return new AdminMemberListResponse(members, query.offset(), query.limit(), totalCount);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 1.14 회원 상세. 없는 아이디는 {@code 2001} 이다. */
    @Transactional(readOnly = true)
    public AdminMemberResponse get(String memberId) {
        return toAdminResponse(findMember(memberId));
    }

    /**
     * 1.15 회원 수정. PATCH omit 규칙은 1.8 과 <b>같은 방식</b>을 쓴다.
     *
     * <p>본인 수정과 다른 점은 {@code role} 을 바꿀 수 있다는 것이다. 비밀번호를 바꾸면
     * 그 회원의 활성 세션을 폐기한다(FR-119) — 관리자가 비밀번호를 바꿨다는 것은 대개
     * 그 계정에 문제가 생겼다는 뜻이라, 기존 세션을 살려 두면 조치가 절반만 된다.
     */
    @Transactional
    public AdminMemberResponse update(String memberId, Map<String, Object> body) {
        PatchFields fields = PatchFields.of(body, UPDATABLE_FIELDS);
        if (fields.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "수정할 필드가 없습니다.");
        }
        User user = findMember(memberId);
        boolean passwordChanged = false;

        if (fields.has("nickname")) {
            String nickname = fields.string("nickname");
            if (!MemberFieldRules.isValidNickname(nickname)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "닉네임은 2~20자여야 합니다.");
            }
            user.setNickname(nickname.trim());
        }
        if (fields.has("role")) {
            Object raw = body.get("role");
            if (!(raw instanceof Number number)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "role 은 숫자여야 합니다.");
            }
            user.setRole(validRole(number.shortValue()));
        }
        if (fields.has("email")) {
            String email = fields.string("email");
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
        return toAdminResponse(saved);
    }

    /**
     * 1.16 회원 정지.
     *
     * <p>{@code active=false} 로 <b>표시만</b> 하고 행·데이터를 지우지 않는다(FR-118).
     * 그 회원의 활성 세션은 폐기한다 — 정지했는데 이미 로그인해 있던 사람이 계속 쓰면
     * 정지가 무의미하다(FR-119).
     *
     * <p>거절하는 두 경우가 모두 {@code 9001} 이다. 설계 명세의 실패 표에 자기 정지 코드가
     * 배정되어 있지 않아 api-contract.md §7 에서 정한 것이며, 둘 다 "이 정지 요청은
     * 성립하지 않는다"는 같은 성격이라 프론트가 취할 조치도 같다.
     *
     * <ul>
     *   <li>이미 정지된 회원의 재정지 — 성공으로 흘리면 "정지했다"와 "이미 정지였다"가
     *       구분되지 않는다
     *   <li>관리자가 자기 계정을 정지 — 마지막 관리자가 스스로를 잠그면 되살릴 방법이
     *       없다(재활성화 API 는 이 기능의 범위 밖이다)
     * </ul>
     */
    @Transactional
    public AdminMemberDeactivateResponse deactivate(AuthPrincipal admin, String memberId) {
        User user = findMember(memberId);
        if (user.getIdKey().equals(admin.idKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "자기 계정은 정지할 수 없습니다.");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 정지된 회원입니다.");
        }

        user.setActive(false);
        User saved = userRepository.saveAndFlush(user);
        sessionService.revokeActiveSession(saved.getIdKey());
        return new AdminMemberDeactivateResponse(saved.getUserId(), false, "회원이 정지되었습니다");
    }

    private User findMember(String memberId) {
        return userRepository.findByUserId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /** 권한 값은 {@code 1}·{@code 3} 둘뿐이다. CHECK 제약과 같은 범위를 애플리케이션에서도 막는다. */
    private static short validRole(Short role) {
        if (role == null || (role != User.ROLE_ADMIN && role != User.ROLE_MEMBER)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "role 은 1(관리자) 또는 3(일반)이어야 합니다.");
        }
        return role;
    }

    private AdminMemberResponse toAdminResponse(User user) {
        var member = memberMapper.toMemberResponse(user);
        return new AdminMemberResponse(member.memberId(), member.nickname(), member.email(),
                member.phone(), member.intro(), member.role(), Boolean.TRUE.equals(user.getActive()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

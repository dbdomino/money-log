package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.data.entity.User;
import java.util.UUID;

/**
 * 인증을 통과한 요청의 주체. {@code SecurityContext}에 실린다.
 *
 * <p>토큰의 {@code sub}는 로그인 아이디({@code user_id})라 그대로는 소유자 조회에 쓸 수
 * 없다. 자식 테이블이 참조하는 소유자 키는 {@code id_key} 하나이기 때문이다. 그래서
 * 인증 필터가 {@code sub}를 {@code id_key}로 <b>환산해</b> 둘을 함께 싣는다.
 *
 * <p>{@code idKey}는 감사 컬럼({@code created_by}·{@code updated_by})의 공급원이기도
 * 하다 — {@code BackendAuditorAware}가 여기서 값을 꺼낸다.
 *
 * @param memberId  로그인 아이디({@code tbl_user.user_id}). API 가 주고받는 값이다
 * @param idKey     회원 대리키({@code tbl_user.id_key}). 소유자 판정과 감사 컬럼에 쓴다
 * @param role      권한. 관리자 {@code 1}, 일반 {@code 3}
 * @param sessionId 이 요청이 쓴 세션({@code tbl_user_session.session_id})
 */
public record AuthPrincipal(String memberId, Long idKey, short role, UUID sessionId) {

    /**
     * Spring Security 권한 이름. {@code hasRole('ADMIN')}이 이 이름을 본다.
     *
     * <p>권한 값 자체({@code 1}·{@code 3})는 {@link User#ROLE_ADMIN}·
     * {@link User#ROLE_MEMBER}가 단일 출처다. 여기에 같은 숫자를 다시 적으면 한쪽만
     * 고치는 순간 인가가 조용히 어긋난다 — CHECK 제약({@code ck_user_role})은 값의
     * 범위만 보지 두 상수가 같은지는 보지 않는다.
     */
    public static final String AUTHORITY_ADMIN = "ROLE_ADMIN";

    /** 일반 회원의 권한 이름. */
    public static final String AUTHORITY_MEMBER = "ROLE_MEMBER";

    public boolean isAdmin() {
        return role == User.ROLE_ADMIN;
    }

    /** 이 주체에게 부여할 Spring Security 권한 이름. */
    public String authority() {
        return isAdmin() ? AUTHORITY_ADMIN : AUTHORITY_MEMBER;
    }
}

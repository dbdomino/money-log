package com.dbdomino.moneylog.front.session;

/**
 * 세션이 들고 있는 회원 식별 정보.
 *
 * <p><b>토큰에서 꺼내 쓰지 않는다.</b> 화면 모듈이 JWT 를 파싱하면 서명 검증 책임이 따라온다
 * — 파싱만 하고 검증을 빼면 위조한 토큰의 권한을 그대로 믿게 된다. 두 값은 로그인 응답과
 * 토큰 검증 응답이 평문 필드로 주므로 그것을 담는다.
 *
 * @param memberId 로그인 아이디. 백엔드의 대리키가 아니라 <b>사용자가 입력한 아이디</b>다
 * @param role 권한. {@link #ROLE_ADMIN} 관리자 · {@link #ROLE_MEMBER} 일반
 */
public record SessionUser(String memberId, int role) {

    /** 관리자. 관리자 전용 주소와 회원 관리 메뉴가 이 값 하나를 본다. */
    public static final int ROLE_ADMIN = 1;

    /** 일반 회원. 가입 기본값이다. */
    public static final int ROLE_MEMBER = 3;

    public boolean isAdmin() {
        return role == ROLE_ADMIN;
    }
}

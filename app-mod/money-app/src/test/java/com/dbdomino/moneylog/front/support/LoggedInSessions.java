package com.dbdomino.moneylog.front.support;

import com.dbdomino.moneylog.front.session.SessionUser;
import org.springframework.mock.web.MockHttpSession;

/**
 * 로그인 상태를 세션에 심는 시험 도우미.
 *
 * <p>007 의 시험 세 곳이 같은 코드를 따로 들고 있었고 008 이 화면 아홉 개분을 더한다. 속성
 * 이름을 열두 곳에 복사해 두면 007 이 이름을 하나 바꿀 때 어느 시험이 조용히 미로그인으로
 * 도는지 알 수 없다 — 그 시험은 실패하는 대신 <b>다른 것을 검증하게</b> 된다.
 *
 * <p>속성 이름은 {@code LoginSession} 의 것과 같아야 한다. 그쪽은 이름을 밖으로 내보내지
 * 않는데, 세션을 읽고 쓰는 자리를 하나로 묶는 것이 그 클래스의 취지이기 때문이다. 시험은
 * 그 취지를 깨지 않으려고 같은 이름을 여기 한 번 더 적는다.
 *
 * <p><b>시험 소스만 손대는 도우미다.</b> 운영 코드를 고치는 것이 아니라 시험이 들고 있던
 * 중복을 모으는 것이라 "007 을 고치지 않는다"에 걸리지 않는다.
 */
public final class LoggedInSessions {

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String MEMBER_ID = "memberId";
    private static final String ROLE = "role";

    /** 일반 회원의 기본 아이디. */
    public static final String MEMBER = "hong";

    /** 관리자의 기본 아이디. */
    public static final String ADMIN = "admin";

    private LoggedInSessions() {
    }

    /** 일반 권한으로 로그인한 세션. */
    public static MockHttpSession member() {
        return of(MEMBER, SessionUser.ROLE_MEMBER);
    }

    /** 관리자 권한으로 로그인한 세션. */
    public static MockHttpSession admin() {
        return of(ADMIN, SessionUser.ROLE_ADMIN);
    }

    /** 권한으로 고른다. 아이디는 권한에 따라 정해진 기본값을 쓴다. */
    public static MockHttpSession of(int role) {
        return of(role == SessionUser.ROLE_ADMIN ? ADMIN : MEMBER, role);
    }

    /** 아이디와 권한을 함께 정한다. 본인인지 남인지 가르는 시험이 이것을 쓴다. */
    public static MockHttpSession of(String memberId, int role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ACCESS_TOKEN, "access-1");
        session.setAttribute(REFRESH_TOKEN, "refresh-1");
        session.setAttribute(MEMBER_ID, memberId);
        session.setAttribute(ROLE, role);
        return session;
    }
}

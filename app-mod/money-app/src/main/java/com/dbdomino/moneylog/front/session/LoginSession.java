package com.dbdomino.moneylog.front.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 로그인 세션을 읽고 쓰는 <b>유일한 자리</b>.
 *
 * <p>컨트롤러·인터셉터·API 클라이언트가 세션 속성을 직접 꺼내지 않는다. 속성 이름이 여러
 * 곳에 흩어지면 오타 하나가 "로그인이 자꾸 풀린다"는 증상으로 나타나고, 어느 자리의 오타인지
 * 찾는 데 드는 시간이 길다.
 *
 * <p><b>토큰을 화면으로 내보내는 통로를 두지 않는다.</b> 접근 토큰을 꺼내는 메서드는 있지만
 * 그것을 쓰는 곳은 백엔드 호출과 진입 판정뿐이고, 모델에 담는 메서드는 아예 없다. 브라우저로
 * 내려가는 것은 세션 식별자뿐이다.
 *
 * <p>만료 시각을 담지 않는다. 화면 모듈이 만료를 미리 판정하면 시계가 어긋났을 때 서버는
 * 멀쩡하다는데 화면만 만료라고 믿는 상태가 생긴다. 판정은 백엔드가 하고 화면은 그 응답에
 * 대응한다.
 */
@Component
public class LoginSession {

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String MEMBER_ID = "memberId";
    private static final String ROLE = "role";

    /**
     * 로그인 성공 시 네 값을 담는다.
     *
     * <p>세션 식별자를 새로 만든 뒤에 담는다 — 로그인 전에 쓰던 식별자를 그대로 이어 쓰면
     * 공격자가 미리 심어 둔 식별자로 로그인 후 세션에 올라탈 수 있다.
     */
    public void login(String accessToken, String refreshToken, String memberId, int role) {
        HttpServletRequest request = currentRequest();
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(ACCESS_TOKEN, accessToken);
        session.setAttribute(REFRESH_TOKEN, refreshToken);
        session.setAttribute(MEMBER_ID, memberId);
        session.setAttribute(ROLE, role);
    }

    /**
     * 재발급 성공 시 토큰 <b>두 개를 모두</b> 덮어쓴다.
     *
     * <p>백엔드 재발급은 Rotation 이라 새 재발급 토큰을 함께 준다. 접근 토큰만 갈아 끼우면
     * 세션에 이미 폐기된 재발급 토큰이 남아 다음 재발급이 실패한다 — "한 번은 되고 두 번째부터
     * 안 되는 로그인 연장"이라 원인을 찾기 어렵다.
     */
    public void replaceTokens(String accessToken, String refreshToken) {
        HttpSession session = currentSession(false);
        if (session == null) {
            return;
        }
        session.setAttribute(ACCESS_TOKEN, accessToken);
        session.setAttribute(REFRESH_TOKEN, refreshToken);
    }

    /** 토큰 검증 응답이 준 최신 식별 정보를 반영한다. 권한이 바뀌었으면 여기서 따라간다. */
    public void refreshUser(String memberId, int role) {
        HttpSession session = currentSession(false);
        if (session == null) {
            return;
        }
        session.setAttribute(MEMBER_ID, memberId);
        session.setAttribute(ROLE, role);
    }

    public String accessToken() {
        return attribute(ACCESS_TOKEN, String.class);
    }

    public String refreshToken() {
        return attribute(REFRESH_TOKEN, String.class);
    }

    /** 화면 표시와 권한 판정에 쓸 회원 정보. 로그인 전이면 {@code null} 이다. */
    public SessionUser user() {
        String memberId = attribute(MEMBER_ID, String.class);
        Integer role = attribute(ROLE, Integer.class);
        if (memberId == null || role == null) {
            return null;
        }
        return new SessionUser(memberId, role);
    }

    /**
     * 세션에 접근 토큰이 있는가.
     *
     * <p>이것만으로 통과시키지 않는다. 토큰이 아직 살아 있는지는 백엔드가 답하고, 그 확인이
     * 진입 판정의 다음 단계다 — 세션 존재만 믿으면 백엔드에서 죽은 토큰으로도 화면이 열린다.
     */
    public boolean isLoggedIn() {
        String token = accessToken();
        return token != null && !token.isBlank();
    }

    public boolean isAdmin() {
        SessionUser user = user();
        return user != null && user.isAdmin();
    }

    /** 세션을 버린다. 이미 없으면 아무것도 하지 않는다. */
    public void invalidate() {
        HttpSession session = currentSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    // ── 현재 요청의 세션 ────────────────────────────────────────────────

    private <T> T attribute(String name, Class<T> type) {
        HttpSession session = currentSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(name);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    private HttpSession currentSession(boolean create) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest().getSession(create);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("요청 밖에서 로그인 세션을 건드릴 수 없다.");
        }
        return attributes.getRequest();
    }
}

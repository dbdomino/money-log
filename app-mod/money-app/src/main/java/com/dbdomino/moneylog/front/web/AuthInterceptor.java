package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.session.LoginSession;
import com.dbdomino.moneylog.front.session.SessionExpiredException;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 화면에 들어올 수 있는지 판정한다. <b>로그인 판정이 권한 판정보다 먼저다.</b>
 *
 * <p>순서가 뒤집히면 로그인하지 않은 사람이 관리자 주소를 쳤을 때 권한 없음 화면을 보게
 * 되는데, 그것은 <b>그 주소가 실재한다는 사실을 알려 주는 것</b>이다. 로그인 화면으로 보내면
 * 있는 주소인지 없는 주소인지 구분되지 않는다.
 *
 * <p>인터셉터를 둘로 나누지 않는 것도 같은 이유다. 순서가 등록 코드에 달려 있으면 다른
 * 파일에서 한 줄 끼워 넣는 것만으로 조용히 뒤집힌다.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 로그인 화면. 판정에 걸린 요청이 여기로 간다. */
    public static final String LOGIN_URL = "/auth/login";

    /** 권한 없음 화면. 로그인은 했으나 권한이 모자란 경우에만 여기로 간다. */
    public static final String FORBIDDEN_URL = "/error/forbidden";

    /** 백엔드 토큰 검증 경로. */
    private static final String VALIDATE_PATH = "/auth/validate";

    /** 관리자 전용 주소의 앞머리. */
    private static final String ADMIN_PREFIX = "/admin";

    /**
     * 로그인 없이 열 수 있는 주소 <b>여섯 개가 전부</b>다.
     *
     * <p>화이트리스트여야 한다. 막을 주소를 나열하는 방식이면 008~012 가 화면을 더할 때마다
     * 목록에 넣는 것을 잊을 수 있고, 잊은 화면은 아무나 열 수 있는 상태로 배포된다. 허용할
     * 주소를 나열하면 새 화면의 기본값이 "로그인 필요"가 된다.
     */
    private static final Set<String> PUBLIC_URLS = Set.of(
            LOGIN_URL,
            "/auth/signup",
            "/auth/find-id",
            "/auth/find-password",
            "/auth/reset-password",
            FORBIDDEN_URL);

    private final LoginSession loginSession;
    private final BackendApiClient backendApiClient;

    public AuthInterceptor(LoginSession loginSession, BackendApiClient backendApiClient) {
        this.loginSession = loginSession;
        this.backendApiClient = backendApiClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String path = request.getRequestURI();
        if (PUBLIC_URLS.contains(path)) {
            return true;
        }

        // ① 로그인 판정
        if (!loginSession.isLoggedIn()) {
            response.sendRedirect(request.getContextPath() + LOGIN_URL);
            return false;
        }

        // 세션에 토큰이 있을 때만 백엔드에 묻는다. 비었으면 인증 실패가 돌아올 것이 확정이라
        // 왕복이 낭비다. 이 호출이 "세션은 살아 있는데 백엔드 토큰이 죽은" 경우를 잡는
        // 유일한 지점이며, API 를 하나도 부르지 않는 화면에서도 죽은 세션이 열리지 않게 한다.
        try {
            TokenValidateResult result = backendApiClient.get(VALIDATE_PATH, TokenValidateResult.class);
            if (result != null) {
                loginSession.refreshUser(result.memberId(), result.role());
            }
        } catch (SessionExpiredException e) {
            // 세션은 이미 버려졌다. 화면만 바꿔 주면 된다.
            response.sendRedirect(request.getContextPath() + LOGIN_URL);
            return false;
        }

        // ② 권한 판정
        if (isAdminUrl(path) && !loginSession.isAdmin()) {
            response.sendRedirect(request.getContextPath() + FORBIDDEN_URL);
            return false;
        }

        return true;
    }

    /** {@code /admin} 과 그 아래 전부가 관리자 전용이다. {@code /administrators} 같은 이름은 아니다. */
    private static boolean isAdminUrl(String path) {
        return path.equals(ADMIN_PREFIX) || path.startsWith(ADMIN_PREFIX + "/");
    }
}

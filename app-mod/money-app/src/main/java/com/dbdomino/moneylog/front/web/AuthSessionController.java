package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.session.LoginSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 로그인 세션을 끝내는 자리.
 *
 * <p>로그아웃 <b>버튼</b>은 008 이 상단바에 만들고 이 주소로 보낸다. 백엔드 토큰 비활성화를
 * 008 이 직접 부르지 않는 이유는 <b>순서를 한 곳에서 지켜야 하기 때문</b>이다 — 화면마다
 * 부르면 어느 화면 하나가 순서를 뒤집었을 때 그 화면에서만 토큰이 살아남는다.
 */
@Controller
public class AuthSessionController {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionController.class);

    /** 백엔드 토큰 비활성화 경로. */
    private static final String REVOKE_PATH = "/auth/revoke";

    private final BackendApiClient backendApiClient;
    private final LoginSession loginSession;

    public AuthSessionController(BackendApiClient backendApiClient, LoginSession loginSession) {
        this.backendApiClient = backendApiClient;
        this.loginSession = loginSession;
    }

    /**
     * 로그아웃. <b>토큰 비활성화가 먼저, 세션 버리기가 나중</b>이다.
     *
     * <p>순서를 뒤집으면 백엔드 토큰이 살아남는다 — 세션을 먼저 비우면 비활성화 요청에 실을
     * 토큰이 없다. 그 토큰은 만료 전까지 유효해서, 어딘가에 새어 있었다면 로그아웃한 뒤에도
     * 쓸 수 있다.
     *
     * <p>비활성화가 실패해도 세션은 버린다. 사용자가 로그아웃을 눌렀는데 화면이 로그인 상태로
     * 남는 쪽이 더 나쁘다.
     */
    @PostMapping("/auth/logout")
    public String logout() {
        try {
            backendApiClient.post(REVOKE_PATH, null, Void.class);
        } catch (RuntimeException e) {
            log.info("토큰 비활성화가 실패했지만 세션은 버린다: {}", e.getMessage());
        }
        loginSession.invalidate();
        return "redirect:" + AuthInterceptor.LOGIN_URL;
    }
}

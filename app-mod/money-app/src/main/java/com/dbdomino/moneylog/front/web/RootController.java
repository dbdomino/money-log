package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.session.LoginSession;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 주소로 들어온 사용자를 로그인 상태에 따라 갈라 보낸다.
 *
 * <p><b>토큰이 아직 유효한지까지 확인하지 않는다.</b> 세션이 있는지만 보고 보내고, 실제 검증은
 * 착지한 화면의 진입 판정이 한다 — 여기서 또 확인하면 백엔드 왕복이 두 번이 된다.
 *
 * <p>영구 이동이 아니라 임시 이동인 이유는 <b>목적지가 로그인 상태에 따라 바뀌기 때문</b>이다.
 * 영구로 두면 브라우저가 첫 결과를 기억해, 로그아웃한 뒤에도 가계부 주소로 가려다 판정에
 * 걸린다.
 */
@Controller
public class RootController {

    /** 로그인한 사용자의 첫 화면. 월별 가계부다. */
    private static final URI LEDGER = URI.create("/ledger");

    private static final URI LOGIN = URI.create(AuthInterceptor.LOGIN_URL);

    private final LoginSession loginSession;

    public RootController(LoginSession loginSession) {
        this.loginSession = loginSession;
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        URI target = loginSession.isLoggedIn() ? LEDGER : LOGIN;
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }
}

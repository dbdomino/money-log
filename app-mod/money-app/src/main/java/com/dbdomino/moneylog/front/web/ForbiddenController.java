package com.dbdomino.moneylog.front.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 1.6 권한 없음. 007 이 만드는 <b>유일한 화면</b>이다.
 *
 * <p>두 가지가 다른 화면과 다르다. ① 권한 차단이 착지할 곳이 없으면 진입 판정을 완성할 수
 * 없어 인터셉터와 함께 서야 한다. ② <b>백엔드를 하나도 부르지 않는다</b> — 008~012 의 화면
 * 30개는 전부 백엔드를 부른다.
 *
 * <p>레이아웃은 로그인 전 쪽이다. 미로그인 사용자도 주소를 직접 치면 들어올 수 있는 화면이라
 * 사이드바를 붙일 수 없다 — 사이드바는 회원 권한을 필요로 한다.
 */
@Controller
public class ForbiddenController {

    @GetMapping(AuthInterceptor.FORBIDDEN_URL)
    public String forbidden() {
        return "error/forbidden";
    }
}

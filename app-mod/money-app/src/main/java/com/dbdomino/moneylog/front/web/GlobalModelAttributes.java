package com.dbdomino.moneylog.front.web;

import com.dbdomino.moneylog.front.session.LoginSession;
import com.dbdomino.moneylog.front.session.SessionUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면이 공통으로 필요로 하는 값을 모델에 담는다.
 *
 * <p>지금은 로그인한 회원 하나뿐이다. 사이드바가 권한을 보고 회원 관리 메뉴를 그릴지 정하고
 * 상단바가 아이디를 표시하는데, 이것을 화면마다 담게 하면 <b>어느 화면 하나가 빠뜨렸을 때
 * 그 화면에서만 메뉴가 사라진다.</b> 원인이 "누락"이라 증상만 보고는 짐작하기 어렵다.
 *
 * <p>토큰은 담지 않는다. 모델에 들어간 값은 화면을 거쳐 브라우저로 나갈 수 있다.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final LoginSession loginSession;

    public GlobalModelAttributes(LoginSession loginSession) {
        this.loginSession = loginSession;
    }

    /** 로그인하지 않았으면 {@code null} 이다. 템플릿이 그 경우를 함께 다룬다. */
    @ModelAttribute("sessionUser")
    public SessionUser sessionUser() {
        return loginSession.user();
    }
}

package com.dbdomino.moneylog.front.auth;

import com.dbdomino.moneylog.front.auth.form.LoginForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.LoginSession;
import com.dbdomino.moneylog.front.support.FormFailure;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 1.1 로그인 화면.
 *
 * <h2>이미 로그인했으면 열지 않는다</h2>
 *
 * <p>007 의 비로그인 허용 목록은 건드리지 않는다. 그 목록이 정하는 것은 "로그인 없이 <b>열
 * 수 있는가</b>"이고(FR-618), 여기서 필요한 것은 "로그인했으면 <b>열 필요가 없다</b>"는 다른
 * 판단이다(FR-709). 진입 판정에 섞으면 공통 기반이 화면별 사정을 알게 된다.
 *
 * <h2>인증 없이 부른다</h2>
 *
 * <p>세션에 남아 있던 만료된 토큰을 실어 보내면 <b>로그인 요청 자체가 만료로 거절된다.</b>
 * 그래서 인증 헤더를 붙이지 않는 통로를 쓴다.
 *
 * <h2>폼을 모델에 담지 않는다</h2>
 *
 * <p>칸을 {@code @ModelAttribute} 로 받으면 스프링이 그 객체를 <b>모델에 함께 담는다</b>.
 * 그러면 비밀번호 평문이 모델을 거치게 되고, 모델에 들어간 값은 화면을 통해 브라우저로 나갈
 * 수 있다(SC-708). 칸을 하나씩 받아 폼을 손으로 만들면 그 경로 자체가 없다.
 *
 * <h2>실패는 오류 화면이 아니라 이 폼으로</h2>
 *
 * <p>아래 {@code @ExceptionHandler} 가 007 의 공통 착지보다 먼저 잡는다. 컨트롤러 지역
 * 선언이 {@code @ControllerAdvice} 보다 먼저 잡히는 것이 프레임워크의 규칙이라 007 을 고치지
 * 않고 착지만 바꾼다. try-catch 로 응답을 손수 만드는 것이 아니라 <b>착지를 선언으로
 * 지정하는 것</b>이다.
 */
@Controller
public class LoginController {

    /** 로그인 뒤 갈 곳. 월별 가계부는 010 이 만든다 — 지금은 주소만 있다. */
    public static final String LEDGER_URL = "/ledger";

    static final String VIEW = "auth/login";

    private static final String LOGIN_PATH = "/auth/login";

    private final BackendApiClient backendApiClient;
    private final LoginSession loginSession;

    public LoginController(BackendApiClient backendApiClient, LoginSession loginSession) {
        this.backendApiClient = backendApiClient;
        this.loginSession = loginSession;
    }

    /**
     * 폼을 그린다.
     *
     * <p>가입 직후 넘어온 아이디는 1.2 가 flash 로 실어 보내 모델의 {@code memberId} 에 이미
     * 들어 있다(FR-704) — 사용자가 방금 정한 값을 두 번 칠 이유가 없다. 주소에 싣지 않는
     * 이유는 아이디가 주소창과 방문 기록에 남기 때문이다.
     *
     * <p>세션이 끝나 밀려난 사용자에게 보일 문구도 007 이 flash 로 실어 보낸다. 둘 다
     * 템플릿이 모델에서 그대로 읽으므로 이 처리가 옮겨 심을 것이 없다 — 다른 곳에서 로그인해
     * 밀려난 사용자가 <b>왜 밀려났는지</b> 알아야 한다. 문구가 없으면 "가만히 있었는데
     * 로그아웃됐다"로만 보이고 계정이 털렸다고 의심한다.
     */
    @GetMapping("/auth/login")
    public String form() {
        if (loginSession.isLoggedIn()) {
            return "redirect:" + LEDGER_URL;
        }
        return VIEW;
    }

    /**
     * 로그인한다.
     *
     * <p>성공하면 토큰 두 개와 아이디·권한을 007 의 로그인 세션에 담는다. 세션 식별자를 새로
     * 만드는 일은 그 세션 계층이 한다 — 로그인 전 식별자를 이어 쓰면 미리 심어 둔 식별자로
     * 로그인 후 세션에 올라탈 수 있다.
     */
    @PostMapping("/auth/login")
    public String submit(
            @RequestParam(name = "memberId", required = false) String memberId,
            @RequestParam(name = "password", required = false) String password) {

        LoginForm form = new LoginForm(memberId, password);
        LoginResult result = backendApiClient.postWithoutAuth(LOGIN_PATH,
                new LoginRequest(form.memberId(), form.password()), LoginResult.class);

        loginSession.login(result.accessToken(), result.refreshToken(), result.memberId(),
                result.role());
        return "redirect:" + LEDGER_URL;
    }

    /**
     * 실패하면 같은 폼을 다시 그린다. <b>안내는 폼 상단에만</b> 둔다.
     *
     * <p>{@code 1003} 은 아이디와 비밀번호 중 무엇이 틀렸는지 백엔드가 일부러 한 코드로 묶은
     * 것이다. 친절하게 아이디 칸에 붙이는 순간 <b>그 아이디가 실재하는지 드러난다</b>(FR-706).
     * {@code 1004} 는 반대로 상태를 분명히 알리는데(FR-707), 정지된 사용자가 이유를 모른 채
     * 비밀번호만 계속 시도하는 쪽이 더 나쁘다고 002 가 이미 판단했다.
     *
     * <p>두 코드를 여기서 가르지 않고 <b>둘 다 상단</b>에 두는 이유는, 가르는 순간 "칸에
     * 붙었는가"만으로 아이디의 존재가 새어 나가기 때문이다. 문구의 차이는 백엔드가 준다.
     *
     * <p>아이디 값은 템플릿이 요청 파라미터에서 되읽어 채운다. 사용자가 방금 친 값이라
     * 그것만으로는 존재 여부가 드러나지 않는다. <b>비밀번호는 되채우지 않는다</b> — 평문이
     * HTML 에 실리면 브라우저 캐시와 방문 기록에 남는다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        FormFailure.applyToFormTop(model, exception);
        return VIEW;
    }

    /** 백엔드 {@code MemberLogin} 요청 본문. */
    private record LoginRequest(String memberId, String password) {
    }
}

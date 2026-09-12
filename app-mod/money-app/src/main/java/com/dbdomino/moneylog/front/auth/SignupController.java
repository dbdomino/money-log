package com.dbdomino.moneylog.front.auth;

import com.dbdomino.moneylog.front.auth.form.SignupForm;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 1.2 회원가입 화면.
 *
 * <p>가입에 성공하면 <b>로그인 화면으로 보내며 방금 만든 아이디를 함께 넘긴다</b>(FR-704).
 * 넘기는 방법은 flash 다 — 주소에 실으면 아이디가 주소창과 방문 기록에 남는다.
 *
 * <p>실패 착지는 1.1 과 같은 방식이되 <b>코드별로 칸을 가른다</b>. 어느 칸인지 아는 실패는
 * 그 칸 아래에 붙이는 편이 사용자가 고칠 곳을 바로 찾는다. 로그인과 달리 여기서는 칸을
 * 가려도 드러날 것이 없다 — 아이디가 중복이라는 사실은 이미 그 코드 자체가 말한다.
 *
 * <p>폼을 {@code @ModelAttribute} 로 받지 않는 이유는 1.1 과 같다. 스프링이 그 객체를 모델에
 * 담으므로 비밀번호 평문이 모델을 거치게 된다(SC-708).
 */
@Controller
public class SignupController {

    static final String VIEW = "auth/signup";

    private static final String SIGNUP_PATH = "/auth/signup";

    private final BackendApiClient backendApiClient;
    private final LoginSession loginSession;

    public SignupController(BackendApiClient backendApiClient, LoginSession loginSession) {
        this.backendApiClient = backendApiClient;
        this.loginSession = loginSession;
    }

    /**
     * 폼을 그린다. 비밀번호 규칙 문구는 템플릿이 언제나 함께 보인다(FR-702).
     *
     * <p>이미 로그인한 사용자는 월별 가계부로 보낸다(FR-709). 로그인한 사용자가 계정을 또
     * 만들 이유가 없다.
     */
    @GetMapping("/auth/signup")
    public String form() {
        if (loginSession.isLoggedIn()) {
            return "redirect:" + LoginController.LEDGER_URL;
        }
        return VIEW;
    }

    /**
     * 가입시킨다.
     *
     * <p>폰은 사용자가 하이픈을 넣어도 <b>숫자만 남겨</b> 보낸다(FR-703). 걷어내는 자리가
     * 화면 모듈이라 브라우저 스크립트가 막힌 환경에서도 저장값이 같다.
     */
    @PostMapping("/auth/signup")
    public String submit(
            @RequestParam(name = "memberId", required = false) String memberId,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "passwordConfirm", required = false) String passwordConfirm,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "intro", required = false) String intro,
            RedirectAttributes redirectAttributes) {

        SignupForm form = new SignupForm(memberId, password, passwordConfirm, nickname, email,
                phone, intro);

        SignupResult result = backendApiClient.postWithoutAuth(SIGNUP_PATH, form.toRequest(),
                SignupResult.class);

        redirectAttributes.addFlashAttribute("memberId", result.memberId());
        return "redirect:/auth/login";
    }

    /**
     * 실패하면 같은 폼을 다시 그리고 <b>코드가 가리키는 칸</b>에 안내를 붙인다.
     *
     * <p>{@code 2002} 아이디 · {@code 2003} 이메일 · {@code 2004} 비밀번호 ·
     * {@code 2005} 비밀번호 확인이고, 그 밖의 코드는 폼 상단이다. 어느 칸인지 모르는데 아무
     * 칸에나 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
     *
     * <p>입력은 템플릿이 요청 파라미터에서 되읽어 채운다. <b>비밀번호 두 칸만 비운다</b>.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        FormFailure.applyTo(model, exception);
        return VIEW;
    }
}

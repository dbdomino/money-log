package com.dbdomino.moneylog.front.auth;

import com.dbdomino.moneylog.front.auth.form.FindIdForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.support.FormFailure;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 1.3 아이디 찾기 화면.
 *
 * <p>조회한 아이디를 <b>같은 화면에</b> 보인다. 결과 전용 주소를 만들지 않는 이유는 그 주소에
 * 직접 들어온 사용자에게 아무 맥락도 없는 아이디가 보이기 때문이다.
 *
 * <p>로그인한 사용자를 되돌려 보내지 않는다. 1.1·1.2 와 달리 이 화면은 로그인한 사용자가
 * 들어올 이유가 있다 — 곁에 있는 사람의 아이디를 함께 찾아 주는 경우다.
 */
@Controller
public class FindIdController {

    static final String VIEW = "auth/find-id";

    private static final String FIND_ID_PATH = "/auth/find-id";

    private final BackendApiClient backendApiClient;

    public FindIdController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    @GetMapping("/auth/find-id")
    public String form() {
        return VIEW;
    }

    /**
     * 아이디를 찾아 같은 화면에 보인다.
     *
     * <p>가림 여부를 함께 모델에 담는다. 가려진 값이면 화면이 그 사실을 적어야 하고, 적지
     * 않으면 사용자는 가려진 값으로 로그인을 시도한다.
     */
    @PostMapping("/auth/find-id")
    public String submit(@RequestParam(name = "email", required = false) String email,
            Model model) {

        FindIdForm form = new FindIdForm(email);
        FindIdResult result = backendApiClient.postWithoutAuth(FIND_ID_PATH,
                new FindIdRequest(form.email()), FindIdResult.class);

        model.addAttribute("foundMemberId", result.memberId());
        model.addAttribute("foundMasked", result.masked());
        return VIEW;
    }

    /** 실패하면 같은 폼을 다시 그린다. 이메일 값은 템플릿이 요청 파라미터에서 되읽어 채운다. */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        FormFailure.applyTo(model, exception);
        return VIEW;
    }

    /** 백엔드 {@code MemberFindId} 요청 본문. */
    private record FindIdRequest(String email) {
    }
}

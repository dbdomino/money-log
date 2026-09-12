package com.dbdomino.moneylog.front.auth;

import com.dbdomino.moneylog.front.auth.form.FindPasswordForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.PasswordResetMark;
import com.dbdomino.moneylog.front.support.FormFailure;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 1.4 비밀번호 찾기 화면. 본인을 확인하고 1.5 로 보낸다.
 *
 * <p>확인에 성공하면 아이디·닉네임을 <b>재설정 표식에 담고</b> 1.5 로 보낸다(FR-711). 표식에
 * 담는 값은 <b>사용자가 입력한 아이디</b>이지 응답이 돌려준 아이디가 아니다 — 응답 쪽은
 * 화면 표시용이라 가려져 올 수 있고, 가려진 값을 저장 요청에 실으면 백엔드가 대조에 실패한다.
 *
 * <p>{@code 2001} 은 아이디와 닉네임 중 무엇이 틀렸는지 <b>가르지 않는다</b>. 백엔드가 한
 * 코드로 묶은 이유를 화면이 풀지 않는다 — 가르면 아이디만 바꿔 넣어 보며 실재하는 아이디를
 * 추려 낼 수 있다.
 */
@Controller
public class FindPasswordController {

    static final String VIEW = "auth/find-password";

    static final String RESET_URL = "/auth/reset-password";

    /**
     * 표식 없이 1.5 에 들어온 사용자를 보낼 곳. 이 화면이 그 흐름의 시작이다.
     *
     * <p>1.5 가 이 이름으로 부르게 두는 이유는, 안내가 가리키는 곳이 바뀌면 고칠 자리가
     * 한 곳이어야 하기 때문이다.
     */
    public static final String RESET_REQUIRED_URL = "/auth/find-password";

    private static final String FIND_PASSWORD_PATH = "/auth/find-password";

    private final BackendApiClient backendApiClient;
    private final PasswordResetMark passwordResetMark;

    public FindPasswordController(BackendApiClient backendApiClient,
            PasswordResetMark passwordResetMark) {
        this.backendApiClient = backendApiClient;
        this.passwordResetMark = passwordResetMark;
    }

    @GetMapping("/auth/find-password")
    public String form() {
        return VIEW;
    }

    /** 본인을 확인하고 표식을 담은 뒤 1.5 로 보낸다. */
    @PostMapping("/auth/find-password")
    public String submit(
            @RequestParam(name = "memberId", required = false) String memberId,
            @RequestParam(name = "nickname", required = false) String nickname) {

        FindPasswordForm form = new FindPasswordForm(memberId, nickname);
        backendApiClient.postWithoutAuth(FIND_PASSWORD_PATH,
                new FindPasswordRequest(form.memberId(), form.nickname()),
                FindPasswordResult.class);

        passwordResetMark.mark(form.memberId(), form.nickname());
        return "redirect:" + RESET_URL;
    }

    /**
     * 실패하면 같은 폼을 다시 그린다.
     *
     * <p>{@code 2001} 도 {@code 1004} 도 폼 상단이다. 표에 없는 코드라 {@link FormFailure} 가
     * 상단으로 보내며, 그것이 이 화면에서 맞는 자리다 — 어느 칸이 틀렸는지 가르지 않는 것이
     * {@code 2001} 의 취지이고, {@code 1004} 는 칸의 문제가 아니라 계정 상태의 문제다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        FormFailure.applyTo(model, exception);
        return VIEW;
    }

    /** 백엔드 {@code MemberFindPassword} 요청 본문. */
    private record FindPasswordRequest(String memberId, String nickname) {
    }
}

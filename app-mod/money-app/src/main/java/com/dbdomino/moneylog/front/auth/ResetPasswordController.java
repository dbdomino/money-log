package com.dbdomino.moneylog.front.auth;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.auth.form.ResetPasswordForm;
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
 * 1.5 비밀번호 변경 화면.
 *
 * <h2>표식이 없으면 폼을 그리지 않는다</h2>
 *
 * <p>폼을 그려 두고 저장에서 실패시키지 않는다(FR-714). 그러면 사용자는 <b>비밀번호를 다 고른
 * 뒤에</b> "처음부터 다시 하세요"를 보게 된다.
 *
 * <h2>실패해도 표식을 지우지 않는다</h2>
 *
 * <p>지우면 규칙에 한 번 어긋난 사용자가 1.4 부터 다시 해야 한다. 지우는 곳은 <b>저장 성공
 * 한 곳뿐</b>이며, 남겨 두면 뒤로 가기로 돌아와 다시 바꿀 수 있다.
 *
 * <h2>완료는 같은 화면이다</h2>
 *
 * <p>완료 전용 주소를 만들지 않는다(FR-713). 같은 화면을 완료 상태로 그리고 잠시 뒤 로그인으로
 * 옮겨 가되 <b>「로그인으로」 버튼을 함께 둔다</b> — 자동 이동이 막힌 환경에서 사용자가
 * 갇히지 않게 한다.
 */
@Controller
public class ResetPasswordController {

    static final String VIEW = "auth/reset-password";

    private static final String RESET_PATH = "/auth/reset-password";

    private final BackendApiClient backendApiClient;
    private final PasswordResetMark passwordResetMark;

    public ResetPasswordController(BackendApiClient backendApiClient,
            PasswordResetMark passwordResetMark) {
        this.backendApiClient = backendApiClient;
        this.passwordResetMark = passwordResetMark;
    }

    /**
     * 표식이 있을 때만 폼을 그린다.
     *
     * <p>확인된 아이디를 화면에 보인다 — 누구의 비밀번호를 바꾸는지 알아야 한다.
     */
    @GetMapping("/auth/reset-password")
    public String form(Model model) {
        if (!passwordResetMark.exists()) {
            return "redirect:" + FindPasswordController.RESET_REQUIRED_URL;
        }
        putMark(model);
        return VIEW;
    }

    /**
     * 비밀번호를 바꾼다.
     *
     * <p>표식의 아이디·닉네임과 새 비밀번호 두 칸을 함께 보낸다. 화면이 실어 보내는 숨은 값을
     * 사용자가 바꿔도 <b>백엔드가 저장 시점에 다시 대조하므로</b> 남의 비밀번호를 바꿀 수 없다.
     * 그래서 여기서는 화면이 받은 숨은 값이 아니라 <b>표식의 값</b>을 쓴다 — 표식이 서버에
     * 있는 한 굳이 브라우저가 돌려준 값을 믿을 이유가 없다.
     */
    @PostMapping("/auth/reset-password")
    public String submit(
            @RequestParam(name = "newPassword", required = false) String newPassword,
            @RequestParam(name = "newPasswordConfirm", required = false) String newPasswordConfirm,
            Model model) {

        if (!passwordResetMark.exists()) {
            return "redirect:" + FindPasswordController.RESET_REQUIRED_URL;
        }

        ResetPasswordForm form = new ResetPasswordForm(newPassword, newPasswordConfirm);
        backendApiClient.postWithoutAuth(RESET_PATH,
                form.toRequest(passwordResetMark.memberId(), passwordResetMark.nickname()),
                Void.class);

        passwordResetMark.clear();
        model.addAttribute("resetDone", true);
        return VIEW;
    }

    /**
     * 실패하면 입력 단계를 다시 그린다. <b>표식은 그대로 둔다.</b>
     *
     * <p>다만 {@code 2001} 은 예외다 — 표식의 값이 더는 맞지 않는다는 뜻이라 이 화면에 머물러도
     * 사용자가 할 수 있는 일이 없다. 표식을 버리고 1.4 로 안내한다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        if (exception.getResCode() == ErrorCode.MEMBER_NOT_FOUND.code()) {
            passwordResetMark.clear();
            return "redirect:" + FindPasswordController.RESET_REQUIRED_URL;
        }
        FormFailure.applyTo(model, exception);
        putMark(model);
        return VIEW;
    }

    /** 확인된 아이디·닉네임을 화면에 싣는다. 실패로 다시 그릴 때도 그대로 실린다. */
    private void putMark(Model model) {
        model.addAttribute("resetMemberId", passwordResetMark.memberId());
        model.addAttribute("resetNickname", passwordResetMark.nickname());
    }
}

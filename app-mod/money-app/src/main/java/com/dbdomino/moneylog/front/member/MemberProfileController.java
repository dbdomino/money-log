package com.dbdomino.moneylog.front.member;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.member.form.MemberProfileForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 1.7 본인 정보 화면. 008 이 만드는 유일한 개인 화면이다.
 *
 * <h2>열릴 때마다 백엔드에서 받는다</h2>
 *
 * <p>값을 세션에 담아 두지 않는다. 담으면 관리자가 그 회원을 고쳤을 때 화면에 옛 값이 남고,
 * 사용자는 자기가 방금 본 것이 지금 값이라고 믿는다.
 *
 * <h2>빈 칸의 뜻이 칸마다 다르다</h2>
 *
 * <p>새 비밀번호를 비우면 <b>바꾸지 않고</b>, 이메일·폰·소개를 비우면 <b>지운다</b>. 새
 * 비밀번호만 다른 이유는 그 칸만 현재 값이 채워지지 않은 채 뜨기 때문이다 — 비어 있는 것이
 * 기본 상태이므로 "비웠다"가 아니라 "건드리지 않았다"로 읽는 것이 맞다. 가르는 일은
 * {@link com.dbdomino.moneylog.front.support.PatchBody} 가 한다.
 *
 * <h2>닉네임을 바꿔도 세션은 그대로다</h2>
 *
 * <p>세션이 들고 있는 것은 아이디와 권한뿐이고 둘 다 본인 수정으로 바뀌지 않는다. 상단바
 * 표시가 아이디라면 바뀌지 않는 것이 맞다.
 *
 * <p>진입 판정은 007 이 한다. 이 화면은 세션이 살아 있다는 전제로만 그려진다.
 */
@Controller
public class MemberProfileController {

    static final String VIEW = "member/profile";

    private static final String ME_PATH = "/members/me";

    private final BackendApiClient backendApiClient;

    public MemberProfileController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    /** 현재 값을 받아 칸을 채운다. 새 비밀번호 칸은 언제나 빈 채로 둔다. */
    @GetMapping("/member/profile")
    public String form(Model model) {
        putProfile(model);
        return VIEW;
    }

    /**
     * 고친 값을 저장한다.
     *
     * <p>브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로 옮긴다. HTML 폼이
     * 보낼 수 있는 메서드가 둘뿐이라 옮기는 일을 화면 모듈이 맡는다.
     */
    @PostMapping("/member/profile")
    public String submit(
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "intro", required = false) String intro,
            @RequestParam(name = "newPassword", required = false) String newPassword,
            Model model) {

        MemberProfileForm form =
                new MemberProfileForm(nickname, email, phone, intro, newPassword);

        MemberView saved = backendApiClient.patch(ME_PATH, form.toRequest(), MemberView.class);

        model.addAttribute("profile", saved);
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("saved", true);
        return VIEW;
    }

    /**
     * 실패하면 같은 폼을 다시 그린다.
     *
     * <p>{@code 2003} 은 이메일 칸, {@code 2004} 는 새 비밀번호 칸 가까이 붙는다(FR-717).
     * 자리를 가르는 것은 {@link FormFailure} 이고 이 처리는 착지만 정한다.
     *
     * <p>현재 값을 다시 받아 오는 이유는 아이디처럼 <b>폼이 돌려주지 않는 값</b>이 화면에
     * 필요하기 때문이다. 사용자가 고친 입력은 템플릿이 요청 파라미터에서 되읽어 덮어쓴다 —
     * 되받은 값으로 덮어 버리면 사용자가 방금 친 것이 사라진다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, Model model) {
        FormFailure.applyTo(model, exception);
        putProfile(model);
        return VIEW;
    }

    private void putProfile(Model model) {
        model.addAttribute("profile", backendApiClient.get(ME_PATH, MemberView.class));
        model.addAttribute("activeMenu", "profile");
    }
}

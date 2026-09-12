package com.dbdomino.moneylog.front.admin;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.admin.form.AdminMemberCreateForm;
import com.dbdomino.moneylog.front.admin.form.AdminMemberSearch;
import com.dbdomino.moneylog.front.admin.form.AdminMemberUpdateForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.member.MemberView;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import com.dbdomino.moneylog.front.web.Paging;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 1.8 회원 목록과 그 위의 모달 둘(1.9 추가 · 1.10 수정), 그리고 회원 정지.
 *
 * <h2>권한 판정을 다시 하지 않는다</h2>
 *
 * <p>{@code /admin} 아래 주소는 007 의 진입 판정이 이미 가린다. 미로그인은 로그인 화면으로,
 * 일반 권한은 권한 없음 화면으로 간다 — 그 순서까지 007 의 요구사항이다. 여기서 한 번 더
 * 판정하면 두 곳의 기준이 갈릴 수 있고, 갈린 쪽이 느슨하면 그것이 실제 기준이 된다.
 *
 * <h2>모달을 열기 전에 값을 가져온다</h2>
 *
 * <p>수정 모달은 <b>목록을 그리기 전에</b> 대상을 조회해 모델에 싣는다. 여는 일만 브라우저가
 * 하면 열린 모달이 빈 채로 뜨고, 값을 채우려면 브라우저가 다시 요청해야 한다.
 *
 * <p>모르는 식별자면 조회가 {@code 2001} 로 실패한다. 그때는 오류 화면이 아니라 <b>모달 없이
 * 목록만</b> 보이고 안내를 띄운다 — 낡은 북마크일 뿐이고 목록은 정상이다.
 *
 * <h2>되돌릴 수 없는 동작은 POST 다</h2>
 *
 * <p>정지를 링크로 두면 브라우저가 미리 불러오는 것만으로 계정이 정지된다. 백엔드 정지는
 * <b>본문 없는 수정 요청</b>이라 007 의 통로에 본문을 싣지 않고 보낸다.
 */
@Controller
public class AdminMemberController {

    static final String VIEW = "admin/members";

    static final String LIST_PATH = "/admin/members";
    static final String MEMBER_PATH = "/admin/members/{memberId}";
    static final String DEACTIVATE_PATH = "/admin/members/{memberId}/deactivate";

    /** 이 화면이 여는 모달. 목록은 007 이 정하지 않고 화면이 낸다. */
    private static final Set<String> MODALS = Set.of("create", "edit");

    /** 모달 값과 화면 안의 식별자를 잇는 지도. 007 의 딥링크 스크립트가 이것을 읽는다. */
    private static final String MODAL_MAP =
            "{\"create\":\"modal-member-create\",\"edit\":\"modal-member-edit\"}";

    private final BackendApiClient backendApiClient;

    public AdminMemberController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    // ── 목록과 모달 ─────────────────────────────────────────────────────

    /**
     * 목록을 그린다. 검색어 둘과 쪽 번호를 받는다.
     *
     * <p>조회 구간은 007 의 페이징 환산기가 만든다. 화면이 직접 계산하면 개수의 배수가 아닌
     * 값이 새어 나가 목록이 통째로 실패한다.
     */
    @GetMapping("/admin/members")
    public String list(
            @RequestParam(name = "memberId", required = false) String memberId,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "m", required = false) String modal,
            @RequestParam(name = "id", required = false) String targetId,
            Model model) {

        AdminMemberSearch search = AdminMemberSearch.of(memberId, nickname, page);
        putList(model, search);

        resolveModal(model, modal, targetId)
                .ifPresent(value -> model.addAttribute(ModalParam.MODEL_ATTRIBUTE, value));
        return VIEW;
    }

    /**
     * 어떤 모달을 열지 정하고, 수정이면 <b>열기 전에</b> 대상을 조회해 싣는다.
     *
     * <p>모달을 열지 않기로 한 경우 키 자체를 담지 않는다. 빈 값을 담으면 템플릿마다 "빈
     * 값인가 없는 값인가"를 가리는 분기가 생긴다 — 007 이 모달 판정을 그렇게 정했다.
     *
     * <p>식별자가 없으면 열지 않는다 — 누구를 고칠지 모르는 상태다. 모르는 식별자면 모달만
     * 접고 목록은 정상으로 둔다.
     */
    private Optional<String> resolveModal(Model model, String modal, String targetId) {
        Optional<String> resolved = ModalParam.resolve(modal, MODALS);
        if (resolved.isEmpty() || !"edit".equals(resolved.get())) {
            return resolved;
        }
        if (targetId == null || targetId.isBlank()) {
            return Optional.empty();
        }
        try {
            model.addAttribute("editTarget",
                    backendApiClient.get(MEMBER_PATH, MemberView.class, targetId));
            return resolved;
        } catch (BackendApiException e) {
            if (e.getResCode() != ErrorCode.MEMBER_NOT_FOUND.code()) {
                throw e;
            }
            model.addAttribute("notice", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 추가 · 수정 · 정지 ──────────────────────────────────────────────

    /** 회원을 만든다. 성공하면 목록을 다시 그리고 만든 회원이 목록에 보인다. */
    @PostMapping("/admin/members")
    public String create(
            @RequestParam(name = "memberId", required = false) String memberId,
            @RequestParam(name = "password", required = false) String password,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "role", required = false) Integer role,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "intro", required = false) String intro,
            Model model) {

        AdminMemberCreateForm form = new AdminMemberCreateForm(memberId, password, nickname, role,
                email, phone, intro);
        backendApiClient.post(LIST_PATH, form.toRequest(), MemberView.class);

        putList(model, AdminMemberSearch.of(null, null, null));
        model.addAttribute("notice", "회원을 추가했습니다.");
        return VIEW;
    }

    /**
     * 회원을 고친다. 빈 칸의 뜻은 본인 정보(1.7)와 같다.
     *
     * <p>브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로 옮긴다.
     */
    @PostMapping("/admin/members/{memberId}")
    public String update(
            @PathVariable String memberId,
            @RequestParam(name = "nickname", required = false) String nickname,
            @RequestParam(name = "role", required = false) Integer role,
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "phone", required = false) String phone,
            @RequestParam(name = "intro", required = false) String intro,
            @RequestParam(name = "newPassword", required = false) String newPassword,
            Model model) {

        AdminMemberUpdateForm form = new AdminMemberUpdateForm(nickname, role, email, phone, intro,
                newPassword);
        backendApiClient.patch(MEMBER_PATH, form.toRequest(), MemberView.class, memberId);

        putList(model, AdminMemberSearch.of(null, null, null));
        model.addAttribute("notice", "회원 정보를 수정했습니다.");
        return VIEW;
    }

    /**
     * 회원을 정지한다. 확인 다이얼로그를 거쳐 들어온다.
     *
     * <p>백엔드 정지는 본문 없는 수정 요청이다. {@code null} 을 본문으로 넘기면 007 의 통로가
     * 본문을 싣지 않는다.
     */
    @PostMapping("/admin/members/{memberId}/deactivate")
    public String deactivate(@PathVariable String memberId, Model model) {
        backendApiClient.patch(DEACTIVATE_PATH, null, MemberView.class, memberId);

        putList(model, AdminMemberSearch.of(null, null, null));
        model.addAttribute("notice", "회원을 정지했습니다.");
        return VIEW;
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>모달을 닫아 버리면 관리자가 채운 칸이 전부 사라진다. 어느 모달이었는지는 <b>요청
     * 주소가 이미 말해 준다</b> — 목록 주소로 온 제출이면 추가, 회원 주소로 온 제출이면
     * 수정이다. 판정에 다시 물을 필요가 없다.
     *
     * <p>정지 실패({@code 2001} 회원 없음 · {@code 9001} 이미 정지)는 모달이 없으므로 목록의
     * 안내로만 보인다. 이미 원하는 상태이거나 이미 사라진 회원이라 관리자가 할 일이 없는데
     * 오류 화면으로 보내면 목록으로 돌아오는 걸음만 늘어난다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        putList(model, AdminMemberSearch.of(null, null, null));

        String path = request.getRequestURI();
        if (path.endsWith("/deactivate")) {
            return VIEW;
        }
        if (LIST_PATH.equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "create");
            return VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "edit");
        model.addAttribute("editTargetId", path.substring(path.lastIndexOf('/') + 1));
        return VIEW;
    }

    // ── 모델 채우기 ─────────────────────────────────────────────────────

    private void putList(Model model, AdminMemberSearch search) {
        AdminMemberListResult result =
                backendApiClient.getByQuery(LIST_PATH, search.toQuery(), AdminMemberListResult.class);
        if (result == null) {
            // 백엔드가 성공 봉투에 빈 값을 담아 보낸 경우다. 목록이 없는 것이지 화면이
            // 고장 난 것이 아니므로 빈 쪽으로 그린다 — 여기서 터지면 관리자는 회원 관리가
            // 통째로 죽었다고 읽는다.
            result = new AdminMemberListResult(List.of(), search.paging().offset(),
                    search.paging().limit(), 0);
        }

        model.addAttribute("members", result.rows());
        model.addAttribute("search", search);
        model.addAttribute("page", search.page());
        model.addAttribute("totalCount", result.totalCount());
        model.addAttribute("totalPages", Paging.totalPages(result.totalCount(), result.limit()));
        model.addAttribute("activeMenu", "admin-members");
        model.addAttribute("modalMap", MODAL_MAP);
    }
}

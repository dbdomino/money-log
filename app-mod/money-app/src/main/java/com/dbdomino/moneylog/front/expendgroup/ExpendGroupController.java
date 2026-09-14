package com.dbdomino.moneylog.front.expendgroup;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.expendgroup.form.ExpendGroupForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 2.5 지출유형 목록과 그 위의 모달 셋(2.6 등록 · 2.7 상세 · 2.8 수정), 그리고 삭제.
 *
 * <p>수단(2.1~2.4)과 같은 모양이며 <b>다른 것은 아이콘 하나</b>다.
 *
 * <h2>아이콘은 어느 방향으로도 브라우저가 백엔드를 부르지 않는다</h2>
 *
 * <p>올릴 때는 이 화면 모듈을 거치고, 보일 때는 007 의 중계를 거친다. 백엔드가 준 주소는
 * 여기서 <b>파일명으로 바뀌어</b> 화면에 실린다 — 주소 전체가 화면까지 가면 그것이 이미지에
 * 걸리고 인증이 붙지 않아 전부 실패한다.
 *
 * <h2>기본 유형은 화면이 미리 막는다</h2>
 *
 * <p>목록 응답이 기본 유형인지 알려 주므로 이름 칸을 잠그고 삭제 버튼을 그리지 않는다. 수단의
 * 용도와 다른 점이며, 그쪽은 참조 건수를 알 방법이 없어 미리 막지 못한다.
 *
 * <p><b>그래도 서버 실패를 함께 다룬다.</b> 주소를 직접 쳐서 올 수 있고, 화면이 막는 것과
 * 서버가 막는 것은 서로를 대신하지 않는다.
 */
@Controller
public class ExpendGroupController {

    static final String VIEW = "expend-groups/list";

    static final String LIST_PATH = "/expend-groups";
    static final String ITEM_PATH = "/expend-groups/{expendGroupId}";

    /** 이 화면이 여는 모달. */
    private static final Set<String> MODALS = Set.of("create", "detail", "edit");

    private static final String MODAL_MAP = "{\"create\":\"modal-expend-group-create\","
            + "\"detail\":\"modal-expend-group-detail\",\"edit\":\"modal-expend-group-edit\"}";

    private final BackendApiClient backendApiClient;

    public ExpendGroupController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    // ── 목록과 모달 ─────────────────────────────────────────────────────

    @GetMapping("/expend-groups")
    public String list(
            @RequestParam(name = "m", required = false) String modal,
            @RequestParam(name = "id", required = false) Long targetId,
            Model model) {

        putList(model);
        resolveModal(model, modal, targetId)
                .ifPresent(value -> model.addAttribute(ModalParam.MODEL_ATTRIBUTE, value));
        return VIEW;
    }

    /**
     * 어떤 모달을 열지 정하고, 상세·수정이면 <b>열기 전에</b> 대상을 조회해 싣는다.
     *
     * <p>모르는 식별자면 모달만 접고 목록은 정상으로 둔다. <b>없는 것과 남의 것을 가르지
     * 않는다</b> — 가르면 식별자를 훑어 남의 유형이 실재하는지 알아낼 수 있다.
     */
    private Optional<String> resolveModal(Model model, String modal, Long targetId) {
        Optional<String> resolved = ModalParam.resolve(modal, MODALS);
        if (resolved.isEmpty() || "create".equals(resolved.get())) {
            return resolved;
        }
        if (targetId == null) {
            return Optional.empty();
        }
        try {
            ExpendGroupResponse response =
                    backendApiClient.get(ITEM_PATH, ExpendGroupResponse.class, targetId);
            model.addAttribute("target", ExpendGroupView.from(response));
            return resolved;
        } catch (BackendApiException e) {
            if (e.getResCode() != ErrorCode.EXPEND_GROUP_NOT_FOUND.code()) {
                throw e;
            }
            model.addAttribute("notice", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 등록 · 수정 · 삭제 ──────────────────────────────────────────────

    /** 유형을 만든다. 파일을 실은 생성 요청으로 나간다. */
    @PostMapping("/expend-groups")
    public String create(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "inUse", required = false) Boolean inUse,
            @RequestParam(name = "iconFile", required = false) MultipartFile iconFile,
            Model model) {

        ExpendGroupForm form = new ExpendGroupForm(name, inUse, iconFile);
        backendApiClient.postMultipart(LIST_PATH, form.toParts(), ExpendGroupResponse.class);

        putList(model);
        model.addAttribute("notice", "지출유형을 등록했습니다.");
        return VIEW;
    }

    /**
     * 유형을 고친다. <b>파일을 실은 수정</b>이라 009 가 007 에 더한 통로로 나간다.
     *
     * <p>파일을 고르지 않았으면 그 칸을 빼고 보내 기존 아이콘이 남는다. 브라우저는
     * {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로 옮긴다.
     */
    @PostMapping("/expend-groups/{expendGroupId}")
    public String update(
            @PathVariable Long expendGroupId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "inUse", required = false) Boolean inUse,
            @RequestParam(name = "iconFile", required = false) MultipartFile iconFile,
            Model model) {

        ExpendGroupForm form = new ExpendGroupForm(name, inUse, iconFile);
        backendApiClient.patchMultipart(ITEM_PATH, form.toParts(), ExpendGroupResponse.class,
                expendGroupId);

        putList(model);
        model.addAttribute("notice", "지출유형을 수정했습니다.");
        return VIEW;
    }

    /** 유형을 삭제 표시한다. 확인 다이얼로그를 거쳐 들어온다. */
    @PostMapping("/expend-groups/{expendGroupId}/delete")
    public String delete(@PathVariable Long expendGroupId, Model model) {
        backendApiClient.delete(ITEM_PATH, expendGroupId);

        putList(model);
        model.addAttribute("notice", "지출유형을 삭제했습니다. 그 이름은 계속 점유됩니다.");
        return VIEW;
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>삭제 실패는 모달이 없으므로 목록의 안내로만 보인다. 그중 <b>사용 중인 유형의
     * 삭제만 대안을 함께 제시한다</b> — 나머지는 사용자가 할 수 있는 일이 없지만 이 경우는
     * 목적을 이룰 다른 길이 있다. 과거 지출이 그 유형을 가리켜야 하므로 지울 수 없지만,
     * 앞으로 쓰지 않게 할 수는 있다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyExpendGroupField(model, exception);
        putList(model);

        String path = request.getRequestURI();
        if (path.endsWith("/delete")) {
            if (exception.getResCode() == ErrorCode.EXPEND_GROUP_IN_USE.code()) {
                model.addAttribute("alternative",
                        "지울 수 없다면 「사용 안 함」으로 돌려 두세요. 새 지출에서 고를 수 없게 됩니다.");
            }
            return VIEW;
        }
        if (LIST_PATH.equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "create");
            return VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "edit");
        model.addAttribute("targetId", path.substring(path.lastIndexOf('/') + 1));
        return VIEW;
    }

    /**
     * 지출유형에만 있는 코드를 칸에 잇는다.
     *
     * <p>이름 중복과 기본 유형의 이름 변경은 이름 칸, 아이콘 오류는 아이콘 칸이다.
     * <b>이름 중복에는 삭제된 이름도 센다는 것을 함께 적는다</b> — 적지 않으면 사용자는
     * 삭제한 것과 겹칠 리 없다고 생각해 화면이 잘못됐다고 읽는다.
     */
    private static void applyExpendGroupField(Model model, BackendApiException exception) {
        int code = exception.getResCode();
        if (code == ErrorCode.EXPEND_GROUP_NAME_DUPLICATED.code()) {
            model.addAttribute(FormFailure.FIELD, "name");
            model.addAttribute("nameHint", "삭제 표시된 유형의 이름도 겹침으로 셉니다.");
        } else if (code == ErrorCode.EXPEND_GROUP_DEFAULT_NAME_LOCKED.code()) {
            model.addAttribute(FormFailure.FIELD, "name");
        } else if (code == ErrorCode.EXPEND_GROUP_ICON_INVALID.code()) {
            model.addAttribute(FormFailure.FIELD, "iconFile");
        }
    }

    // ── 모델 채우기 ─────────────────────────────────────────────────────

    /**
     * 목록을 조회해 모델에 담는다. <b>아이콘 주소를 파일명으로 바꾸는 곳이 여기다.</b>
     *
     * <p>조회 구간을 싣지 않으며, 응답이 비어 와도 빈 목록으로 그린다.
     */
    private void putList(Model model) {
        ExpendGroupListResult result =
                backendApiClient.getByQuery(LIST_PATH, Map.of(), ExpendGroupListResult.class);

        List<ExpendGroupView> groups = result == null
                ? List.of()
                : result.rows().stream().map(ExpendGroupView::from).toList();

        model.addAttribute("groups", groups);
        model.addAttribute("activeMenu", "expend-groups");
        model.addAttribute("modalMap", MODAL_MAP);
    }
}

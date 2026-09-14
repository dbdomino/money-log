package com.dbdomino.moneylog.front.payment;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.payment.form.PaymentMethodForm;
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

/**
 * 2.1 수단 목록과 그 위의 모달 셋(2.2 등록 · 2.3 상세 · 2.4 수정), 그리고 삭제.
 *
 * <p><b>목록 하나에 모달 셋</b>이라는 모양을 처음 세우는 자리다. 지출유형이 이것을 그대로
 * 따르고 010~012 의 화면 넷이 다시 따른다.
 *
 * <h2>쪽 넘기기가 없다</h2>
 *
 * <p>백엔드 목록이 조회 구간을 받지 않고 본인 것을 전부 돌려준다. 007 의 페이징 환산기를
 * 쓰지 않는다 — 받지 않는 값을 실어 보낼 이유가 없다. <b>010~012 는 다르다</b>: 가계부·
 * 고정지출 목록에는 쪽 넘기기가 있고 그쪽은 환산기를 써야 한다.
 *
 * <h2>모달을 열기 전에 값을 가져온다</h2>
 *
 * <p>상세·수정은 <b>목록을 그리기 전에</b> 대상을 조회해 모델에 싣는다. 여는 일만 브라우저가
 * 하면 열린 모달이 빈 채로 뜨고 값을 채우려면 다시 요청해야 한다.
 *
 * <h2>되돌릴 수 없는 동작은 POST 다</h2>
 *
 * <p>삭제를 링크로 두면 브라우저가 미리 불러오는 것만으로 실행된다.
 */
@Controller
public class PaymentMethodController {

    static final String VIEW = "payments/list";

    static final String LIST_PATH = "/payment-methods";
    static final String ITEM_PATH = "/payment-methods/{paymentMethodId}";

    /** 이 화면이 여는 모달. 목록은 007 이 정하지 않고 화면이 낸다. */
    private static final Set<String> MODALS = Set.of("create", "detail", "edit");

    /** 모달 값과 화면 안의 식별자를 잇는 지도. 007 의 딥링크 스크립트가 이것을 읽는다. */
    private static final String MODAL_MAP = "{\"create\":\"modal-payment-create\","
            + "\"detail\":\"modal-payment-detail\",\"edit\":\"modal-payment-edit\"}";

    private final BackendApiClient backendApiClient;

    public PaymentMethodController(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    // ── 목록과 모달 ─────────────────────────────────────────────────────

    @GetMapping("/payments")
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
     * <p>모달을 열지 않기로 한 경우 키 자체를 담지 않는다 — 007 이 모달 판정을 그렇게 정했다.
     * 식별자가 없으면 열지 않고, 모르는 식별자면 모달만 접고 목록은 정상으로 둔다.
     *
     * <p><b>없는 것과 남의 것을 가르지 않는다.</b> 백엔드가 한 코드로 묶었고 화면도 풀지
     * 않는다 — 가르면 식별자를 훑어 남의 수단이 실재하는지 알아낼 수 있다.
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
            model.addAttribute("target",
                    backendApiClient.get(ITEM_PATH, PaymentMethodView.class, targetId));
            return resolved;
        } catch (BackendApiException e) {
            if (e.getResCode() != ErrorCode.PAYMENT_METHOD_NOT_FOUND.code()) {
                throw e;
            }
            model.addAttribute("notice", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 등록 · 수정 · 삭제 ──────────────────────────────────────────────

    @PostMapping("/payments")
    public String create(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "purpose", required = false) String purpose,
            @RequestParam(name = "inUse", required = false) Boolean inUse,
            @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
            Model model) {

        PaymentMethodForm form = new PaymentMethodForm(name, type, purpose, inUse, cardExpiry);
        backendApiClient.post(LIST_PATH, form.toCreateRequest(), PaymentMethodView.class);

        putList(model);
        model.addAttribute("notice", "수단을 등록했습니다.");
        return VIEW;
    }

    /**
     * 수단을 고친다. 브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로 옮긴다.
     *
     * <p>용도를 바꿀 수 있는지는 <b>저장해 봐야 안다</b> — 참조 건수를 화면이 알 방법이 없다.
     * 거절당하면 실패 착지가 용도 칸 가까이 사유를 보인다.
     */
    @PostMapping("/payments/{paymentMethodId}")
    public String update(
            @PathVariable Long paymentMethodId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "purpose", required = false) String purpose,
            @RequestParam(name = "inUse", required = false) Boolean inUse,
            @RequestParam(name = "cardExpiry", required = false) String cardExpiry,
            Model model) {

        PaymentMethodForm form = new PaymentMethodForm(name, type, purpose, inUse, cardExpiry);
        backendApiClient.patch(ITEM_PATH, form.toUpdateRequest(), PaymentMethodView.class,
                paymentMethodId);

        putList(model);
        model.addAttribute("notice", "수단을 수정했습니다.");
        return VIEW;
    }

    /** 수단을 삭제 표시한다. 확인 다이얼로그를 거쳐 들어온다. */
    @PostMapping("/payments/{paymentMethodId}/delete")
    public String delete(@PathVariable Long paymentMethodId, Model model) {
        backendApiClient.delete(ITEM_PATH, paymentMethodId);

        putList(model);
        model.addAttribute("notice", "수단을 삭제했습니다. 과거 내역의 수단 이름은 그대로 남습니다.");
        return VIEW;
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다. 어느 모달이었는지는 <b>요청
     * 주소가 이미 말해 준다</b> — 목록 주소로 온 제출이면 등록, 항목 주소면 수정이다.
     *
     * <p>삭제 실패는 모달이 없으므로 목록의 안내로만 보인다. 이미 삭제된 수단을 다시 지우려
     * 한 경우가 그러한데, <b>이미 원하는 상태라 사용자가 할 일이 없다</b> — 오류 화면으로
     * 보내면 목록으로 돌아오는 걸음만 늘어난다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyPaymentField(model, exception);
        putList(model);

        String path = request.getRequestURI();
        if (path.endsWith("/delete")) {
            return VIEW;
        }
        if ("/payments".equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "create");
            return VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, "edit");
        model.addAttribute("targetId", path.substring(path.lastIndexOf('/') + 1));
        return VIEW;
    }

    /**
     * 수단에만 있는 코드를 칸에 잇는다. 008 의 표에는 회원 코드만 있다.
     *
     * <p>유효기간 형식 오류는 그 칸, 용도를 바꿀 수 없다는 것은 용도 칸이다. <b>구분·용도 값
     * 오류는 상단에 둔다</b> — 한 코드가 두 칸을 함께 가리켜 어느 칸인지 가릴 수 없고,
     * 짐작해 아무 칸에나 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
     */
    private static void applyPaymentField(Model model, BackendApiException exception) {
        int code = exception.getResCode();
        if (code == ErrorCode.PAYMENT_METHOD_EXPIRY_INVALID.code()) {
            model.addAttribute(FormFailure.FIELD, "cardExpiry");
        } else if (code == ErrorCode.PAYMENT_METHOD_PURPOSE_LOCKED.code()) {
            model.addAttribute(FormFailure.FIELD, "purpose");
        }
    }

    // ── 모델 채우기 ─────────────────────────────────────────────────────

    /**
     * 목록을 조회해 모델에 담는다. <b>조회 구간을 싣지 않는다.</b>
     *
     * <p>응답이 비어 와도 빈 목록으로 그린다 — 여기서 터지면 사용자는 수단 관리가 통째로
     * 죽었다고 읽는다.
     */
    private void putList(Model model) {
        PaymentMethodListResult result =
                backendApiClient.getByQuery(LIST_PATH, Map.of(), PaymentMethodListResult.class);

        model.addAttribute("methods", result == null ? List.of() : result.rows());
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("modalMap", MODAL_MAP);
    }
}

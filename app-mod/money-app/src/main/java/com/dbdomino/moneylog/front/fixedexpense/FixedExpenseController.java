package com.dbdomino.moneylog.front.fixedexpense;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.fixedexpense.form.FixedExpenseForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import jakarta.servlet.http.HttpServletRequest;
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
 * 4.2 고정지출 설정 목록과 그 위의 모달 셋(4.3 등록 · 4.4 상세 · 4.5 수정), 그리고 삭제.
 *
 * <p><b>010 이 읽기만 하던 것을 만드는 자리다.</b> 가계부의 고정지출 행에는 수정·삭제가
 * 없고 대신 이 화면으로 오는 안내가 있다.
 *
 * <h2>컨트롤러가 둘이다</h2>
 *
 * <p>009 는 자원 하나에 한 컨트롤러, 010 은 한 부모에 자원 둘이라 넷이었다. 011 은 <b>한
 * 부모에 저장 단위 둘</b>이 얹힌다 — 설정과 그 달 내역이다.
 *
 * <p><b>나누는 이유가 010 과 다르다.</b> 다루는 저장 단위가 다르고 <b>수정의 뜻도
 * 다르다</b> — 여기의 수정은 미래 달로 번지고 {@code FixedExpenseMonthlyController} 의
 * 수정은 그 달에서 끝난다. 한 파일에 두면 <b>두 개의 "수정"이 나란히 놓여</b> 읽는 사람이
 * 매번 어느 쪽인지 세야 한다.
 *
 * <h2>쪽 넘기기가 있다</h2>
 *
 * <p>009·010 과 다르다. 백엔드가 시작점과 개수를 <b>필수로</b> 받으며 시작점이 개수의
 * 배수가 아니면 목록이 통째로 실패한다 — 007 의 환산기를 쓴다.
 *
 * <h2>되돌릴 수 없는 동작은 POST 다</h2>
 *
 * <p>삭제를 링크로 두면 브라우저가 미리 불러오는 것만으로 실행된다.
 */
@Controller
public class FixedExpenseController {

    /**
     * 이 컨트롤러가 여는 모달 셋. <b>넷 중 {@code monthly} 는 빠져 있다.</b>
     *
     * <p>셋은 설정 하나를 다루는데 그것은 <b>그 달 전체</b>를 본다 — 다루는 저장 단위가
     * 다르고 대상 식별자도 싣지 않는다. 그래서 {@code FixedExpenseMonthlyController} 가
     * 맡고, 이 목록 매핑은 {@code m!=monthly} 로 그 요청을 비켜 준다.
     *
     * <p><b>주소는 둘이 같다</b>({@code /fixed-expenses}) — 모달이 부모 목록 위에 얹히는
     * 구조라 그래야 하고, 갈라지는 자리는 매핑의 조건 하나뿐이다.
     */
    private static final Set<String> MODALS = Set.of(
            FixedExpensePageModel.MODAL_CREATE, FixedExpensePageModel.MODAL_DETAIL,
            FixedExpensePageModel.MODAL_EDIT);

    private final FixedExpensePageModel pageModel;
    private final BackendApiClient backendApiClient;

    public FixedExpenseController(FixedExpensePageModel pageModel,
            BackendApiClient backendApiClient) {
        this.pageModel = pageModel;
        this.backendApiClient = backendApiClient;
    }

    // ── 목록과 모달 ─────────────────────────────────────────────────────

    /**
     * 설정 목록을 그리고, 요청에 모달이 실려 있으면 함께 연다.
     *
     * <p><b>사용자가 시작점을 직접 넣는 칸을 두지 않는다</b> — 쪽 번호만 받고 환산기가
     * 시작점을 만든다.
     */
    @GetMapping(value = "/fixed-expenses", params = "m!=monthly")
    public String list(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "m", required = false) String modal,
            @RequestParam(name = "id", required = false) Long targetId,
            Model model) {

        pageModel.putList(model, FixedExpensePageModel.pagingOf(page));
        resolveModal(model, modal, targetId)
                .ifPresent(value -> model.addAttribute(ModalParam.MODEL_ATTRIBUTE, value));
        return FixedExpensePageModel.VIEW;
    }

    /**
     * 어떤 모달을 열지 정하고, 상세·수정이면 <b>목록을 그리기 전에</b> 대상을 조회해 싣는다.
     *
     * <p>여는 일만 브라우저가 하면 열린 모달이 빈 채로 뜨고 값을 채우려면 다시 요청해야
     * 한다. 모달을 열지 않기로 한 경우 <b>키 자체를 담지 않는다</b> — 007 이 모달 판정을
     * 그렇게 정했다.
     *
     * <p><b>없는 것과 남의 것을 가르지 않는다.</b> 백엔드가 한 코드로 묶었고 화면도 풀지
     * 않는다 — 가르면 식별자를 훑어 남의 고정지출이 실재하는지 알아낼 수 있다.
     *
     * <p><b>월별 내역은 여기 오지 않는다.</b> 그 달 전체를 보는 화면이라 가리킬 설정이
     * 없고, 목록과 단건 수정을 {@code FixedExpenseMonthlyController} 가 맡는다.
     */
    private Optional<String> resolveModal(Model model, String modal, Long targetId) {
        Optional<String> resolved = ModalParam.resolve(modal, MODALS);
        if (resolved.isEmpty()) {
            return resolved;
        }

        String value = resolved.get();
        if (FixedExpensePageModel.MODAL_CREATE.equals(value)) {
            return resolved;
        }

        // 무엇을 조회할지 알 수 없으면 열지 않는다. 빈 모달을 띄우지 않는다.
        if (targetId == null) {
            return Optional.empty();
        }

        try {
            model.addAttribute("target", backendApiClient.get(
                    FixedExpensePageModel.ITEM_PATH, FixedExpenseView.class, targetId));
            return resolved;
        } catch (BackendApiException e) {
            if (e.getResCode() != ErrorCode.FIXED_EXPENSE_NOT_FOUND.code()) {
                throw e;
            }
            // 빈 모달이 아니라 목록만 남긴다. 사용자는 하려던 일을 이어서 할 수 있다.
            model.addAttribute("notice", e.getMessage());
            return Optional.empty();
        }
    }

    // ── 등록 · 수정 · 삭제 ──────────────────────────────────────────────

    /**
     * 고정지출 설정을 등록한다. <b>적용 기간이 정수 네 칸으로 나간다.</b>
     *
     * <p>010 의 할부가 문자열 한 칸이라 한쪽 규칙을 옮기면 <b>등록이 형식 오류로 막힌다</b> —
     * 가르는 자리를 {@link FixedExpenseForm} 하나에 두었다.
     */
    @PostMapping("/fixed-expenses")
    public String create(FixedExpenseParams params, Model model) {
        backendApiClient.post(FixedExpensePageModel.LIST_PATH, params.toForm().toRequest(),
                Void.class);
        return redraw(model, params, "고정지출을 등록했습니다.");
    }

    /**
     * 설정을 고친다. 브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로
     * 옮긴다.
     *
     * <p><b>응답에 드러나지 않는 부작용이 있다</b> — 백엔드가 <b>미래 달이면서 직접 고치지
     * 않은 월별 내역</b>을 함께 갱신한다. 그 사실을 저장 전에 알리는 일은 US2 가 모달에
     * 붙이며, 여기서는 저장만 한다.
     */
    @PostMapping("/fixed-expenses/{fixedExpenseId}")
    public String update(@PathVariable Long fixedExpenseId, FixedExpenseParams params,
            Model model) {

        backendApiClient.patch(FixedExpensePageModel.ITEM_PATH, params.toForm().toRequest(),
                Void.class, fixedExpenseId);
        return redraw(model, params, "고정지출을 수정했습니다.");
    }

    /**
     * 설정을 지운다. 확인 다이얼로그를 거쳐 들어온다.
     *
     * <p><b>009 와 다르다.</b> 수단·지출유형은 삭제 표시라 행이 남았고, 고정지출은 <b>물리
     * 삭제</b>이며 <b>월별 내역이 지난 달 것까지 함께 사라진다.</b>
     */
    @PostMapping("/fixed-expenses/{fixedExpenseId}/delete")
    public String delete(@PathVariable Long fixedExpenseId, FixedExpenseParams params,
            Model model) {

        backendApiClient.delete(FixedExpensePageModel.ITEM_PATH, fixedExpenseId);
        return redraw(model, params, "고정지출을 삭제했습니다. 월별 내역도 함께 사라졌습니다.");
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다. 어느 모달이었는지는 <b>요청
     * 주소가 이미 말해 준다</b> — 목록 주소로 온 제출이면 등록, 항목 주소면 수정이다.
     *
     * <p><b>삭제는 모달이 없다.</b> 목록의 안내로만 보인다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyFixedExpenseField(model, exception);
        pageModel.putList(model, FixedExpensePageModel.pagingOf(intParam(request, "page")));

        String path = request.getRequestURI();
        if (path.endsWith("/delete")) {
            return FixedExpensePageModel.VIEW;
        }
        if (FixedExpensePageModel.LIST_PATH.equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_CREATE);
            return FixedExpensePageModel.VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_EDIT);
        model.addAttribute("targetId", path.substring(path.lastIndexOf('/') + 1));
        return FixedExpensePageModel.VIEW;
    }

    /**
     * 고정지출에만 있는 코드를 칸에 잇는다.
     *
     * <p><b>값 오류를 상단에 두는 것이 요점이다.</b> 한 코드가 결제일·금액·기간 오류와
     * <b>수단 용도 불일치</b>를 겸해 <b>코드만으로는 어느 칸인지 가릴 수 없다</b> — 짐작해
     * 결제일 칸에 붙이면 수단을 잘못 고른 사용자가 결제일을 고치고 또 틀린다.
     *
     * <p>그 자리에서는 <b>서버 문구를 그대로</b> 보인다. 용도 불일치일 때 그 문구가
     * 「지출용 수단을 고르라」를 말해 준다 — 화면이 「수단이 없다」로 바꿔 적지 않는다.
     */
    private static void applyFixedExpenseField(Model model, BackendApiException exception) {
        int code = exception.getResCode();
        if (code == ErrorCode.PAYMENT_METHOD_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "paymentMethodId");
        } else if (code == ErrorCode.EXPEND_GROUP_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "expendGroupId");
        }
        // 값 오류(FIXED_EXPENSE_FIELD_INVALID)는 표에 없으므로 상단으로 간다.
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    /**
     * 실패 착지가 쪽 번호를 요청에서 직접 읽는다.
     *
     * <p>{@code @ExceptionHandler} 는 정해진 인자만 받고 <b>폼 객체를 받지 못한다.</b>
     * 그것을 인자로 두면 처리 메서드가 아예 호출되지 않아 예외가 오류 화면까지 올라가고,
     * 증상은 "모달 제출이 실패하면 목록이 아니라 오류 화면이 뜬다"로만 나타난다.
     */
    private static Integer intParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 성공한 뒤 목록을 다시 그린다. 쪽 번호는 제출에 실려 온 값을 그대로 쓴다. */
    private String redraw(Model model, FixedExpenseParams params, String notice) {
        pageModel.putList(model, FixedExpensePageModel.pagingOf(params.page));
        model.addAttribute("notice", notice);
        return FixedExpensePageModel.VIEW;
    }

    /**
     * 설정 폼이 보내는 값 묶음.
     *
     * <p>쪽 번호를 함께 받는 이유는 <b>실패 착지가 같은 쪽을 다시 그려야 하기 때문</b>이다 —
     * 없으면 셋째 쪽에서 저장에 실패했는데 첫 쪽이 뜬다.
     */
    public static class FixedExpenseParams {

        private String name;
        private Long paymentMethodId;
        private Long expendGroupId;
        private Long amount;
        private Integer paymentDayOfMonth;
        private String content;
        private Integer startYear;
        private Integer startMonth;
        private Integer endYear;
        private Integer endMonth;
        private Integer page;

        FixedExpenseForm toForm() {
            return new FixedExpenseForm(name, paymentMethodId, expendGroupId, amount,
                    paymentDayOfMonth, content, startYear, startMonth, endYear, endMonth);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPaymentMethodId(Long paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }

        public void setExpendGroupId(Long expendGroupId) {
            this.expendGroupId = expendGroupId;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public void setPaymentDayOfMonth(Integer paymentDayOfMonth) {
            this.paymentDayOfMonth = paymentDayOfMonth;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setStartYear(Integer startYear) {
            this.startYear = startYear;
        }

        public void setStartMonth(Integer startMonth) {
            this.startMonth = startMonth;
        }

        public void setEndYear(Integer endYear) {
            this.endYear = endYear;
        }

        public void setEndMonth(Integer endMonth) {
            this.endMonth = endMonth;
        }

        public void setPage(Integer page) {
            this.page = page;
        }
    }
}

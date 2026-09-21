package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.ledger.form.IncomeForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 소득의 등록(3.3) · 수정(3.4) · 삭제.
 *
 * <p>지출과 나눈 이유는 <b>폼·검증·실패 착지가 섞이면 어느 코드가 어느 자원의 것인지 읽어서
 * 세야 하기 때문</b>이다.
 *
 * <h2>지출보다 다루는 것이 적다</h2>
 *
 * <p>칸이 넷이고 통로가 하나다 — 소득에 할부라는 개념이 없어 갈릴 것이 없고, 중도상환도
 * 없다. <b>지출유형·장소 칸도 없다</b>: 백엔드가 받지 않아 두면 사용자가 채운 값이 조용히
 * 버려진다.
 *
 * <h2>목록을 다시 그리는 일은 함께 쓴다</h2>
 *
 * <p>{@link LedgerPageModel} 을 지출 쪽과 같이 쓴다. 복사하면 <b>지출 모달에서 실패했을
 * 때와 소득 모달에서 실패했을 때 목록이 달라 보이는</b> 차이가 화면에 남는다.
 */
@Controller
public class IncomeController {

    static final String CREATE_PATH = "/incomes";
    static final String ITEM_PATH = "/incomes/{incomeId}";

    private final BackendApiClient backendApiClient;
    private final LedgerPageModel pageModel;

    public IncomeController(BackendApiClient backendApiClient, LedgerPageModel pageModel) {
        this.backendApiClient = backendApiClient;
        this.pageModel = pageModel;
    }

    // ── 등록 · 수정 · 삭제 ──────────────────────────────────────────────

    /** 소득을 등록한다. 내용은 선택이라 비어 있으면 싣지 않는다. */
    @PostMapping("/ledger/incomes")
    public String create(IncomeParams params, Model model) {
        backendApiClient.post(CREATE_PATH, params.toForm().toCreateRequest(), Void.class);
        return redraw(model, params, "소득을 등록했습니다.");
    }

    /**
     * 소득을 고친다. 브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로
     * 옮긴다.
     *
     * <p><b>내용을 비우면 비우라는 뜻으로 보낸다.</b> 현재 값이 채워진 채로 뜨는 칸이라
     * 사용자가 지웠다면 지우려는 뜻이다.
     */
    @PostMapping("/ledger/incomes/{incomeId}")
    public String update(@PathVariable Long incomeId, IncomeParams params, Model model) {
        backendApiClient.patch(ITEM_PATH, params.toForm().toUpdateRequest(), Void.class, incomeId);
        return redraw(model, params, "소득을 수정했습니다.");
    }

    /** 소득을 지운다. 확인 다이얼로그를 거쳐 들어온다. */
    @PostMapping("/ledger/incomes/{incomeId}/delete")
    public String delete(@PathVariable Long incomeId, IncomeParams params, Model model) {
        backendApiClient.delete(ITEM_PATH, incomeId);
        return redraw(model, params, "소득을 삭제했습니다. 이 달 합계에서도 빠집니다.");
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>어느 모달이었는지는 요청 주소가 말해 준다. <b>삭제는 모달이 없어</b> 목록의 안내로만
     * 보인다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyIncomeField(model, exception);
        // @ExceptionHandler 는 폼 객체를 인자로 받지 못한다. 연·월을 요청에서 직접 읽는다 —
        // 폼 객체를 두면 처리 메서드가 호출되지 않아 예외가 오류 화면까지 올라간다.
        pageModel.putList(model, LedgerQuery.of(intParam(request, "year"),
                intParam(request, "month")));

        String path = request.getRequestURI();
        if (path.endsWith("/delete")) {
            return LedgerPageModel.VIEW;
        }

        if ("/ledger/incomes".equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, LedgerPageModel.MODAL_INCOME_CREATE);
            return LedgerPageModel.VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, LedgerPageModel.MODAL_INCOME_EDIT);
        model.addAttribute("targetId", path.substring(path.lastIndexOf('/') + 1));
        return LedgerPageModel.VIEW;
    }

    /**
     * 소득에만 있는 코드를 칸에 잇는다.
     *
     * <p>수단 실패만 칸을 얻는다. <b>소득 값 오류는 상단</b>이다 — 한 코드가 금액·날짜·내용을
     * 함께 가리켜 어느 칸인지 가릴 수 없다.
     */
    private static void applyIncomeField(Model model, BackendApiException exception) {
        if (exception.getResCode() == ErrorCode.PAYMENT_METHOD_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "paymentMethodId");
        }
    }

    /** 실패 착지가 쓰는 연·월 읽기. 읽지 못하면 {@link LedgerQuery} 가 이번 달로 정한다. */
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

    private String redraw(Model model, IncomeParams params, String notice) {
        pageModel.putList(model, params.toQuery());
        model.addAttribute("notice", notice);
        return LedgerPageModel.VIEW;
    }

    /**
     * 소득 폼이 보내는 값 묶음. <b>지출유형·장소 자리를 두지 않는다.</b>
     *
     * <p>연·월을 함께 받는 이유는 실패 착지가 같은 달을 다시 그려야 하기 때문이다.
     */
    public static class IncomeParams {

        private Long paymentMethodId;
        private Long amount;
        private String paymentDate;
        private String content;
        private Integer year;
        private Integer month;

        IncomeForm toForm() {
            return new IncomeForm(paymentMethodId, amount, paymentDate, content);
        }

        LedgerQuery toQuery() {
            return LedgerQuery.of(year, month);
        }

        public void setPaymentMethodId(Long paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }
    }
}

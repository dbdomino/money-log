package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.ledger.form.ExpenseForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 지출의 등록(3.1) · 수정(3.2) · 삭제 · 중도상환.
 *
 * <p>소득과 나눈 이유는 <b>폼·검증·실패 착지가 섞이면 어느 코드가 어느 자원의 것인지 읽어서
 * 세야 하기 때문</b>이다. 한 컨트롤러에 담으면 등록·수정·삭제가 여덟 개 처리로 쌓인다.
 *
 * <h2>나가는 통로가 둘이다</h2>
 *
 * <p>등록은 일시불과 할부가 <b>서로 다른 곳으로</b> 나가고 금액 칸 이름도 다르다. 가르는
 * 판단은 {@link ExpenseForm} 이 한다 — 여기서 가르면 등록과 수정 두 곳에 같은 판단이 생긴다.
 *
 * <h2>중도상환은 수정에 얹지 않는다</h2>
 *
 * <p>그 지출 한 건을 고치는 일이 아니라 <b>여러 회차를 함께 바꾸는 일</b>이라, 수정 처리에
 * 얹으면 금액을 고치러 들어갔다가 할부 전체가 정리되는 사고가 난다.
 *
 * <h2>브라우저는 POST 만 쓴다</h2>
 *
 * <p>백엔드가 요구하는 메서드로 옮기는 일은 화면 모듈이 한다. 되돌릴 수 없는 동작을 링크로
 * 두면 브라우저가 미리 불러오는 것만으로 실행된다.
 */
@Controller
public class ExpenseController {

    static final String CREATE_PATH = "/expenses";
    static final String CREATE_INSTALLMENT_PATH = "/expenses/installments";
    static final String ITEM_PATH = "/expenses/{expenseId}";
    static final String SETTLE_PATH = "/expenses/installments/{installmentGroupId}/remainder";

    private final BackendApiClient backendApiClient;
    private final LedgerPageModel pageModel;

    public ExpenseController(BackendApiClient backendApiClient, LedgerPageModel pageModel) {
        this.backendApiClient = backendApiClient;
        this.pageModel = pageModel;
    }

    // ── 등록 · 수정 · 삭제 ──────────────────────────────────────────────

    /**
     * 지출을 등록한다. <b>폼이 가린 통로로 나간다.</b>
     *
     * <p>토글이 접히지 않아 양쪽 값이 다 넘어와도 <b>사용자가 고른 값대로</b> 갈린다 —
     * 접는 것은 편의이고 가르는 것이 보장이다.
     */
    @PostMapping("/ledger/expenses")
    public String create(ExpenseParams params, Model model) {
        ExpenseForm form = params.toForm();

        if (form.isInstallment()) {
            backendApiClient.post(CREATE_INSTALLMENT_PATH, form.toInstallmentCreateRequest(),
                    Void.class);
        } else {
            backendApiClient.post(CREATE_PATH, form.toLumpCreateRequest(), Void.class);
        }

        return redraw(model, params, "지출을 등록했습니다.");
    }

    /**
     * 지출을 고친다. 브라우저는 {@code POST} 로 보내고 이 처리가 백엔드의 수정 요청으로
     * 옮긴다.
     *
     * <p><b>할부 건이어도 개월 수·시작 연월을 싣지 않는다.</b> 화면이 그 칸을 잠그지만
     * 주소로 직접 올 수 있고, 실어 보내면 백엔드가 거절한다.
     */
    @PostMapping("/ledger/expenses/{expenseId}")
    public String update(@PathVariable Long expenseId, ExpenseParams params, Model model) {
        backendApiClient.patch(ITEM_PATH, params.toForm().toUpdateRequest(), Void.class, expenseId);
        return redraw(model, params, "지출을 수정했습니다.");
    }

    /** 지출을 지운다. 확인 다이얼로그를 거쳐 들어온다. */
    @PostMapping("/ledger/expenses/{expenseId}/delete")
    public String delete(@PathVariable Long expenseId, ExpenseParams params, Model model) {
        backendApiClient.delete(ITEM_PATH, expenseId);
        return redraw(model, params, "지출을 삭제했습니다. 이 달 합계에서도 빠집니다.");
    }

    // ── 중도상환 ────────────────────────────────────────────────────────

    /**
     * 할부의 남은 회차를 한 번에 정리한다. <b>본문 없이</b> 부른다.
     *
     * <p>정리할 회차가 없으면 오류 화면이 아니라 <b>목록의 안내</b>로 보인다 — 이미 다 낸
     * 할부이고 사용자가 할 일이 없는데 오류 화면으로 보내면 돌아오는 걸음만 늘어난다.
     */
    @PostMapping("/ledger/expenses/installments/{installmentGroupId}/settle")
    public String settleInstallment(@PathVariable Long installmentGroupId, ExpenseParams params,
            Model model) {

        backendApiClient.patch(SETTLE_PATH, null, Void.class, installmentGroupId);
        return redraw(model, params, "남은 할부를 중도상환 처리했습니다.");
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>모달을 연 채로</b> 목록을 다시 그린다.
     *
     * <p>모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다. 어느 모달이었는지는 <b>요청
     * 주소가 이미 말해 준다</b> — 목록 주소로 온 제출이면 등록, 항목 주소면 수정이다.
     *
     * <p><b>삭제와 중도상환은 모달이 없다.</b> 목록의 안내로만 보인다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyExpenseField(model, exception);
        pageModel.putList(model, LedgerQuery.of(intParam(request, "year"),
                intParam(request, "month")));

        String path = request.getRequestURI();
        if (path.endsWith("/delete") || path.endsWith("/settle")) {
            return LedgerPageModel.VIEW;
        }

        if ("/ledger/expenses".equals(path)) {
            model.addAttribute(ModalParam.MODEL_ATTRIBUTE, LedgerPageModel.MODAL_EXPENSE_CREATE);
            return LedgerPageModel.VIEW;
        }
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, LedgerPageModel.MODAL_EXPENSE_EDIT);
        model.addAttribute("targetId", path.substring(path.lastIndexOf('/') + 1));
        return LedgerPageModel.VIEW;
    }

    /**
     * 지출에만 있는 코드를 칸에 잇는다. 008 의 표에는 회원 코드만 있다.
     *
     * <p><b>수단 실패가 수단 칸인 이유</b>는 그 코드가 가리키는 것이 언제나 수단 하나이기
     * 때문이다. 없는 것인지 용도가 안 맞는 것인지는 가르지 않는다 — 사용자가 할 일은 어느
     * 쪽이든 다른 수단을 고르는 것이다.
     *
     * <p><b>지출 값 오류를 상단에 두는 이유</b>는 한 코드가 금액·날짜·장소·내용 네 칸을 함께
     * 가리키기 때문이다. 짐작해 아무 칸에나 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
     */
    private static void applyExpenseField(Model model, BackendApiException exception) {
        int code = exception.getResCode();
        if (code == ErrorCode.PAYMENT_METHOD_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "paymentMethodId");
        } else if (code == ErrorCode.EXPEND_GROUP_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "expendGroupId");
        } else if (code == ErrorCode.EXPENSE_INSTALLMENT_IMMUTABLE.code()
                || code == ErrorCode.INSTALLMENT_VALIDATION_FAILED.code()) {
            model.addAttribute(FormFailure.FIELD, "installmentMonths");
        }
    }

    /**
     * 실패 착지가 연·월을 요청에서 직접 읽는다.
     *
     * <p>{@code @ExceptionHandler} 는 정해진 인자만 받고 <b>폼 객체를 받지 못한다.</b>
     * 그것을 인자로 두면 처리 메서드가 아예 호출되지 않아 예외가 오류 화면까지 올라가고,
     * 증상은 "모달 제출이 실패하면 목록이 아니라 오류 화면이 뜬다"로만 나타난다.
     *
     * <p>읽지 못하면 {@link LedgerQuery} 가 이번 달로 정한다.
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

    /** 성공한 뒤 목록을 다시 그린다. 연·월은 제출에 실려 온 값을 그대로 쓴다. */
    private String redraw(Model model, ExpenseParams params, String notice) {
        pageModel.putList(model, params.toQuery());
        model.addAttribute("notice", notice);
        return LedgerPageModel.VIEW;
    }

    /**
     * 지출 폼이 보내는 값 묶음.
     *
     * <p>연·월을 함께 받는 이유는 <b>실패 착지가 같은 달을 다시 그려야 하기 때문</b>이다 —
     * 없으면 7월 목록에서 저장에 실패했는데 이번 달 목록이 뜬다.
     */
    public static class ExpenseParams {

        private String payType;
        private Long paymentMethodId;
        private Long expendGroupId;
        private Long amount;
        private String paymentDate;
        private String place;
        private String content;
        private Long monthlyAmount;
        private Integer installmentMonths;
        private String startYearMonth;
        private Integer year;
        private Integer month;

        ExpenseForm toForm() {
            return new ExpenseForm(payType, paymentMethodId, expendGroupId, amount, paymentDate,
                    place, content, monthlyAmount, installmentMonths, startYearMonth);
        }

        LedgerQuery toQuery() {
            return LedgerQuery.of(year, month);
        }

        public void setPayType(String payType) {
            this.payType = payType;
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

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setMonthlyAmount(Long monthlyAmount) {
            this.monthlyAmount = monthlyAmount;
        }

        public void setInstallmentMonths(Integer installmentMonths) {
            this.installmentMonths = installmentMonths;
        }

        public void setStartYearMonth(String startYearMonth) {
            this.startYearMonth = startYearMonth;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public void setMonth(Integer month) {
            this.month = month;
        }
    }
}

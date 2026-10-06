package com.dbdomino.moneylog.front.fixedexpense;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.fixedexpense.form.MonthlyRowForm;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.web.ModalParam;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 4.6 월별 고정지출 내역 모달 — 그 달 목록과 <b>그 달 한 건</b>의 수정.
 *
 * <h2>주소는 설정 목록과 같다</h2>
 *
 * <p>{@code /fixed-expenses?m=monthly} 다. 모달이 부모 목록 위에 얹히는 구조라 그래야 하고,
 * 갈라지는 자리는 <b>매핑 조건 하나</b>({@code params = "m=monthly"})뿐이다.
 * {@code FixedExpenseController} 쪽이 {@code m!=monthly} 로 비켜 준다.
 *
 * <h2>설정과 다른 저장 단위다</h2>
 *
 * <p>{@code FixedExpenseController} 의 수정은 <b>미래 달로 번지고</b> 여기의 수정은 <b>그
 * 달에서 끝난다.</b> 한 파일에 두면 두 개의 「수정」이 나란히 놓여 읽는 사람이 매번 어느
 * 쪽인지 세야 한다.
 *
 * <h2>조회가 쓰기를 겸한다</h2>
 *
 * <p>그 연·월의 행이 없으면 <b>백엔드가 만들어 저장한 뒤</b> 목록에 넣는다. {@code GET} 인데
 * 부작용이 있는 유일한 호출이다.
 *
 * <p><b>화면은 그 달을 열 뿐이고 만들라고 따로 요청하지 않는다</b> — 빈 목록을 받아
 * 「만들까요?」를 묻지 않는다. 만드는 규칙(적용 기간·말일 보정)이 백엔드에 있고, 화면이 따로
 * 요청하면 같은 판단이 두 곳에 생긴다.
 *
 * <h2>모달을 겹치지 않는다</h2>
 *
 * <p>단건 수정은 <b>같은 모달을 편집 상태로</b> 다시 연다. 007 의 모달 스크립트는 겹친
 * 모달을 가정하지 않는다 — Esc 가 전부 닫는 구조라, 겹치면 위의 것만 닫으려 해도 둘 다
 * 닫히고 <b>사용자가 고치던 값이 사라진다.</b>
 */
@Controller
public class FixedExpenseMonthlyController {

    static final String MONTHLY_PATH = "/fixed-expenses/monthly";
    static final String MONTHLY_ITEM_PATH =
            "/fixed-expenses/monthly/{year}/{month}/{fixedExpenseId}";

    /** 반영. 연·월을 <b>본문</b>으로 받는다 — POST 는 주소에 값을 싣지 않는다. */
    static final String SYNC_PATH = "/fixed-expenses/monthly/sync";

    /** 모달 안에서 고치는 중인 행. 있으면 편집 상태로 그린다. */
    static final String EDITING_ROW = "editingRow";

    /** 고치려는 행의 식별자. 조회가 실패해도 편집 상태를 잃지 않게 따로 담는다. */
    static final String EDITING_ROW_ID = "editingRowId";

    private final FixedExpensePageModel pageModel;
    private final BackendApiClient backendApiClient;

    public FixedExpenseMonthlyController(FixedExpensePageModel pageModel,
            BackendApiClient backendApiClient) {
        this.pageModel = pageModel;
        this.backendApiClient = backendApiClient;
    }

    // ── 그 달 목록 ──────────────────────────────────────────────────────

    /**
     * 월별 내역 모달을 연 채로 부모 목록을 그린다.
     *
     * <p><b>대상 식별자가 붙지 않는 것이 기본이다</b> — 그 달 전체를 보는 화면이고,
     * {@code id} 는 <b>단건 수정에 들어갈 때만</b> 붙는다.
     *
     * @param page 부모 목록의 쪽 번호. 모달 뒤의 목록이 첫 쪽으로 돌아가지 않게 함께 나른다
     * @param year 주소의 연도. 없으면 <b>서버 시각의 이번 달</b>
     * @param month 주소의 월
     * @param targetId 고칠 행. 있으면 같은 모달이 편집 상태로 열린다
     */
    @GetMapping(value = "/fixed-expenses", params = "m=monthly")
    public String monthly(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = MonthlyQuery.PARAM_YEAR, required = false) Integer year,
            @RequestParam(name = MonthlyQuery.PARAM_MONTH, required = false) Integer month,
            @RequestParam(name = "id", required = false) Long targetId,
            Model model) {

        pageModel.putList(model, FixedExpensePageModel.pagingOf(page));
        putMonthly(model, MonthlyQuery.of(year, month), targetId);
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_MONTHLY);
        return FixedExpensePageModel.VIEW;
    }

    /**
     * 그 달 내역을 조회해 모델에 담고, 고칠 행이 지정됐으면 그 행을 함께 싣는다.
     *
     * <p><b>연·월 값이 틀려도 모달은 열린다</b> — 빈 목록과 합계 0 으로 그리고 안내만 띄운다.
     * 여기서 오류 화면으로 보내면 사용자는 고정지출 화면 전체가 죽었다고 읽는다.
     *
     * <p><b>고칠 행을 목록에서 찾는다.</b> 단건 조회 API 를 따로 부르지 않는 이유는 그 행이
     * 방금 받은 목록 안에 이미 있기 때문이다 — 한 화면에서 같은 값을 두 번 받게 된다.
     */
    private void putMonthly(Model model, MonthlyQuery query, Long targetId) {
        applyMonthly(model, query, fetchMonthly(model, query), targetId);
    }

    /** 그 달 내역을 조회한다. 연·월 값 오류는 빈 목록과 안내로 눕힌다. */
    private MonthlyResult fetchMonthly(Model model, MonthlyQuery query) {
        try {
            MonthlyResult result = backendApiClient.getByQuery(
                    MONTHLY_PATH, query.toBackendQuery(), MonthlyResult.class);
            return result == null ? MonthlyResult.empty(query.year(), query.month()) : result;
        } catch (BackendApiException e) {
            if (e.getResCode() != ErrorCode.FIXED_EXPENSE_MONTH_INVALID.code()) {
                throw e;
            }
            model.addAttribute("notice", e.getMessage());
            return MonthlyResult.empty(query.year(), query.month());
        }
    }

    /**
     * 받은 그 달 내역을 모델에 담는다.
     *
     * <p><b>조회한 것과 반영이 돌려준 것이 같은 자리에 담긴다</b> — 목록을 그리는 코드가
     * 하나여야 반영 뒤의 표와 조회 뒤의 표가 달라 보일 일이 없다.
     */
    private static void applyMonthly(Model model, MonthlyQuery query, MonthlyResult result,
            Long targetId) {

        model.addAttribute("monthly", result);
        model.addAttribute("monthlyQuery", query);
        model.addAttribute("monthlyPrev", query.previousMonth());
        model.addAttribute("monthlyNext", query.nextMonth());

        if (targetId == null) {
            return;
        }
        model.addAttribute(EDITING_ROW_ID, targetId);
        result.rows().stream()
                .filter(row -> targetId.equals(row.fixedExpenseId()))
                .findFirst()
                .ifPresent(row -> model.addAttribute(EDITING_ROW, row));
    }

    // ── 그 달 한 건 수정 ────────────────────────────────────────────────

    /**
     * 그 달 한 건을 고친다. <b>금액·결제일·내용·수단만</b> 나간다.
     *
     * <p>이름과 지출유형은 이 통로로 바꾸지 않는다 — 4.5 에서 바꾼다.
     *
     * <p>성공하면 <b>그 달을 다시 조회해</b> 그린다. 고친 행에는 백엔드가 직접 수정 표시를
     * 켜므로 목록에서 「수정됨」으로 구분된다. <b>다른 달은 바뀌지 않는다.</b>
     */
    @PostMapping(MONTHLY_ITEM_PATH)
    public String updateRow(@PathVariable int year, @PathVariable int month,
            @PathVariable Long fixedExpenseId, MonthlyRowParams params, Model model) {

        backendApiClient.patch(MONTHLY_ITEM_PATH, params.toForm().toRequest(), Void.class,
                year, month, fixedExpenseId);

        pageModel.putList(model, FixedExpensePageModel.pagingOf(params.page));
        putMonthly(model, new MonthlyQuery(year, month), null);
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_MONTHLY);
        model.addAttribute("notice", "그 달 내역을 수정했습니다. 다른 달은 그대로입니다.");
        return FixedExpensePageModel.VIEW;
    }

    // ── 반영 ────────────────────────────────────────────────────────────

    /**
     * 그 달 내역을 <b>설정 기준으로 다시 만든다.</b>
     *
     * <p>US2 가 만든 「지난 달은 안 따라온다」의 <b>유일한 해결책</b>이다. 적용 기간에
     * 걸리는데 없는 것은 추가하고, 직접 고치지 않은 것은 갱신하고, <b>직접 고친 것은
     * 보존하고</b>, 기간이 더는 그 달을 포함하지 않는 것은 지운다.
     *
     * <p><b>응답의 목록과 합계로 갱신하고 다시 조회하지 않는다.</b> 다시 부르면 그사이 바뀐
     * 값이 섞여 「방금 반영한 결과」가 아닌 것을 보게 된다.
     *
     * <p>실패는 <b>목록의 안내</b>로 보인다 — 반영은 모달이 없는 자리다.
     *
     * @param overwriteModified 직접 수정분까지 되돌릴지. <b>기본 꺼짐</b>이며 켠 쪽은 확인
     *        문구가 다른 다이얼로그를 거쳐 들어온다
     */
    @PostMapping(SYNC_PATH)
    public String sync(
            @RequestParam(name = MonthlyQuery.PARAM_YEAR, required = false) Integer year,
            @RequestParam(name = MonthlyQuery.PARAM_MONTH, required = false) Integer month,
            @RequestParam(name = "overwriteModified", defaultValue = "false")
                    boolean overwriteModified,
            @RequestParam(name = "page", required = false) Integer page,
            Model model) {

        MonthlyQuery query = MonthlyQuery.of(year, month);
        SyncResult result = backendApiClient.post(
                SYNC_PATH, syncBody(query, overwriteModified), SyncResult.class);

        pageModel.putList(model, FixedExpensePageModel.pagingOf(page));
        applyMonthly(model, query, result.toMonthly(), null);
        model.addAttribute("syncResult", result);
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_MONTHLY);
        return FixedExpensePageModel.VIEW;
    }

    /** 반영 요청 본문. 연·월은 POST 규칙에 따라 <b>주소가 아니라 본문</b>으로 간다. */
    private static Map<String, Object> syncBody(MonthlyQuery query, boolean overwriteModified) {
        Map<String, Object> body = new LinkedHashMap<>(query.toBackendQuery());
        body.put("overwriteModified", overwriteModified);
        return body;
    }

    // ── 실패 착지 ───────────────────────────────────────────────────────

    /**
     * 실패해도 오류 화면으로 보내지 않는다. <b>그 달과 그 대상이 유지된 채</b> 모달이 다시
     * 열린다.
     *
     * <p>모달을 닫아 버리면 사용자가 고치던 값이 사라지고, 연·월까지 잃으면 어느 달을 보고
     * 있었는지부터 다시 찾아야 한다.
     */
    @ExceptionHandler(BackendApiException.class)
    public String handleFailure(BackendApiException exception, HttpServletRequest request,
            Model model) {

        FormFailure.applyTo(model, exception);
        applyMonthlyField(model, exception);

        MonthlyQuery query = MonthlyQuery.of(
                intParam(request, MonthlyQuery.PARAM_YEAR),
                intParam(request, MonthlyQuery.PARAM_MONTH));

        pageModel.putList(model, FixedExpensePageModel.pagingOf(intParam(request, "page")));
        putMonthly(model, query, longParam(request, "id"));
        model.addAttribute(ModalParam.MODEL_ATTRIBUTE, FixedExpensePageModel.MODAL_MONTHLY);

        if (exception.getResCode() == ErrorCode.FIXED_EXPENSE_MONTHLY_NOT_CREATED.code()) {
            // 그 달을 여는 것이 곧 만드는 일이다. 방금 다시 그리면서 이미 열었으므로
            // 사용자가 할 일은 한 번 더 저장하는 것뿐이다.
            model.addAttribute("notice",
                    "그 달 내역이 아직 없었습니다. 먼저 그 달을 열어 내역을 만들었으니 다시 저장해 주세요.");
        }
        return FixedExpensePageModel.VIEW;
    }

    /**
     * 월별 내역에만 있는 코드를 칸에 잇는다.
     *
     * <p><b>{@code 3401} 은 여기서도 상단이다.</b> 금액·결제일 오류와 <b>수단 용도
     * 불일치</b>를 한 코드가 겸해 코드만으로는 어느 칸인지 가릴 수 없다 — 짐작해 붙이면
     * 사용자가 맞는 칸을 고치고 또 틀린다. 그 자리에서는 <b>서버 문구를 그대로</b> 보인다.
     */
    private static void applyMonthlyField(Model model, BackendApiException exception) {
        if (exception.getResCode() == ErrorCode.PAYMENT_METHOD_NOT_FOUND.code()) {
            model.addAttribute(FormFailure.FIELD, "paymentMethodId");
        }
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    /**
     * 실패 착지가 연·월·대상을 요청에서 직접 읽는다.
     *
     * <p>{@code @ExceptionHandler} 는 정해진 인자만 받고 <b>폼 객체를 받지 못한다.</b>
     * 그것을 인자로 두면 처리 메서드가 아예 호출되지 않아 예외가 오류 화면까지 올라간다.
     * 증상은 「모달 제출이 실패하면 목록이 아니라 오류 화면이 뜬다」로만 나타난다.
     */
    private static Integer intParam(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long longParam(HttpServletRequest request, String name) {
        Integer value = intParam(request, name);
        return value == null ? null : value.longValue();
    }

    /**
     * 그 달 한 건 폼이 보내는 값 묶음.
     *
     * <p>연·월·쪽 번호를 함께 받는 이유는 <b>실패 착지가 같은 달과 같은 쪽으로 돌아가야
     * 하기 때문</b>이다. 주소에도 실려 있지만 처리 메서드가 폼을 받지 못하므로, 폼이 나르는
     * 값과 요청에서 읽는 값의 이름을 같게 둔다.
     */
    public static class MonthlyRowParams {

        private Long amount;
        private String paymentDate;
        private String content;
        private Long paymentMethodId;
        private Integer page;

        MonthlyRowForm toForm() {
            return new MonthlyRowForm(amount, paymentDate, content, paymentMethodId);
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

        public void setPaymentMethodId(Long paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }

        public void setPage(Integer page) {
            this.page = page;
        }
    }
}

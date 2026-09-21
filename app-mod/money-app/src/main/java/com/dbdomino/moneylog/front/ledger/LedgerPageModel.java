package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupResponse;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 가계부 화면의 목록·합계·필터·모달 값을 <b>모델에 담는 한 자리</b>.
 *
 * <p>컨트롤러 넷이 함께 쓴다 — 목록·지출·소득·엑셀. 나누어도 <b>실패 착지는 목록으로
 * 모이기 때문</b>이다: 지출 모달에서 실패해도, 소득 모달에서 실패해도, 다시 그려야 하는
 * 것은 같은 목록이다.
 *
 * <p>이것을 복사하면 <b>한쪽만 고친 차이가 화면에 남는다</b> — 지출 모달에서 실패했을 때와
 * 소득 모달에서 실패했을 때 목록이 달라 보이는 식이다. 증상만 보고는 복사가 원인이라는 것을
 * 짐작하기 어렵다.
 *
 * <h2>목록 조회가 실패해도 그린다</h2>
 *
 * <p>로그인 직후 착지가 이 화면이다. 여기서 터지면 <b>사용자는 로그인하자마자 오류를
 * 만난다.</b> 응답이 비어 와도 빈 목록과 합계 0 으로 그린다.
 *
 * <p><b>백엔드에 닿지 못한 것은 다르다.</b> 그것은 007 의 오류 화면으로 가며, 이 클래스가
 * 잡지 않고 그대로 올려 보낸다 — "거래가 없는 달"과 "불러오지 못한 달"이 똑같이 보이면
 * 사용자는 자기 데이터가 없어졌다고 읽는다.
 */
@Component
public class LedgerPageModel {

    /** 목록·모달 넷이 모두 이 뷰 하나로 모인다. */
    public static final String VIEW = "ledger/list";

    static final String LEDGER_PATH = "/ledger/monthly";
    static final String ACTIVE_PAYMENT_PATH = "/payment-methods/active/{purpose}";
    static final String ACTIVE_EXPEND_GROUP_PATH = "/expend-groups/active";

    /** 수단의 용도. 지출 모달과 소득 모달이 서로 다른 쪽을 부른다. */
    public static final String PURPOSE_EXPENSE = "EXPENSE";
    public static final String PURPOSE_INCOME = "INCOME";

    /**
     * 이 화면이 여는 모달 넷. <b>009 와 달리 자원별로 넷이다</b> — 한 부모에 두 자원이
     * 얹히기 때문이다.
     */
    public static final String MODAL_EXPENSE_CREATE = "expense-create";
    public static final String MODAL_EXPENSE_EDIT = "expense-edit";
    public static final String MODAL_INCOME_CREATE = "income-create";
    public static final String MODAL_INCOME_EDIT = "income-edit";

    /** 모달 값과 화면 안의 식별자를 잇는 지도. 007 의 딥링크 스크립트가 이것을 읽는다. */
    static final String MODAL_MAP = "{\"expense-create\":\"modal-expense-create\","
            + "\"expense-edit\":\"modal-expense-edit\","
            + "\"income-create\":\"modal-income-create\","
            + "\"income-edit\":\"modal-income-edit\"}";

    private final BackendApiClient backendApiClient;

    public LedgerPageModel(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    /**
     * 그 달의 목록과 합계를 조회해 모델에 담는다.
     *
     * <p><b>조회 구간을 싣지 않는다</b> — 한 달치를 전부 받고 합계가 함께 온다.
     *
     * <p><b>합계는 백엔드가 준 값 그대로 간다.</b> 화면이 목록을 다시 더하지 않는다 — 더해
     * 맞추면 "필터를 걸었는데 이번 달 지출이 줄어드는" 화면이 된다.
     */
    public void putList(Model model, LedgerQuery query) {
        LedgerMonth month = backendApiClient.getByQuery(
                LEDGER_PATH, query.toBackendQuery(), LedgerMonth.class);
        if (month == null) {
            month = LedgerMonth.empty(query.year(), query.month());
        }

        model.addAttribute("ledger", month);
        model.addAttribute("rows", month.rows());
        model.addAttribute("query", query);
        model.addAttribute("activeMenu", "ledger");
        model.addAttribute("modalMap", MODAL_MAP);

        putChoices(model);
    }

    /**
     * 선택 목록 셋을 <b>한 번만</b> 받아 도구줄과 모달 넷이 함께 쓰게 한다.
     *
     * <p>도구줄과 모달이 각자 부르면 <b>같은 목록을 한 화면에서 두 번 받는다.</b> 모달을
     * 연 채 목록을 그리는 것이 이 화면의 기본 동작이라 그 중복이 늘 일어난다.
     *
     * <p><b>009 의 관리 목록을 쓰지 않는다.</b> 거기에는 삭제 표시된 것과 사용 안 함이 함께
     * 들어 있어, 쓰면 사용자가 <b>지운 수단으로 지출을 등록</b>하거나 <b>고르면 언제나
     * 0건인 조건</b>을 걸게 된다.
     *
     * <p>수단을 용도별로 따로 담는 이유는 <b>모달마다 가리키는 쪽이 다르기 때문</b>이다 —
     * 지출 모달에 소득 수단이 뜨면 고른 순간 백엔드가 용도 불일치로 거절하고, 사용자에게는
     * 자기가 고른 것이 거절된 것으로 보인다. <b>도구줄은 반대로 둘을 합쳐 쓴다</b>: 목록에
     * 네 종류가 섞여 있어 한쪽만 놓으면 소득 행을 수단으로 거를 방법이 없다.
     *
     * <p><b>소득 모달은 지출유형 목록을 쓰지 않는다.</b> 그 칸이 없기 때문이며, 목록을
     * 받아 오는 것은 도구줄의 지출유형 필터 때문이다.
     */
    public void putChoices(Model model) {
        List<PaymentMethodView> expenseMethods = activePaymentMethods(PURPOSE_EXPENSE);
        List<PaymentMethodView> incomeMethods = activePaymentMethods(PURPOSE_INCOME);
        List<ExpendGroupResponse> groups = activeExpendGroups();

        model.addAttribute("expensePaymentMethods", expenseMethods);
        model.addAttribute("incomePaymentMethods", incomeMethods);
        model.addAttribute("expendGroups", groups);

        List<PaymentMethodView> both = new ArrayList<>(expenseMethods);
        both.addAll(incomeMethods);
        model.addAttribute("filterPaymentMethods", both);
        model.addAttribute("filterExpendGroups", groups);
    }

    /** 사용 중 수단 목록. 받지 못해도 빈 목록으로 둔다 — 그 칸만 비고 모달은 열린다. */
    public List<PaymentMethodView> activePaymentMethods(String purpose) {
        PaymentMethodListResult result = backendApiClient.get(
                ACTIVE_PAYMENT_PATH, PaymentMethodListResult.class, purpose);
        return result == null ? List.of() : result.rows();
    }

    /** 사용 중 지출유형 목록. */
    public List<ExpendGroupResponse> activeExpendGroups() {
        ExpendGroupListResult result = backendApiClient.getByQuery(
                ACTIVE_EXPEND_GROUP_PATH, Map.of(), ExpendGroupListResult.class);
        return result == null ? List.of() : result.rows();
    }
}

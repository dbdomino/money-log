package com.dbdomino.moneylog.front.fixedexpense;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupResponse;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import com.dbdomino.moneylog.front.web.Paging;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 고정지출 화면의 목록·쪽 정보·선택 목록·모달 값을 <b>모델에 담는 한 자리</b>.
 *
 * <p>컨트롤러 둘이 함께 쓴다 — 설정({@code FixedExpenseController})과 그 달 내역
 * ({@code FixedExpenseMonthlyController}). 나누어도 <b>실패 착지는 목록으로 모이기
 * 때문</b>이다: 등록에서 실패해도, 그 달 한 건을 고치다 실패해도, 다시 그려야 하는 것은
 * 같은 설정 목록이다.
 *
 * <p>이것을 복사하면 <b>한쪽만 고친 차이가 화면에 남는다</b> — 010 에서 그 차이를 확인했고,
 * 증상만 보고는 복사가 원인이라는 것을 짐작하기 어렵다.
 *
 * <h2>쪽 넘기기가 있다</h2>
 *
 * <p>009·010 과 다르다. 백엔드가 시작점과 개수를 <b>필수로</b> 받으며 시작점이 개수의
 * 배수가 아니면 목록이 통째로 실패한다 — 007 의 환산기가 <b>쪽 번호에서만</b> 시작점을
 * 만들므로 어긋난 값이 생길 경로가 없다.
 *
 * <h2>선택 목록은 한 번만 받는다</h2>
 *
 * <p>모달 넷이 같은 목록을 쓰므로 각자 부르면 <b>같은 목록을 한 화면에서 두 번 받는다</b> —
 * 모달을 연 채 목록을 그리는 것이 이 화면의 기본 동작이라 그 중복이 늘 일어난다. 010 이
 * 같은 정리를 했다.
 *
 * <p><b>009 의 관리 목록을 쓰지 않는다.</b> 거기에는 삭제 표시된 것과 사용 안 함이 함께
 * 들어 있어, 쓰면 사용자가 <b>지운 수단으로 고정지출을 만들게</b> 된다.
 *
 * <p>수단은 <b>지출용</b>만 부른다 — 고정지출에 소득 수단을 걸 일이 없고, 소득용이 섞이면
 * 고른 순간 백엔드가 용도 불일치로 거절한다.
 */
@Component
public class FixedExpensePageModel {

    /** 목록·모달 넷이 모두 이 뷰 하나로 모인다. */
    public static final String VIEW = "fixed-expenses/list";

    static final String LIST_PATH = "/fixed-expenses";
    static final String ITEM_PATH = "/fixed-expenses/{fixedExpenseId}";
    static final String ACTIVE_PAYMENT_PATH = "/payment-methods/active/{purpose}";
    static final String ACTIVE_EXPEND_GROUP_PATH = "/expend-groups/active";

    /** 고정지출이 쓰는 수단의 용도. 소득용은 부르지 않는다. */
    public static final String PURPOSE_EXPENSE = "EXPENSE";

    /**
     * 이 화면이 여는 모달 넷.
     *
     * <p><b>{@code MONTHLY} 만 성격이 다르다</b> — 나머지 셋은 설정 하나를 다루는데 그것은
     * <b>그 달 전체</b>를 본다. 그래서 대상 식별자를 싣지 않는다.
     */
    public static final String MODAL_CREATE = "create";
    public static final String MODAL_DETAIL = "detail";
    public static final String MODAL_EDIT = "edit";
    public static final String MODAL_MONTHLY = "monthly";

    /** 모달 값과 화면 안의 식별자를 잇는 지도. 007 의 딥링크 스크립트가 이것을 읽는다. */
    static final String MODAL_MAP = "{\"create\":\"modal-fixed-create\","
            + "\"detail\":\"modal-fixed-detail\",\"edit\":\"modal-fixed-edit\","
            + "\"monthly\":\"modal-fixed-monthly\"}";

    private final BackendApiClient backendApiClient;

    public FixedExpensePageModel(BackendApiClient backendApiClient) {
        this.backendApiClient = backendApiClient;
    }

    /**
     * 쪽 번호를 007 의 환산기로 바꾼다. 음수는 첫 쪽으로 본다.
     *
     * <p>컨트롤러 둘이 함께 쓴다 — 월별 내역 모달도 <b>부모 목록을 함께 그리기 때문</b>이다.
     * 각자 두면 한쪽만 고친 차이가 남고, 증상은 「모달을 열면 목록이 첫 쪽으로 돌아간다」로만
     * 보인다.
     *
     * <p>주소를 오타로 친 것을 오류로 막지 않는다 — 007 이 모달 딥링크를 그렇게 정했다.
     */
    public static Paging pagingOf(Integer page) {
        return page == null || page < 0 ? Paging.first() : Paging.of(page, Paging.DEFAULT_LIMIT);
    }

    /**
     * 설정 목록을 조회해 모델에 담는다.
     *
     * <p>응답이 비어 와도 <b>빈 목록으로 그린다</b> — 여기서 터지면 사용자는 고정지출 관리가
     * 통째로 죽었다고 읽는다. <b>백엔드에 닿지 못한 것은 다르다</b>: 그것은 007 의 오류
     * 화면으로 가며 이 클래스가 잡지 않는다.
     */
    public void putList(Model model, Paging paging) {
        FixedExpenseListResult result = backendApiClient.getByQuery(
                LIST_PATH,
                Map.of("offset", paging.offset(), "limit", paging.limit()),
                FixedExpenseListResult.class);

        if (result == null) {
            result = FixedExpenseListResult.empty(paging.offset(), paging.limit());
        }

        model.addAttribute("fixedExpenses", result.rows());
        model.addAttribute("paging", paging);
        model.addAttribute("totalCount", result.total());
        model.addAttribute("totalPages", Paging.totalPages(result.total(), result.pageSize()));
        model.addAttribute("activeMenu", "fixed-list");
        model.addAttribute("modalMap", MODAL_MAP);

        putChoices(model);
    }

    /**
     * 선택 목록 둘을 담는다. 모달 넷이 함께 쓴다.
     *
     * <p><b>사용 중 목록만 쓴다.</b> 009 의 관리 목록에는 삭제 표시된 것과 사용 안 함이
     * 함께 들어 있어, 쓰면 사용자가 지운 수단으로 고정지출을 만들게 된다.
     *
     * <p>받지 못해도 <b>빈 목록으로</b> 둔다 — 그 칸만 비고 모달은 열린다.
     */
    public void putChoices(Model model) {
        model.addAttribute("paymentMethods", activePaymentMethods());
        model.addAttribute("expendGroups", activeExpendGroups());
    }

    /** 사용 중 수단 목록. <b>지출용</b>만 부른다. */
    public List<PaymentMethodView> activePaymentMethods() {
        PaymentMethodListResult result = backendApiClient.get(
                ACTIVE_PAYMENT_PATH, PaymentMethodListResult.class, PURPOSE_EXPENSE);
        return result == null ? List.of() : result.rows();
    }

    /** 사용 중 지출유형 목록. */
    public List<ExpendGroupResponse> activeExpendGroups() {
        ExpendGroupListResult result = backendApiClient.getByQuery(
                ACTIVE_EXPEND_GROUP_PATH, Map.of(), ExpendGroupListResult.class);
        return result == null ? List.of() : result.rows();
    }
}

package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.web.ModalParam;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 5.1 월별 가계부의 목록·합계·필터.
 *
 * <p><b>로그인 뒤 착지가 이 화면이다.</b> 008 이 로그인 성공 시 이 주소로 보내며, 010 까지
 * 서야 그 자리가 채워진다.
 *
 * <h2>컨트롤러가 넷이다</h2>
 *
 * <p>009 는 자원 하나에 목록과 모달 셋이라 한 컨트롤러가 맞았다. 010 은 <b>한 부모에 자원
 * 둘이 얹히고</b> 엑셀이 독립 페이지다 — 한 컨트롤러에 담으면 지출과 소득의 등록·수정·삭제가
 * 한 파일에 여덟 개 처리로 쌓인다.
 *
 * <p><b>나누어도 실패 착지는 목록으로 모인다.</b> 넷이 같은 뷰 이름을 돌려주고, 목록을 다시
 * 그리는 일은 {@link LedgerPageModel} 한 곳에 모아 함께 쓴다.
 *
 * <h2>쪽 넘기기가 없다</h2>
 *
 * <p>백엔드 목록이 조회 구간을 받지 않고 한 달치를 전부 돌려주며 합계도 함께 온다. 007 의
 * 페이징 환산기를 쓰지 않는다 — 쪽을 나누면 합계와 목록의 기준이 갈린다.
 */
@Controller
public class LedgerController {

    static final String EXPENSE_ITEM_PATH = "/expenses/{expenseId}";
    static final String INCOME_ITEM_PATH = "/incomes/{incomeId}";

    /**
     * 이 화면이 여는 모달. <b>009 와 달리 넷이다</b> — 한 부모에 자원 둘이 얹힌다.
     *
     * <p>상세 모달이 없다. 목록에 값이 다 보여 따로 읽을 자리가 필요 없다.
     */
    private static final Set<String> MODALS = Set.of(
            LedgerPageModel.MODAL_EXPENSE_CREATE, LedgerPageModel.MODAL_EXPENSE_EDIT,
            LedgerPageModel.MODAL_INCOME_CREATE, LedgerPageModel.MODAL_INCOME_EDIT);

    private final LedgerPageModel pageModel;
    private final BackendApiClient backendApiClient;

    public LedgerController(LedgerPageModel pageModel, BackendApiClient backendApiClient) {
        this.pageModel = pageModel;
        this.backendApiClient = backendApiClient;
    }

    /**
     * 그 달의 가계부를 그리고, 요청에 모달이 실려 있으면 함께 연다.
     *
     * <p>연·월이 없으면 <b>서버 시각의 이번 달</b>이다 — 브라우저 시각을 믿으면 시차가 있는
     * 사용자가 다른 달을 본다.
     */
    @GetMapping("/ledger")
    public String list(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "paymentMethodId", required = false) Long paymentMethodId,
            @RequestParam(name = "expendGroupId", required = false) Long expendGroupId,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "type", required = false) List<String> types,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "m", required = false) String modal,
            @RequestParam(name = "id", required = false) Long targetId,
            Model model) {

        // 비어 있는 조건을 싣지 않는 일은 LedgerQuery 가 한다 — 빈 값을 보내면 백엔드가
        // "그 값으로 걸러 달라"로 읽어 결과가 통째로 빈다.
        pageModel.putList(model, LedgerQuery.of(year, month, paymentMethodId, expendGroupId,
                dateFrom, dateTo, keyword, types, sort, order));

        // 선택 목록 셋은 putList 가 함께 담는다. 도구줄의 필터와 모달 넷이 같은 목록을
        // 쓰므로 여기서 또 부르면 한 화면에서 같은 것을 두 번 받는다.
        resolveModal(model, modal, targetId)
                .ifPresent(value -> model.addAttribute(ModalParam.MODEL_ATTRIBUTE, value));
        return LedgerPageModel.VIEW;
    }

    /**
     * 어떤 모달을 열지 정하고, 수정이면 <b>목록을 그리기 전에</b> 대상을 조회해 싣는다.
     *
     * <p>여는 일만 브라우저가 하면 열린 모달이 빈 채로 뜨고 값을 채우려면 다시 요청해야
     * 한다. 모달을 열지 않기로 한 경우 <b>키 자체를 담지 않는다</b> — 007 이 모달 판정을
     * 그렇게 정했다.
     *
     * <p><b>없는 것과 남의 것을 가르지 않는다.</b> 백엔드가 한 코드로 묶었고 화면도 풀지
     * 않는다 — 가르면 식별자를 훑어 남의 지출이 실재하는지 알아낼 수 있다.
     */
    private Optional<String> resolveModal(Model model, String modal, Long targetId) {
        Optional<String> resolved = ModalParam.resolve(modal, MODALS);
        if (resolved.isEmpty()) {
            return resolved;
        }

        String value = resolved.get();
        boolean expenseSide = value.startsWith("expense");

        // 등록 모달은 조회할 대상이 없다. 선택 목록은 putList 가 이미 담아 두었다.
        if (value.endsWith("create")) {
            return resolved;
        }

        // 무엇을 조회할지 알 수 없으면 열지 않는다. 빈 모달을 띄우지 않는다.
        if (targetId == null) {
            return Optional.empty();
        }

        try {
            if (expenseSide) {
                model.addAttribute("targetExpense",
                        backendApiClient.get(EXPENSE_ITEM_PATH, ExpenseView.class, targetId));
            } else {
                model.addAttribute("targetIncome",
                        backendApiClient.get(INCOME_ITEM_PATH, IncomeView.class, targetId));
            }
            return resolved;
        } catch (BackendApiException e) {
            if (!isNotFound(e, expenseSide)) {
                throw e;
            }
            // 빈 모달이 아니라 목록만 남긴다. 사용자는 하려던 일을 이어서 할 수 있다.
            model.addAttribute("notice", e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isNotFound(BackendApiException exception, boolean expenseSide) {
        int expected = expenseSide
                ? ErrorCode.EXPENSE_NOT_FOUND.code()
                : ErrorCode.INCOME_NOT_FOUND.code();
        return exception.getResCode() == expected;
    }

}

package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 5.1 상단 요약 — <b>합계는 필터와 무관하다.</b>
 *
 * <p>이 화면의 핵심이 여기 있다(FR-904 · SC-903). 화면이 목록을 다시 더해 맞추려 드는 것이
 * 자연스러운 실수인데, 맞추면 <b>"필터를 걸었는데 이번 달 지출이 줄어드는" 화면</b>이 되고
 * 사용자는 자기가 쓴 돈이 줄었다고 읽는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LedgerSummaryTest {

    private static final String LIST_URL = "/ledger";

    /** 자료의 합계. 목록의 합과 <b>일부러 다르게</b> 두었다. */
    private static final long EXPENSE_TOTAL = 659_500L;
    private static final long INCOME_TOTAL = 3_500_000L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        LedgerTestSupport.stubCommon(backendApiClient);
    }

    @Test
    @DisplayName("상단 합계가 백엔드 값 그대로이고 화면이 목록을 다시 더하지 않는다 (FR-903·FR-904)")
    void 합계는_백엔드_값_그대로다() throws Exception {
        LedgerTestSupport.stubDefaultMonth(backendApiClient);
        LedgerMonth model = ledgerModel();

        // 거르지 않은 달이라 목록의 합과 우연히 같다. 같다는 것으로는 "다시 더하지
        // 않았다"를 보일 수 없어, 그 보장은 아래 필터 시험이 진다.
        assertThat(model.expenseSum()).isEqualTo(EXPENSE_TOTAL);
        assertThat(model.incomeSum()).isEqualTo(INCOME_TOTAL);
    }

    @Test
    @DisplayName("잔액이 두 합계의 차다")
    void 잔액은_두_합계의_차다() throws Exception {
        LedgerTestSupport.stubDefaultMonth(backendApiClient);

        // 목록을 더해 만들면 필터를 걸 때마다 잔액이 흔들린다.
        assertThat(ledgerModel().balance()).isEqualTo(INCOME_TOTAL - EXPENSE_TOTAL);
    }

    @Test
    @DisplayName("필터를 걸어도 합계가 그대로다 — 목록만 좁혀진다 (SC-903)")
    void 필터를_걸어도_합계가_그대로다() throws Exception {
        LedgerTestSupport.stubMonth(backendApiClient, LedgerFixture.filteredMonth());

        LedgerMonth model = ledgerModel("keyword", "김치");

        // 목록은 한 건으로 줄었는데 합계는 그 달 전체 기준으로 남아 있어야 한다.
        assertThat(model.rows()).hasSize(1);
        assertThat(model.expenseSum()).isEqualTo(EXPENSE_TOTAL);
        assertThat(model.incomeSum()).isEqualTo(INCOME_TOTAL);
        assertThat(model.balance()).isEqualTo(INCOME_TOTAL - EXPENSE_TOTAL);

        // 여기가 "다시 더하지 않는다"를 보이는 자리다. 맞추려 들었다면 지출 합계가 남은
        // 한 건(9,500)이 되고, 소득 합계는 0 이 된다 — 사용자는 자기가 쓴 돈이 줄었다고
        // 읽는다.
        long listExpenseSum = model.rows().stream()
                .filter(row -> !row.isIncome())
                .mapToLong(LedgerRow::amount)
                .sum();
        assertThat(model.expenseSum()).isNotEqualTo(listExpenseSum);
        assertThat(model.incomeSum()).isNotZero();
    }

    @Test
    @DisplayName("거래가 없는 달은 합계 0 과 빈 목록 안내가 함께 보인다 (FR-907)")
    void 거래가_없는_달() throws Exception {
        LedgerTestSupport.stubMonth(backendApiClient, LedgerFixture.emptyMonth());

        MvcResult result = mockMvc.perform(get(LIST_URL)
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn();

        LedgerMonth model = (LedgerMonth) result.getModelAndView().getModel().get("ledger");
        assertThat(model.expenseSum()).isZero();
        assertThat(model.incomeSum()).isZero();
        assertThat(model.isEmpty()).isTrue();

        // 목록만 비어 있으면 사용자는 불러오지 못한 것으로 읽는다.
        assertThat(result.getResponse().getContentAsString()).contains("거래가 없습니다");
    }

    @Test
    @DisplayName("목록 응답이 비어 와도 빈 목록과 합계 0 으로 그린다")
    void 응답이_비어_와도_그린다() throws Exception {
        LedgerTestSupport.stubMonth(backendApiClient, null);

        // 로그인 직후 착지라 여기서 터지면 사용자는 로그인하자마자 오류를 만난다.
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk());
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private LedgerMonth ledgerModel(String... params) throws Exception {
        var request = get(LIST_URL).param("year", "2026").param("month", "7")
                .session(LoggedInSessions.member());
        for (int i = 0; i + 1 < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return (LedgerMonth) result.getModelAndView().getModel().get("ledger");
    }
}

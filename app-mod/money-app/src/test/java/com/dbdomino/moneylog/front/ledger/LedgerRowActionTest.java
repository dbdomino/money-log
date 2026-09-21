package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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

/**
 * 5.1 목록 행의 관리 버튼 — <b>행 종류마다 열 수 있는 것이 다르다.</b>
 *
 * <p>표만 보고는 알 수 없어 시험으로 고정한다. 적지 않으면 구현자가 <b>모든 행에 같은
 * 버튼을 그린다</b> — 그러면 고정지출 행에 "누르면 언제나 실패하는" 버튼이 남고, 일반 지출
 * 행에는 자기 지출이 할부인 줄 알게 하는 버튼이 남는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LedgerRowActionTest {

    private static final String LIST_URL = "/ledger";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        LedgerTestSupport.stubCommon(backendApiClient);
        LedgerTestSupport.stubDefaultMonth(backendApiClient);
    }

    @Test
    @DisplayName("지출·소득 행에 수정·삭제가 있다")
    void 지출과_소득에_수정과_삭제가_있다() throws Exception {
        String body = LedgerTestSupport.tableBody(render());

        String expenseRow = LedgerListTest.rowContaining(body, "김치찌개");
        assertThat(expenseRow).contains("수정").contains("삭제");

        String incomeRow = LedgerListTest.rowContaining(body, "7월 급여");
        assertThat(incomeRow).contains("수정").contains("삭제");
    }

    @Test
    @DisplayName("할부 행에 중도상환이 더 있다 (FR-922)")
    void 할부_행에_중도상환이_있다() throws Exception {
        String installmentRow =
                LedgerListTest.rowContaining(LedgerTestSupport.tableBody(render()), "노트북");

        assertThat(installmentRow).contains("수정").contains("삭제").contains("중도상환");
    }

    @Test
    @DisplayName("일반 지출 행에 중도상환이 없다 — 두면 자기 지출이 할부인 줄 안다")
    void 일반_지출에_중도상환이_없다() throws Exception {
        String body = LedgerTestSupport.tableBody(render());

        assertThat(LedgerListTest.rowContaining(body, "김치찌개")).doesNotContain("중도상환");
        assertThat(LedgerListTest.rowContaining(body, "7월 급여")).doesNotContain("중도상환");
        assertThat(LedgerListTest.rowContaining(body, "원룸 월세")).doesNotContain("중도상환");
    }

    @Test
    @DisplayName("고정지출 행에 수정·삭제·중도상환이 없고 안내가 있다 (FR-917)")
    void 고정지출_행은_읽기만_한다() throws Exception {
        String fixedRow =
                LedgerListTest.rowContaining(LedgerTestSupport.tableBody(render()), "원룸 월세");

        // 여기서 지우면 그 달 내역만 사라질지 설정 전체가 사라질지 사용자가 알 수 없다 —
        // 되돌릴 수 없는 동작의 범위가 불분명한 버튼을 두지 않는다.
        assertThat(fixedRow).doesNotContain("수정").doesNotContain("삭제").doesNotContain("중도상환");

        // 왜 이 행만 버튼이 없는지 적지 않으면 화면이 고장 난 것으로 읽힌다.
        assertThat(fixedRow).contains("고정지출 설정");
        assertThat(fixedRow).contains("/fixed-expenses");
    }

    private String render() throws Exception {
        return mockMvc.perform(get(LIST_URL).param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();
    }
}

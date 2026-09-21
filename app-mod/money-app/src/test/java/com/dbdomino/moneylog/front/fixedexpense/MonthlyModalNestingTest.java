package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.web.ModalParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 4.6 단건 수정이 <b>모달을 겹치지 않는다</b>.
 *
 * <p>007 의 모달 스크립트는 겹친 모달을 가정하지 않는다 — Esc 가 전부 닫는 구조라, 겹치면
 * 위의 것만 닫으려 해도 <b>둘 다 닫히고 사용자가 고치던 값이 사라진다.</b>
 *
 * <p><b>시험으로만 고정되는 자리다.</b> 겹쳐도 화면은 열리고, Esc 를 눌러 봐야 드러난다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlyModalNestingTest {

    private static final String LIST_URL = "/fixed-expenses";
    private static final String UPDATE_URL = "/fixed-expenses/monthly/2026/7/1";
    private static final String ITEM_PATH = "/fixed-expenses/monthly/{year}/{month}/{fixedExpenseId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        FixedExpenseTestSupport.stubCommon(backendApiClient);
        FixedExpenseTestSupport.stubFirstPage(backendApiClient);
        FixedExpenseTestSupport.stubMonthly(backendApiClient, FixedExpenseFixture.monthly());
    }

    @Test
    @DisplayName("편집도 같은 모달이다 — 모달을 겹치지 않는다")
    void 편집이_같은_모달이다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "monthly")
                        .param("y", "2026").param("mm", "7").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                // 모달 값이 그대로 monthly 다. 편집용 값이 따로 생기지 않는다.
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"))
                .andReturn().getResponse().getContentAsString();

        // 이 화면이 여는 월별 모달은 하나뿐이다.
        assertThat(occurrences(html, "id=\"modal-fixed-monthly\"")).isOne();
        // 편집 전용 모달을 따로 만들지 않는다.
        assertThat(html).doesNotContain("modal-fixed-monthly-edit");
        // 그리고 그 하나가 편집 상태로 열려 있다.
        assertThat(html).contains("id=\"form-modal-fixed-monthly\"");
    }

    @Test
    @DisplayName("편집 상태의 주소에 대상과 연·월이 함께 실린다")
    void 편집_주소에_대상과_연월이_함께_실린다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "monthly")
                        .param("y", "2026").param("mm", "7")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        String row = FixedExpenseTestSupport.rowContaining(
                FixedExpenseTestSupport.monthlyBody(html), "월세");

        // 셋이 함께여야 한 행이 특정된다 — 고정지출 식별자 하나로는 어느 달인지 모른다.
        assertThat(row).contains("m=monthly").contains("id=1")
                .contains("y=2026").contains("mm=7");
    }

    @Test
    @DisplayName("편집에서 실패해도 그 달과 그 대상이 유지된 채 돌아온다")
    void 실패해도_달과_대상이_유지된다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(Void.class), eq(2026), eq(7), eq(1L)))
                .thenThrow(new BackendApiException(
                        ErrorCode.FIXED_EXPENSE_FIELD_INVALID.code(),
                        "결제일이 그 달 밖입니다."));

        mockMvc.perform(post(UPDATE_URL)
                        .param("amount", "550000")
                        .param("paymentDate", "2026-08-10")
                        .param("content", "7월만 관리비 포함")
                        .param("paymentMethodId", "1")
                        .param("y", "2026").param("mm", "7").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                // 연·월까지 잃으면 어느 달을 보고 있었는지부터 다시 찾아야 한다.
                .andExpect(model().attribute("monthlyQuery", new MonthlyQuery(2026, 7)))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"))
                .andExpect(model().attribute("editingRowId", 1L))
                .andExpect(model().attribute("failMessage", "결제일이 그 달 밖입니다."));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private static int occurrences(String html, String mark) {
        int count = 0;
        for (int at = html.indexOf(mark); at >= 0; at = html.indexOf(mark, at + mark.length())) {
            count++;
        }
        return count;
    }
}

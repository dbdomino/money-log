package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.support.FormFailure;
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
 * 모달의 선택 목록 — <b>관리 목록이 아니라 사용 중 목록</b>이다(FR-909).
 *
 * <p>009 의 관리 목록에는 <b>삭제 표시된 것과 사용 안 함이 함께</b> 들어 있다. 그것을
 * 선택지로 쓰면 사용자가 <b>지운 수단으로 지출을 등록하게</b> 된다.
 *
 * <p>수단은 <b>용도까지 갈린다</b> — 지출에 소득 수단이 뜨면 고른 순간 백엔드가 용도
 * 불일치로 거절하고, 사용자에게는 자기가 고른 것이 거절된 것으로 보인다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActiveChoiceListTest {

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
    @DisplayName("지출 모달의 수단이 사용 중 목록으로 채워지고 지출용을 가리킨다 (FR-909)")
    void 지출_모달은_지출용_사용_중_목록을_쓴다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "expense-create")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attributeExists("expensePaymentMethods"))
                .andReturn().getResponse().getContentAsString();

        // 지출 모달의 수단 칸은 지출용만 담는다 — 소득 수단이 섞이면 고른 순간 백엔드가
        // 용도 불일치로 거절하고, 사용자에게는 자기가 고른 것이 거절된 것으로 보인다.
        String modal = ExpenseModalTest.modalBody(html, "expense-create-body");
        assertThat(modal).contains("국민카드").doesNotContain("월급통장");

        verify(backendApiClient).get(eq("/payment-methods/active/{purpose}"),
                eq(PaymentMethodListResult.class), eq("EXPENSE"));

        // 009 의 관리 목록을 쓰지 않는다 — 거기에는 삭제 표시된 것이 함께 들어 있다.
        verify(backendApiClient, never())
                .getByQuery(eq("/payment-methods"), any(), eq(PaymentMethodListResult.class));
        verify(backendApiClient, never())
                .getByQuery(eq("/expend-groups"), any(), eq(ExpendGroupListResult.class));
    }

    @Test
    @DisplayName("소득 모달은 소득용 사용 중 목록을 쓴다")
    void 소득_모달은_소득용을_쓴다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "income-create")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attributeExists("incomePaymentMethods"))
                .andReturn().getResponse().getContentAsString();

        // 소득 모달의 수단 칸에는 소득용만 담긴다. 지출용 목록도 같은 화면에서 받아 오지만
        // (도구줄의 수단 필터가 둘을 함께 쓴다) 모달마다 가리키는 쪽은 갈려 있어야 한다.
        String modal = ExpenseModalTest.modalBody(html, "income-create-body");
        assertThat(modal).contains("월급통장").doesNotContain("국민카드");

        verify(backendApiClient).get(eq("/payment-methods/active/{purpose}"),
                eq(PaymentMethodListResult.class), eq("INCOME"));
    }

    @Test
    @DisplayName("지출유형도 사용 중 목록으로 채워진다")
    void 지출유형도_사용_중_목록이다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "expense-create")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attributeExists("expendGroups"))
                .andReturn().getResponse().getContentAsString();

        assertThat(ExpenseModalTest.modalBody(html, "expense-create-body"))
                .contains("식비").contains("쇼핑");

        verify(backendApiClient).getByQuery(eq("/expend-groups/active"), any(),
                eq(ExpendGroupListResult.class));
    }

    @Test
    @DisplayName("지출유형을 비우고 저장하면 거절된다 (FR-910)")
    void 지출유형을_비우면_거절된다() throws Exception {
        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_NOT_FOUND.code(),
                        "지출유형을 찾을 수 없습니다."));

        // 지출 한 건에 유형이 하나 필수다.
        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("paymentMethodId", "1")
                        .param("expendGroupId", "")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(FormFailure.FIELD, "expendGroupId"));
    }

    @Test
    @DisplayName("지출유형 칸이 비울 수 없는 칸으로 그려진다")
    void 지출유형_칸이_필수로_그려진다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "expense-create")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        String modal = ExpenseModalTest.modalBody(html, "expense-create-body");
        int at = modal.indexOf("name=\"expendGroupId\"");
        assertThat(at).isGreaterThanOrEqualTo(0);
        assertThat(modal.substring(at, modal.indexOf('>', at))).contains("required");
    }

    @Test
    @DisplayName("선택 목록을 받지 못해도 모달은 열린다 — 그 칸만 빈다")
    void 선택_목록을_못_받아도_모달은_열린다() throws Exception {
        when(backendApiClient.get(eq("/payment-methods/active/{purpose}"),
                eq(PaymentMethodListResult.class), eq("EXPENSE"))).thenReturn(null);

        mockMvc.perform(get(LIST_URL).param("m", "expense-create")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("expensePaymentMethods",
                        org.hamcrest.Matchers.empty()));
    }
}

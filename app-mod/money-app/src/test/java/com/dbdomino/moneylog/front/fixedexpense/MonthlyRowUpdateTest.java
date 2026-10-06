package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.web.ModalParam;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 4.6 그 달 한 건의 수정.
 *
 * <p>핵심은 <b>이 통로로 바뀌는 것이 넷뿐</b>이라는 것이다 — 그 달 금액·결제일·내용·수단.
 * 고정지출 <b>이름과 지출유형은 4.5 에서</b> 바꾼다.
 *
 * <p>그리고 <b>{@code 3405} 를 오류로 끝내지 않는다.</b> 아직 만들어지지 않은 달의 한 건을
 * 고치려 한 경우이고, 그 달을 여는 것이 곧 만드는 일이라 <b>사용자가 할 일은 한 번 더 여는
 * 것뿐</b>이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlyRowUpdateTest {

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
    @DisplayName("단건 수정이 금액·결제일·내용·수단만 싣는다 (FR-1014)")
    void 칸_넷만_실린다() throws Exception {
        mockMvc.perform(filled()).andExpect(status().isOk());

        assertThat(capturedBody()).containsOnlyKeys(
                "amount", "paymentDate", "content", "paymentMethodId");
    }

    @Test
    @DisplayName("이름·지출유형을 싣지 않는다 — 그것은 4.5 에서 바꾼다")
    void 이름과_지출유형은_실리지_않는다() throws Exception {
        mockMvc.perform(filled()).andExpect(status().isOk());

        // 백엔드가 이 통로로 받지 않는 칸을 보내면 언제나 버려지는 값이 생긴다.
        assertThat(capturedBody()).doesNotContainKeys(
                "name", "fixedExpenseName", "expendGroupId", "expendGroupName");
    }

    @Test
    @DisplayName("저장하면 그 행에 「수정됨」이 붙는다")
    void 저장하면_수정됨이_붙는다() throws Exception {
        FixedExpenseTestSupport.stubMonthly(backendApiClient,
                FixedExpenseFixture.monthlyAfterUpdate());

        String html = mockMvc.perform(filled())
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                // 그 달 모달이 열린 채로 돌아온다. 사용자는 고친 결과를 그 자리에서 본다.
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"))
                .andReturn().getResponse().getContentAsString();

        String body = FixedExpenseTestSupport.monthlyBody(html);
        assertThat(FixedExpenseTestSupport.rowContaining(body, "월세")).contains("수정됨");
    }

    @Test
    @DisplayName("결제일이 그 달 안이어야 함이 화면에 적혀 있다 (FR-1014)")
    void 결제일이_그_달_안이어야_함이_적혀_있다() throws Exception {
        String form = editForm();

        assertThat(form).contains("그 달 안이어야");
        // 화면이 먼저 알리고 밖의 날짜는 서버가 거절한다. 둘은 서로를 대신하지 않는다.
        assertThat(form).contains("min=\"2026-07-01\"").contains("max=\"2026-07-31\"");
    }

    @Test
    @DisplayName("3405 가 「먼저 그 달을 열라」는 안내로 이어진다 (FR-1017)")
    void 삼사공오는_안내로_이어진다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(Void.class), eq(2026), eq(7), eq(1L)))
                .thenThrow(new BackendApiException(
                        ErrorCode.FIXED_EXPENSE_MONTHLY_NOT_CREATED.code(),
                        "해당 연·월의 고정지출 내역이 아직 만들어지지 않았습니다."));

        // 오류 화면으로 끝내지 않는다. 그 달을 여는 것이 곧 만드는 일이다.
        mockMvc.perform(filled())
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"))
                .andExpect(result -> assertThat((String) result.getModelAndView()
                        .getModel().get("notice"))
                        .contains("먼저").contains("열"));
    }

    @Test
    @DisplayName("수단 선택 목록이 사용 중 목록(지출용)으로 채워진다")
    void 수단_목록이_사용_중_지출용이다() throws Exception {
        String form = editForm();

        // 지운 수단이나 소득용이 섞이면 고른 순간 백엔드가 거절한다.
        assertThat(form).contains("국민카드(새이름)");
        verify(backendApiClient, org.mockito.Mockito.atLeastOnce())
                .get(eq("/payment-methods/active/{purpose}"), eq(PaymentMethodListResult.class),
                        eq("EXPENSE"));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    /** 칸 넷을 채운 제출. 연·월·대상은 실패 착지를 위해 함께 나른다. */
    private static MockHttpServletRequestBuilder filled() {
        return post(UPDATE_URL)
                .param("amount", "550000")
                .param("paymentDate", "2026-07-10")
                .param("content", "7월만 관리비 포함")
                .param("paymentMethodId", "1")
                .param("y", "2026")
                .param("mm", "7")
                .param("id", "1")
                .session(LoggedInSessions.member());
    }

    /** 편집 상태로 연 모달의 폼. */
    private String editForm() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "monthly")
                        .param("y", "2026").param("mm", "7").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = html.indexOf("id=\"form-modal-fixed-monthly\"");
        assertThat(start).as("편집 폼이 있어야 한다").isGreaterThanOrEqualTo(0);
        return html.substring(start, html.indexOf("</form>", start));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedBody() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).patch(eq(ITEM_PATH), captor.capture(), eq(Void.class),
                eq(2026), eq(7), eq(1L));
        return captor.getValue();
    }
}

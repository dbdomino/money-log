package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
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

/**
 * 3.1 지출 제출 — <b>한 폼에서 나가는 통로가 갈린다.</b>
 *
 * <p>일시불과 할부가 서로 다른 곳으로 나가고 <b>금액 칸 이름도 다르다</b>(FR-912 · SC-904).
 * 특히 <b>토글이 막혀도 고른 값대로 갈린다</b>는 것이 중요하다 — 접는 것은 편의이고 가르는
 * 것이 보장이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseSubmitTest {

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
    @DisplayName("일시불로 저장하면 일시불 통로로 나가고 금액 칸 이름이 그쪽이다")
    void 일시불은_일시불_통로로_나간다() throws Exception {
        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("paymentMethodId", "1")
                        .param("expendGroupId", "5").param("amount", "9500")
                        .param("paymentDate", "2026-07-02").param("place", "회사 근처")
                        .param("content", "김치찌개")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        Map<String, Object> body = capturedBody("/expenses");
        assertThat(body).containsEntry("amount", 9_500L).containsEntry("paymentDate", "2026-07-02");
        // 일시불 폼 그대로 할부를 보내면 형식 오류다. 칸 이름이 섞이지 않아야 한다.
        assertThat(body).doesNotContainKeys("monthlyAmount", "installmentMonths", "startYearMonth");

        verify(backendApiClient, never()).post(eq("/expenses/installments"), any(), any());
    }

    @Test
    @DisplayName("할부로 저장하면 다른 통로로 나가고 개월 수·시작 연월이 함께 실린다 (FR-912·SC-904)")
    void 할부는_다른_통로로_나간다() throws Exception {
        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "INSTALLMENT").param("paymentMethodId", "1")
                        .param("expendGroupId", "6").param("monthlyAmount", "150000")
                        .param("installmentMonths", "12").param("startYearMonth", "2026-05")
                        .param("place", "가전매장").param("content", "노트북")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        Map<String, Object> body = capturedBody("/expenses/installments");
        assertThat(body)
                .containsEntry("monthlyAmount", 150_000L)
                .containsEntry("installmentMonths", 12)
                .containsEntry("startYearMonth", "2026-05");
        // 금액 칸 이름이 다르고 결제일이 없다 — 회차는 시작 연월로 만들어진다.
        assertThat(body).doesNotContainKeys("amount", "paymentDate");

        verify(backendApiClient, never()).post(eq("/expenses"), any(), any());
    }

    @Test
    @DisplayName("토글이 막혀 양쪽 값이 다 와도 고른 값대로 통로가 갈린다 (research 5)")
    void 토글이_막혀도_고른_값대로_갈린다() throws Exception {
        // 스크립트가 막힌 환경이면 칸이 접히지 않아 양쪽 값이 모두 넘어온다.
        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "INSTALLMENT").param("paymentMethodId", "1")
                        .param("expendGroupId", "6")
                        .param("amount", "9999").param("paymentDate", "2026-07-02")
                        .param("monthlyAmount", "150000").param("installmentMonths", "12")
                        .param("startYearMonth", "2026-05")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        // 할부 칸이 채워져 있는지로 짐작하지 않는다. 고른 값 하나로만 가린다.
        verify(backendApiClient, never()).post(eq("/expenses"), any(), any());
        assertThat(capturedBody("/expenses/installments")).doesNotContainKey("amount");
    }

    @Test
    @DisplayName("할부 수정에서 개월 수·시작 연월을 싣지 않는다 — 그 회차 하나만 바뀐다")
    void 할부_수정은_잠긴_칸을_싣지_않는다() throws Exception {
        mockMvc.perform(post("/ledger/expenses/205")
                        .param("paymentMethodId", "1").param("expendGroupId", "6")
                        .param("amount", "140000").param("paymentDate", "2026-07-15")
                        // 주소를 직접 쳐서 잠긴 칸을 실어 보낸 경우를 본뜬다.
                        .param("installmentMonths", "6").param("startYearMonth", "2026-01")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).patch(eq("/expenses/{expenseId}"), captor.capture(),
                eq(Void.class), eq(205L));

        // 화면이 칸을 잠그지만 주소로 직접 올 수 있다. 실어 보내면 백엔드가 거절하므로
        // 화면 모듈이 앞에서 뺀다.
        assertThat(captor.getValue())
                .containsEntry("amount", 140_000L)
                .doesNotContainKeys("installmentMonths", "startYearMonth");
    }

    @Test
    @DisplayName("수단 실패가 수단 칸에, 지출유형 실패가 지출유형 칸에 붙는다")
    void 실패가_칸에_갈라_붙는다() throws Exception {
        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_NOT_FOUND.code(),
                        "결제수단을 찾을 수 없습니다."));

        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("paymentMethodId", "9")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(FormFailure.FIELD, "paymentMethodId"));

        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_NOT_FOUND.code(),
                        "지출유형을 찾을 수 없습니다."));

        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("expendGroupId", "9")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(FormFailure.FIELD, "expendGroupId"));
    }

    @Test
    @DisplayName("지출 값 오류는 한 코드가 네 칸을 가리켜 모달 폼 상단이다")
    void 지출_값_오류는_상단이다() throws Exception {
        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPENSE_FIELD_INVALID.code(),
                        "지출의 금액 또는 날짜 형식이 올바르지 않습니다."));

        // 짐작해 아무 칸에나 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("amount", "0")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(FormFailure.FIELD, FormFailure.FIELD_FORM));
    }

    @Test
    @DisplayName("할부 개월 수 오류는 개월 수 칸에 붙는다")
    void 할부_오류는_개월_수_칸이다() throws Exception {
        when(backendApiClient.post(eq("/expenses/installments"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.INSTALLMENT_VALIDATION_FAILED.code(),
                        "할부 개월 수 또는 금액이 올바르지 않습니다."));

        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "INSTALLMENT").param("installmentMonths", "1")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(FormFailure.FIELD, "installmentMonths"));
    }

    @Test
    @DisplayName("실패해도 그 달 목록이 다시 그려진다 — 이번 달로 튀지 않는다")
    void 실패_착지가_같은_달이다() throws Exception {
        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPENSE_FIELD_INVALID.code(), "오류"));

        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq("/ledger/monthly"), captor.capture(),
                eq(LedgerMonth.class));
        assertThat(captor.getValue()).containsEntry("year", 2026).containsEntry("month", 7);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedBody(String path) {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).post(eq(path), captor.capture(), eq(Void.class));
        return captor.getValue();
    }
}

package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
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

/**
 * 3.3 소득 등록 · 3.4 소득 수정 모달.
 *
 * <p>핵심은 <b>지출유형·장소 칸이 없다</b>는 것이다(FR-914). 백엔드가 그 칸을 받지 않아,
 * 두면 <b>사용자가 채운 값이 조용히 버려진다</b> — 사용자는 자기가 적은 것이 저장됐다고
 * 믿는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncomeModalTest {

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
    @DisplayName("?m=income-create 면 등록 모달이 열린 채 목록이 뜬다")
    void 등록_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "income-create")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "income-create"))
                .andExpect(model().attributeExists("rows"));
    }

    @Test
    @DisplayName("소득 모달에 지출유형·장소 칸이 없다 (FR-914)")
    void 지출유형과_장소_칸이_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "income-create")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        String createModal = ExpenseModalTest.modalBody(html, "income-create-body");
        assertThat(createModal)
                .doesNotContain("name=\"expendGroupId\"")
                .doesNotContain("name=\"place\"");

        String editModal = ExpenseModalTest.modalBody(html, "income-edit-body");
        assertThat(editModal)
                .doesNotContain("name=\"expendGroupId\"")
                .doesNotContain("name=\"place\"");
    }

    @Test
    @DisplayName("소득 모달에 할부 토글이 없다 — 소득에 할부라는 개념이 없다")
    void 할부_토글이_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "income-create")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        String createModal = ExpenseModalTest.modalBody(html, "income-create-body");
        assertThat(createModal)
                .doesNotContain("name=\"payType\"")
                .doesNotContain("name=\"installmentMonths\"")
                .doesNotContain("name=\"monthlyAmount\"");
    }

    @Test
    @DisplayName("없는 식별자면 빈 모달이 아니라 목록만 뜬다")
    void 없는_식별자면_목록만_뜬다() throws Exception {
        when(backendApiClient.get(eq("/incomes/{incomeId}"), eq(IncomeView.class), eq(999L)))
                .thenThrow(new BackendApiException(ErrorCode.INCOME_NOT_FOUND.code(),
                        "소득 내역을 찾을 수 없습니다."));

        mockMvc.perform(get(LIST_URL).param("m", "income-edit").param("id", "999")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attribute("notice", "소득 내역을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("수정 모달에 그 소득의 값이 채워진다")
    void 수정_모달에_값이_채워진다() throws Exception {
        when(backendApiClient.get(eq("/incomes/{incomeId}"), eq(IncomeView.class), eq(501L)))
                .thenReturn(new IncomeView(501L, 3L, 3_500_000L, "2026-07-25", "7월 급여"));

        String html = mockMvc.perform(get(LIST_URL).param("m", "income-edit").param("id", "501")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "income-edit"))
                .andReturn().getResponse().getContentAsString();

        assertThat(ExpenseModalTest.modalBody(html, "income-edit-body"))
                .contains("3500000").contains("2026-07-25").contains("7월 급여");
    }

    @Test
    @DisplayName("등록에서 내용을 비우면 싣지 않고, 수정에서 비우면 비우라는 값으로 싣는다")
    void 내용을_비우는_뜻이_다르다() throws Exception {
        mockMvc.perform(post("/ledger/incomes")
                        .param("paymentMethodId", "3").param("amount", "3500000")
                        .param("paymentDate", "2026-07-25").param("content", "")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> create = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).post(eq("/incomes"), create.capture(), eq(Void.class));
        // 등록에서 빈 내용은 "없다"는 뜻이다.
        assertThat(create.getValue()).doesNotContainKey("content");

        mockMvc.perform(post("/ledger/incomes/501")
                        .param("paymentMethodId", "3").param("amount", "3500000")
                        .param("paymentDate", "2026-07-25").param("content", "")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> update = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).patch(eq("/incomes/{incomeId}"), update.capture(),
                eq(Void.class), eq(501L));
        // 수정에서 빈 내용은 "지운다"는 뜻이다 — 현재 값이 채워진 채로 뜨는 칸이다.
        assertThat(update.getValue()).containsKey("content");
        assertThat(update.getValue().get("content")).isNull();
    }

    @Test
    @DisplayName("소득 모달은 지출유형 목록을 쓰지 않는다 — 그 칸이 없다")
    void 지출유형_목록을_쓰지_않는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "income-create")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 목록 자체는 같은 화면이 받아 온다 — 도구줄의 지출유형 필터가 쓴다. 다만 소득
        // 모달에는 그 값이 닿을 칸이 없어야 한다.
        assertThat(ExpenseModalTest.modalBody(html, "income-create-body"))
                .doesNotContain("식비").doesNotContain("쇼핑");

        // 009 의 관리 목록은 어느 쪽도 부르지 않는다.
        verify(backendApiClient, org.mockito.Mockito.never())
                .getByQuery(eq("/expend-groups"), any(), any());
    }
}

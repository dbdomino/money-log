package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * 3.1 지출 등록 · 3.2 지출 수정 모달이 열리는 방식.
 *
 * <p>핵심은 <b>할부 건의 두 칸이 잠긴다</b>는 것(FR-913 · SC-905)과 <b>없는 대상이면 빈
 * 모달을 띄우지 않는다</b>는 것(FR-916)이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseModalTest {

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
    @DisplayName("?m=expense-create 면 등록 모달이 열린 채 목록이 뜬다 (FR-908)")
    void 등록_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "expense-create")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "expense-create"))
                // 모달은 목록 위에 얹힌다. 부모가 함께 그려져야 한다.
                .andExpect(model().attributeExists("rows"));
    }

    @Test
    @DisplayName("?m=expense-edit&id=… 면 그 지출의 값이 채워진 채 모달이 열린다")
    void 수정_모달에_값이_채워진다() throws Exception {
        when(backendApiClient.get(eq("/expenses/{expenseId}"), eq(ExpenseView.class), eq(101L)))
                .thenReturn(lumpExpense());

        String html = mockMvc.perform(get(LIST_URL).param("m", "expense-edit").param("id", "101")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "expense-edit"))
                .andExpect(model().attributeExists("targetExpense"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("value=\"9500\"").contains("2026-07-02");
    }

    @Test
    @DisplayName("할부 건이면 개월 수·시작 연월이 잠겨 있고 이유와 대안이 적혀 있다 (FR-913·SC-905)")
    void 할부_건은_두_칸이_잠긴다() throws Exception {
        when(backendApiClient.get(eq("/expenses/{expenseId}"), eq(ExpenseView.class), eq(205L)))
                .thenReturn(installmentExpense());

        String html = mockMvc.perform(get(LIST_URL).param("m", "expense-edit").param("id", "205")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        String modal = modalBody(html, "expense-edit-body");

        // 고칠 수 없는 칸을 열어 두고 저장에서 실패시킬 이유가 없다.
        assertThat(modal).contains("readonly").contains("disabled");
        assertThat(modal).contains("개월 수와 시작 연월을 바꿀 수 없습니다");

        // 대안을 적지 않으면 사용자는 막혔다고만 읽는다.
        assertThat(modal).contains("지우고 다시 등록");

        // 잠긴 칸은 이름을 갖지 않는다 — 이름이 있으면 그대로 실려 나가 거절된다.
        assertThat(modal).doesNotContain("name=\"installmentMonths\"");
        assertThat(modal).doesNotContain("name=\"startYearMonth\"");
    }

    @Test
    @DisplayName("일시불 건에는 잠긴 칸 자체가 없다")
    void 일시불_건에는_잠긴_칸이_없다() throws Exception {
        when(backendApiClient.get(eq("/expenses/{expenseId}"), eq(ExpenseView.class), eq(101L)))
                .thenReturn(lumpExpense());

        String modal = modalBody(mockMvc.perform(get(LIST_URL)
                        .param("m", "expense-edit").param("id", "101")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString(), "expense-edit-body");

        assertThat(modal).doesNotContain("개월 수와 시작 연월을 바꿀 수 없습니다");
    }

    @Test
    @DisplayName("없는 식별자면 빈 모달이 아니라 목록만 뜨고 안내가 보인다 (FR-916)")
    void 없는_식별자면_목록만_뜬다() throws Exception {
        when(backendApiClient.get(eq("/expenses/{expenseId}"), eq(ExpenseView.class), eq(999L)))
                .thenThrow(new BackendApiException(ErrorCode.EXPENSE_NOT_FOUND.code(),
                        "지출 내역을 찾을 수 없습니다."));

        mockMvc.perform(get(LIST_URL).param("m", "expense-edit").param("id", "999")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                // 모달을 열지 않기로 한 경우 키 자체를 담지 않는다.
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attribute("notice", "지출 내역을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("식별자 없이 수정 모달을 열면 목록만 뜬다")
    void 식별자가_없으면_열지_않는다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "expense-edit")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));
    }

    @Test
    @DisplayName("모달 제출이 실패하면 모달을 연 채로 목록이 다시 뜬다")
    void 제출_실패는_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.post(eq("/expenses"), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPENSE_FIELD_INVALID.code(),
                        "지출의 금액 또는 날짜 형식이 올바르지 않습니다."));

        mockMvc.perform(post("/ledger/expenses")
                        .param("payType", "LUMP").param("paymentMethodId", "1")
                        .param("expendGroupId", "5").param("amount", "0")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/list"))
                // 모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다.
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "expense-create"))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attribute("failMessage",
                        "지출의 금액 또는 날짜 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("삭제가 실패하면 모달 없이 목록의 안내로 보인다")
    void 삭제_실패는_목록의_안내다() throws Exception {
        doThrow(new BackendApiException(ErrorCode.EXPENSE_NOT_FOUND.code(),
                "지출 내역을 찾을 수 없습니다."))
                .when(backendApiClient).delete(eq("/expenses/{expenseId}"), eq(101L));

        mockMvc.perform(post("/ledger/expenses/101/delete")
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attributeExists("rows"));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private static ExpenseView lumpExpense() {
        return new ExpenseView(101L, 1L, 9_500L, "2026-07-02", "회사 근처", "김치찌개",
                5L, null, null, null, null);
    }

    private static ExpenseView installmentExpense() {
        return new ExpenseView(205L, 1L, 150_000L, "2026-07-15", "가전매장", "노트북",
                6L, 50L, 3, 12, "2026-05");
    }

    /** 모달 본문만 잘라낸다. 목록·도구줄의 문자열이 섞이지 않게 한다. */
    static String modalBody(String html, String bodyClass) {
        int start = html.indexOf("class=\"" + bodyClass + "\"");
        assertThat(start).as("'%s' 모달이 있어야 한다", bodyClass).isGreaterThanOrEqualTo(0);
        int end = html.indexOf("</form>", start);
        return html.substring(start, end);
    }
}

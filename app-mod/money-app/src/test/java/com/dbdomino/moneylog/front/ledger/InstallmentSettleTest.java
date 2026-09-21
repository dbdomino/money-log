package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
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
 * 할부 중도상환 (US4) — <b>수정도 삭제도 아니다.</b>
 *
 * <p>그 지출 한 건을 고치는 일이 아니라 <b>여러 회차를 함께 바꾸는 일</b>이라 수정 모달에
 * 넣지 않는다 — 넣으면 금액을 고치러 들어갔다가 할부 전체가 정리되는 사고가 난다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InstallmentSettleTest {

    private static final String SETTLE_URL = "/ledger/expenses/installments/50/settle";
    private static final String SETTLE_PATH = "/expenses/installments/{installmentGroupId}/remainder";

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
    @DisplayName("중도상환이 POST 로 들어와 본문 없이 나간다")
    void 본문_없이_나간다() throws Exception {
        mockMvc.perform(post(SETTLE_URL)
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/list"));

        // 처리할 대상은 경로의 할부 그룹만으로 충분하다.
        verify(backendApiClient).patch(eq(SETTLE_PATH), isNull(), eq(Void.class), eq(50L));

        // 수정 처리에 얹지 않는다 — 얹으면 금액을 고치러 들어갔다가 할부 전체가 정리된다.
        verify(backendApiClient, never())
                .patch(eq("/expenses/{expenseId}"), org.mockito.ArgumentMatchers.any(),
                        eq(Void.class), org.mockito.ArgumentMatchers.any(Object[].class));
    }

    @Test
    @DisplayName("확인 본문에 되돌릴 수 없다는 것과 남은 회차가 한 번에 정리된다는 것이 적혀 있다 (FR-923)")
    void 확인_본문에_세_가지가_담긴다() throws Exception {
        String html = render();

        // 다이얼로그는 id 로 찾는다 — 같은 값이 버튼의 여는 표시로도 쓰여 첫 자리를
        // 그대로 잡으면 버튼이 잘린다.
        int at = html.indexOf("id=\"confirm-settle-50\"");
        assertThat(at).as("할부 행에 중도상환 확인 다이얼로그가 있어야 한다").isGreaterThanOrEqualTo(0);
        String dialog = html.substring(at, html.indexOf("modal-footer", at));

        // ① 어느 할부인지 ② 남은 회차가 한 번에 정리된다 ③ 되돌릴 수 없다
        assertThat(dialog).contains("노트북");
        assertThat(dialog).contains("남은 회차를 한 번에 정리");
        assertThat(dialog).contains("되돌릴 수 없습니다");
    }

    @Test
    @DisplayName("정리할 회차가 없으면 오류 화면이 아니라 목록의 안내로 보인다")
    void 정리할_회차가_없으면_목록의_안내다() throws Exception {
        when(backendApiClient.patch(eq(SETTLE_PATH), isNull(), eq(Void.class), eq(50L)))
                .thenThrow(new BackendApiException(ErrorCode.INSTALLMENT_NOTHING_TO_SETTLE.code(),
                        "중도상환할 남은 할부 건이 없습니다."));

        // 이미 다 낸 할부이고 사용자가 할 일이 없다. 오류 화면으로 보내면 목록으로
        // 돌아오는 걸음만 늘어난다.
        mockMvc.perform(post(SETTLE_URL)
                        .param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/list"))
                .andExpect(model().attributeExists("rows"))
                .andExpect(model().attribute("failMessage", "중도상환할 남은 할부 건이 없습니다."))
                // 모달이 없는 자리라 모달을 열지 않는다.
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));
    }

    @Test
    @DisplayName("중도상환 버튼과 확인 다이얼로그가 할부 행에만 있다 (FR-922)")
    void 할부_행에만_있다() throws Exception {
        String html = render();
        String body = LedgerTestSupport.tableBody(html);

        assertThat(LedgerListTest.rowContaining(body, "노트북")).contains("중도상환");
        assertThat(LedgerListTest.rowContaining(body, "김치찌개")).doesNotContain("중도상환");
        assertThat(LedgerListTest.rowContaining(body, "7월 급여")).doesNotContain("중도상환");
        assertThat(LedgerListTest.rowContaining(body, "원룸 월세")).doesNotContain("중도상환");

        // 다이얼로그도 할부 그룹 하나뿐이다 — 자료의 네 행 중 할부만 그룹을 갖는다.
        assertThat(html.indexOf("id=\"confirm-settle-50\""))
                .as("할부 그룹의 다이얼로그가 하나 있어야 한다")
                .isEqualTo(html.lastIndexOf("id=\"confirm-settle-50\""))
                .isGreaterThanOrEqualTo(0);
    }

    private String render() throws Exception {
        return mockMvc.perform(get("/ledger").param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();
    }
}

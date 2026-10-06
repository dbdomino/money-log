package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
 * 4.2 목록의 삭제.
 *
 * <p>핵심은 확인 본문에 <b>「월별 내역이 지난 달 것까지 함께 사라진다」</b>가 적혀 있다는
 * 것이다(SC-1006).
 *
 * <p><b>009 와 갈리는 자리다.</b> 수단·지출유형은 <b>삭제 표시</b>라 행이 남고 과거 내역의
 * 이름도 살아 있었다. 고정지출은 <b>물리 삭제</b>이고 <b>내역이 따라 사라진다</b> — 적지
 * 않으면 사용자는 「설정만 지우는 것」으로 읽고 <b>지난 기록이 날아간 뒤에야 안다.</b>
 */
@SpringBootTest
@AutoConfigureMockMvc
class FixedExpenseDeleteTest {

    private static final String LIST_URL = "/fixed-expenses";
    private static final String DELETE_URL = "/fixed-expenses/1/delete";
    private static final String ITEM_PATH = "/fixed-expenses/{fixedExpenseId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        FixedExpenseTestSupport.stubCommon(backendApiClient);
        FixedExpenseTestSupport.stubFirstPage(backendApiClient);
    }

    @Test
    @DisplayName("삭제가 POST 로 나가고 확인 다이얼로그를 거친다")
    void 삭제가_확인을_거쳐_POST_로_나간다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 되돌릴 수 없는 동작을 링크로 두면 브라우저가 미리 불러오는 것만으로 실행된다.
        String row = FixedExpenseTestSupport.rowContaining(
                FixedExpenseTestSupport.tableBody(html), "월세");
        assertThat(row).contains("data-modal-open=\"confirm-fixed-delete-1\"");
        assertThat(row).doesNotContain("href=\"/fixed-expenses/1/delete\"");

        // 다이얼로그의 제출이 POST 다.
        int at = html.indexOf("id=\"confirm-fixed-delete-1\"");
        assertThat(at).isGreaterThanOrEqualTo(0);
        String dialog = html.substring(at, html.indexOf("</div>", html.indexOf("modal-footer", at)));
        assertThat(dialog).contains("method=\"post\"")
                .contains("action=\"/fixed-expenses/1/delete\"");

        mockMvc.perform(post(DELETE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"));
        verify(backendApiClient).delete(eq(ITEM_PATH), eq(1L));
    }

    @Test
    @DisplayName("확인 본문에 월별 내역도 사라진다와 되돌릴 수 없다가 적혀 있다 (SC-1006)")
    void 확인_본문에_세_가지가_담긴다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        int at = html.indexOf("id=\"confirm-fixed-delete-1\"");
        assertThat(at).as("삭제 확인 다이얼로그가 있어야 한다").isGreaterThanOrEqualTo(0);
        String dialog = html.substring(at, html.indexOf("modal-footer", at));

        // ① 어느 고정지출인지 ② 월별 내역이 지난 달 것까지 사라진다 ③ 되돌릴 수 없다
        assertThat(dialog).contains("월세");
        assertThat(dialog).contains("월별 내역");
        assertThat(dialog).contains("지난 달");
        assertThat(dialog).contains("되돌릴 수 없");
    }

    @Test
    @DisplayName("삭제 실패는 오류 화면이 아니라 목록의 안내로 보인다")
    void 삭제_실패는_목록의_안내다() throws Exception {
        doThrow(new BackendApiException(ErrorCode.FIXED_EXPENSE_NOT_FOUND.code(),
                "고정지출을 찾을 수 없습니다."))
                .when(backendApiClient).delete(eq(ITEM_PATH), eq(1L));

        mockMvc.perform(post(DELETE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                .andExpect(model().attributeExists("fixedExpenses"))
                // 삭제는 모달이 없는 자리라 모달을 열지 않는다.
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attribute("failMessage", "고정지출을 찾을 수 없습니다."));
    }
}

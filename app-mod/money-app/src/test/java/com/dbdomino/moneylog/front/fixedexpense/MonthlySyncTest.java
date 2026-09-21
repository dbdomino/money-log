package com.dbdomino.moneylog.front.fixedexpense;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
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
 * 4.6 「고정지출 반영」.
 *
 * <p><b>US2 가 만든 「지난 달은 안 따라온다」의 유일한 해결책이다.</b>
 *
 * <p>핵심은 <b>안전장치가 두 겹</b>이라는 것이다 — 되돌리기 체크박스가 기본 꺼짐이고, 켠 채
 * 누르면 확인 문구가 달라진다. <b>한 겹이면 실수로 켠 것을 알아챌 자리가 없다.</b>
 *
 * <p>그리고 결과에 <b>「보존」이 함께 보인다.</b> 그 숫자가 직접 고친 값이 살아남았다는
 * 증거이고, 되돌리기를 켰을 때 0 이 되는 것으로 무엇이 달랐는지 읽힌다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlySyncTest {

    private static final String LIST_URL = "/fixed-expenses";
    private static final String SYNC_URL = "/fixed-expenses/monthly/sync";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        FixedExpenseTestSupport.stubCommon(backendApiClient);
        FixedExpenseTestSupport.stubFirstPage(backendApiClient);
        FixedExpenseTestSupport.stubMonthly(backendApiClient, FixedExpenseFixture.monthly());
        when(backendApiClient.post(eq(SYNC_URL), any(), eq(SyncResult.class)))
                .thenReturn(FixedExpenseFixture.syncKept());
    }

    @Test
    @DisplayName("반영이 POST 로 나가고 연·월을 싣는다")
    void 반영이_연월을_싣고_POST_로_나간다() throws Exception {
        mockMvc.perform(sync("false"))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"));

        // 연·월은 주소가 아니라 본문으로 간다.
        assertThat(capturedBody())
                .containsEntry("year", 2026)
                .containsEntry("month", 7)
                .containsEntry("overwriteModified", false);
    }

    @Test
    @DisplayName("반영이 확인 다이얼로그를 거친다 (FR-1018)")
    void 반영이_확인을_거친다() throws Exception {
        String html = openMonthly();

        // 버튼이 곧바로 제출하지 않는다.
        int at = html.indexOf("data-sync-open");
        assertThat(at).as("반영 버튼이 있어야 한다").isGreaterThanOrEqualTo(0);
        assertThat(html.substring(html.lastIndexOf("<button", at), at))
                .contains("type=\"button\"");

        // 되돌릴 수 없는 동작이라 확인의 제출이 POST 다.
        String dialog = dialog(html, "confirm-fixed-sync-keep");
        assertThat(dialog).contains("method=\"post\"")
                .contains("action=\"/fixed-expenses/monthly/sync");
    }

    @Test
    @DisplayName("결과로 네 건수가 보이고 목록이 갱신된다 (FR-1019)")
    void 네_건수가_보이고_목록이_갱신된다() throws Exception {
        String html = mockMvc.perform(sync("false"))
                .andReturn().getResponse().getContentAsString();

        int at = html.indexOf("class=\"alert alert-info sync-result\"");
        assertThat(at).as("반영 결과가 보여야 한다").isGreaterThanOrEqualTo(0);
        String result = html.substring(at, html.indexOf("</div>", at));

        // 「보존」이 함께 보이는 것이 요점이다 — 셋만 보이면 손으로 고친 값이 어떻게 됐는지
        // 알 수 없다.
        assertThat(result).contains("추가").contains("갱신").contains("삭제").contains("보존");

        // 목록도 반영 뒤의 것으로 바뀐다.
        assertThat(FixedExpenseTestSupport.monthlyBody(html)).contains("전기요금");
    }

    @Test
    @DisplayName("응답의 목록으로 갱신하고 다시 조회하지 않는다")
    void 다시_조회하지_않는다() throws Exception {
        mockMvc.perform(sync("false")).andExpect(status().isOk());

        // 다시 부르면 그사이 바뀐 값이 섞여 「방금 반영한 결과」가 아닌 것을 보게 된다.
        verify(backendApiClient, never())
                .getByQuery(eq("/fixed-expenses/monthly"), any(), eq(MonthlyResult.class));
    }

    @Test
    @DisplayName("「직접 수정분도 되돌리기」가 기본 꺼짐이다 (FR-1020·SC-1005)")
    void 되돌리기가_기본_꺼짐이다() throws Exception {
        assertThat(overwriteCheckbox(openMonthly())).doesNotContain("checked");
    }

    @Test
    @DisplayName("켜고 누르면 확인 문구가 달라진다 (FR-1021)")
    void 켜면_확인_문구가_달라진다() throws Exception {
        String html = openMonthly();

        String keep = dialog(html, "confirm-fixed-sync-keep");
        String overwrite = dialog(html, "confirm-fixed-sync-overwrite");

        // 범위가 다르므로 문구도 다르다. 같은 문구를 쓰면 두 겹이 한 겹이 된다.
        assertThat(keep).contains("2026년 7월").contains("보존");
        assertThat(keep).doesNotContain("사라");

        assertThat(overwrite).contains("직접 수정한 값이 사라");
        assertThat(overwrite).contains("되돌릴 수 없");

        // 보내는 곳도 갈린다.
        assertThat(keep).contains("overwriteModified=false");
        assertThat(overwrite).contains("overwriteModified=true");
    }

    @Test
    @DisplayName("모달이 열릴 때마다 꺼진 상태로 시작한다")
    void 열_때마다_꺼진_상태로_시작한다() throws Exception {
        when(backendApiClient.post(eq(SYNC_URL), any(), eq(SyncResult.class)))
                .thenReturn(FixedExpenseFixture.syncOverwritten());

        // 되돌리기를 켜고 반영한 직후에도 체크박스는 꺼져 있다.
        String html = mockMvc.perform(sync("true"))
                .andReturn().getResponse().getContentAsString();
        assertThat(overwriteCheckbox(html)).doesNotContain("checked");

        // 닫았다 다시 여는 동안에도 꺼지도록 되돌리는 자리가 걸려 있다.
        assertThat(html).contains("data-sync-reset");

        // 되돌리며 반영하면 보존이 0 이 된다 — 무엇이 달랐는지가 그 숫자로 읽힌다.
        int at = html.indexOf("class=\"alert alert-info sync-result\"");
        assertThat(html.substring(at, html.indexOf("</div>", at))).contains("보존 <strong>0</strong>");
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private static MockHttpServletRequestBuilder sync(String overwriteModified) {
        return post(SYNC_URL)
                .param("y", "2026")
                .param("mm", "7")
                .param("overwriteModified", overwriteModified)
                .session(LoggedInSessions.member());
    }

    private String openMonthly() throws Exception {
        return mockMvc.perform(get(LIST_URL).param("m", "monthly")
                        .param("y", "2026").param("mm", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** 확인 다이얼로그 한 덩이. 본문과 보낼 곳이 모두 그 안에 있다. */
    private static String dialog(String html, String id) {
        int at = html.indexOf("id=\"" + id + "\"");
        assertThat(at).as(id + " 다이얼로그가 있어야 한다").isGreaterThanOrEqualTo(0);
        int footer = html.indexOf("modal-footer", at);
        return html.substring(at, html.indexOf("</div>", html.indexOf("<form", footer)));
    }

    private static String overwriteCheckbox(String html) {
        int at = html.indexOf("id=\"sync-overwrite\"");
        assertThat(at).as("되돌리기 체크박스가 있어야 한다").isGreaterThanOrEqualTo(0);
        return html.substring(html.lastIndexOf("<input", at), html.indexOf(">", at));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedBody() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).post(eq(SYNC_URL), captor.capture(), eq(SyncResult.class));
        return captor.getValue();
    }
}

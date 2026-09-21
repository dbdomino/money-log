package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 5.1 도구줄의 필터 여섯과 정렬 둘 (US3).
 *
 * <p>프로토타입에는 연·월·검색어 셋뿐이라 <b>다섯을 더한다.</b> 그대로 옮기면 스펙의 절반이
 * 없는, <b>거를 수 없는 목록</b>이 된다.
 *
 * <p>핵심 둘 — <b>필터를 걸어도 합계가 그대로</b>이고(SC-903), <b>연·월을 바꿔도 필터가
 * 주소에 남는다</b>(FR-921).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LedgerFilterTest {

    private static final String LIST_URL = "/ledger";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        LedgerTestSupport.stubCommon(backendApiClient);
        LedgerTestSupport.stubMonth(backendApiClient, LedgerFixture.filteredMonth());
    }

    @Test
    @DisplayName("필터 여섯이 백엔드 요청에 각각 실린다 (FR-918~920)")
    void 필터_여섯이_각각_실린다() throws Exception {
        perform(get(LIST_URL)
                .param("year", "2026").param("month", "7")
                .param("paymentMethodId", "1").param("expendGroupId", "5")
                .param("dateFrom", "2026-07-01").param("dateTo", "2026-07-15")
                .param("keyword", "김치").param("type", "EXPENSE"));

        assertThat(capturedQuery())
                .containsEntry("paymentMethodId", 1L)
                .containsEntry("expendGroupId", 5L)
                .containsEntry("dateFrom", "2026-07-01")
                .containsEntry("dateTo", "2026-07-15")
                .containsEntry("keyword", "김치")
                .containsEntry("type", "EXPENSE");
    }

    @Test
    @DisplayName("비어 있는 조건은 싣지 않는다 — 빈 값을 보내면 결과가 통째로 빈다")
    void 비어_있는_조건은_싣지_않는다() throws Exception {
        perform(get(LIST_URL)
                .param("year", "2026").param("month", "7")
                .param("paymentMethodId", "").param("keyword", "").param("dateFrom", ""));

        assertThat(capturedQuery())
                .containsOnlyKeys("year", "month");
    }

    @Test
    @DisplayName("검색어가 내용과 장소 두 칸을 본다는 것이 화면에 적혀 있다")
    void 검색어가_보는_칸이_적혀_있다() throws Exception {
        String html = perform(get(LIST_URL)).getResponse().getContentAsString();

        // 적지 않으면 사용자는 내용만 검색되는 줄 알고 장소로 찾기를 포기한다.
        assertThat(html).contains("내용과 장소를 함께 봅니다");
    }

    @Test
    @DisplayName("종류 필터가 여러 값을 실을 수 있다 (FR-919)")
    void 종류는_여러_값을_실을_수_있다() throws Exception {
        perform(get(LIST_URL).param("year", "2026").param("month", "7")
                .param("type", "EXPENSE").param("type", "INSTALLMENT"));

        // 백엔드가 콤마로 받는다. 한 값만 보내면 "지출과 할부만" 같은 조합을 만들 수 없다.
        assertThat(capturedQuery()).containsEntry("type", "EXPENSE,INSTALLMENT");
    }

    @Test
    @DisplayName("정렬 기준과 방향이 함께 실리고 기준은 결제일과 금액 둘뿐이다 (FR-920)")
    void 정렬_기준은_둘뿐이다() throws Exception {
        perform(get(LIST_URL).param("year", "2026").param("month", "7")
                .param("sort", "amount").param("order", "asc"));

        assertThat(capturedQuery()).containsEntry("sort", "amount").containsEntry("order", "asc");
    }

    @Test
    @DisplayName("백엔드가 받지 않는 정렬 기준은 싣지 않는다 — 화면이 스스로 정렬하지도 않는다")
    void 받지_않는_정렬_기준은_싣지_않는다() throws Exception {
        MvcResult result = perform(get(LIST_URL).param("year", "2026").param("month", "7")
                .param("sort", "paymentMethodName"));

        // 부를 방법이 없는 기준이다. 실어 보내면 백엔드가 거절한다.
        assertThat(capturedQuery()).doesNotContainKey("sort");

        // 화면이 받은 목록을 스스로 다시 정렬하지도 않는다 — 지금은 한 달치를 전부 받아
        // 괜찮아 보이지만 그 가정이 화면 코드에 숨고, 쪽 넘기기가 생기는 순간 깨진다.
        LedgerMonth model = (LedgerMonth) result.getModelAndView().getModel().get("ledger");
        assertThat(model.rows()).containsExactlyElementsOf(LedgerFixture.filteredMonth().rows());
    }

    @Test
    @DisplayName("연·월을 바꿔도 필터가 주소에 남는다 (FR-921)")
    void 연월을_바꿔도_필터가_남는다() throws Exception {
        String html = perform(get(LIST_URL)
                .param("year", "2026").param("month", "7")
                .param("paymentMethodId", "1").param("keyword", "김치")
                .param("type", "EXPENSE"))
                .getResponse().getContentAsString();

        // 연·월만 바꾼 주소를 만들면 나머지가 그대로 실린다 — 주소 방식에서 저절로 따라온다.
        String nextLink = linkContaining(html, "month=8");
        assertThat(nextLink).contains("paymentMethodId=1").contains("type=EXPENSE");
        assertThat(nextLink).contains("keyword=");

        String prevLink = linkContaining(html, "month=6");
        assertThat(prevLink).contains("paymentMethodId=1").contains("type=EXPENSE");
    }

    @Test
    @DisplayName("필터를 걸어도 상단 합계가 그대로다 (SC-903)")
    void 필터를_걸어도_합계가_그대로다() throws Exception {
        MvcResult result = perform(get(LIST_URL)
                .param("year", "2026").param("month", "7").param("keyword", "김치"));

        LedgerMonth model = (LedgerMonth) result.getModelAndView().getModel().get("ledger");

        // 목록은 한 건인데 합계는 그 달 전체 기준이다.
        assertThat(model.rows()).hasSize(1);
        assertThat(model.expenseSum()).isEqualTo(659_500L);
        assertThat(model.incomeSum()).isEqualTo(3_500_000L);
    }

    @Test
    @DisplayName("결과가 0건이어도 필터가 풀리지 않는다")
    void 결과가_0건이어도_필터가_풀리지_않는다() throws Exception {
        LedgerTestSupport.stubMonth(backendApiClient, LedgerFixture.emptyMonth());

        String html = perform(get(LIST_URL)
                .param("year", "2026").param("month", "7").param("keyword", "없는말"))
                .getResponse().getContentAsString();

        // 사용자가 건 조건이다. 자동으로 풀면 자기가 무엇을 걸었는지 잃는다.
        assertThat(html).contains("value=\"없는말\"");
        assertThat(html).contains("조건은 그대로 두었습니다");

        // 다시 조회할 때도 그 조건이 그대로 실린다.
        assertThat(capturedQuery()).containsEntry("keyword", "없는말");
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedQuery() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq("/ledger/monthly"), captor.capture(),
                eq(LedgerMonth.class));
        return captor.getValue();
    }

    /** 그 문구를 담은 링크의 href 값을 잘라낸다. */
    private static String linkContaining(String html, String text) {
        int at = html.indexOf(text);
        assertThat(at).as("'%s' 를 담은 링크가 있어야 한다", text).isGreaterThanOrEqualTo(0);
        int start = html.lastIndexOf("href=\"", at);
        int end = html.indexOf('"', start + "href=\"".length());
        return html.substring(start, end);
    }
}

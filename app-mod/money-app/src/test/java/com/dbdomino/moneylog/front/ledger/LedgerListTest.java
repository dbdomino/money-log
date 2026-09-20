package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.time.LocalDate;
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
 * 5.1 월별 가계부 — 한 목록에 섞인 네 종류.
 *
 * <p>핵심은 <b>고정지출 행만 현재 이름</b>이라는 것이다(FR-905 · SC-906). 눈으로 보면
 * "고정지출만 이름이 다르다"가 버그처럼 읽혀 <b>시험으로만 고정된다.</b>
 */
@SpringBootTest
@AutoConfigureMockMvc
class LedgerListTest {

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
    @DisplayName("주소에 연·월이 없으면 이번 달이 선택된다 (FR-901)")
    void 연월이_없으면_이번_달이다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/list"));

        // 브라우저 시각이 아니라 서버 시각으로 정한다. 시차가 있는 사용자가 다른 달을
        // 보지 않게 하려는 것이다.
        LocalDate today = LocalDate.now();
        assertThat(capturedQuery())
                .containsEntry("year", today.getYear())
                .containsEntry("month", today.getMonthValue());
    }

    @Test
    @DisplayName("주소의 연·월이 그대로 조회에 실린다")
    void 주소의_연월이_실린다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        assertThat(capturedQuery()).containsEntry("year", 2026).containsEntry("month", 7);
    }

    @Test
    @DisplayName("네 종류가 뱃지로 구분된다 (FR-902)")
    void 네_종류가_뱃지로_구분된다() throws Exception {
        String body = LedgerTestSupport.tableBody(render());

        assertThat(body)
                .contains("badge-expense")
                .contains("badge-installment")
                .contains("badge-income")
                .contains("badge-fixed");
        assertThat(body).contains("지출").contains("할부").contains("소득").contains("고정지출");

        // EXPENSE·INSTALLMENT 는 화면 밖의 값이다. 표에 그대로 나오면 안 된다.
        assertThat(body)
                .doesNotContain("EXPENSE")
                .doesNotContain("INSTALLMENT")
                .doesNotContain(">INCOME<")
                .doesNotContain(">FIXED<");
    }

    @Test
    @DisplayName("소득 행의 지출유형·장소가 빈 칸이고 지어낸 말이 없다 (FR-906)")
    void 소득_행의_빈_칸에_말을_지어내지_않는다() throws Exception {
        String body = LedgerTestSupport.tableBody(render());

        // 「없음」·「-」 같은 말을 채우면 "비어 있는 값"과 "없는 항목"이 구분되지 않는다.
        assertThat(body).doesNotContain("없음").doesNotContain("해당 없음");

        // 소득 행은 내용만 갖는다. 다른 행의 유형 이름이 소득 행에 새어 들어가지 않는지
        // 그 행 하나만 잘라서 본다.
        String incomeRow = rowContaining(body, "7월 급여");
        assertThat(incomeRow).doesNotContain("식비").doesNotContain("쇼핑").doesNotContain("주거");
        assertThat(incomeRow).doesNotContain("회사 근처").doesNotContain("가전매장");
    }

    @Test
    @DisplayName("할부 행에 회차가 보인다")
    void 할부_행에_회차가_보인다() throws Exception {
        String installmentRow = rowContaining(LedgerTestSupport.tableBody(render()), "노트북");

        // 적지 않으면 사용자는 같은 지출이 달마다 여러 번 들어간 줄 안다.
        assertThat(installmentRow).contains("3/12");
    }

    @Test
    @DisplayName("고정지출 행만 현재 이름이고 나머지는 받은 값 그대로다 (FR-905·SC-906)")
    void 이름을_다시_조회해_덮어쓰지_않는다() throws Exception {
        String body = LedgerTestSupport.tableBody(render());

        // 자료의 네 행이 같은 수단(1번)을 가리키는데 고정지출 행만 이름이 다르다.
        // 화면이 맞추려 들면 넷이 같은 이름으로 보인다 — 그것이 "과거 기록이 사후에
        // 바뀌는" 화면이다.
        assertThat(rowContaining(body, "김치찌개")).contains("국민카드");
        assertThat(rowContaining(body, "노트북")).contains("국민카드");
        assertThat(rowContaining(body, "원룸 월세")).contains("새이름카드");

        // 화면이 이름을 다시 받아 오지 않는다 — 부르는 호출 자체가 없어야 한다.
        verify(backendApiClient, org.mockito.Mockito.never())
                .get(eq("/payment-methods/{paymentMethodId}"), any(), any(Object[].class));
    }

    @Test
    @DisplayName("목록 요청에 조회 구간이 실리지 않는다 (research 10)")
    void 조회_구간을_싣지_않는다() throws Exception {
        render();

        // 한 달치를 전부 받고 합계가 함께 온다. 쪽을 나누면 합계와 목록의 기준이 갈린다.
        assertThat(capturedQuery()).doesNotContainKeys("offset", "limit", "page", "size");
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private String render() throws Exception {
        return mockMvc.perform(get(LIST_URL).param("year", "2026").param("month", "7")
                        .session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedQuery() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq("/ledger/monthly"), captor.capture(),
                eq(LedgerMonth.class));
        return captor.getValue();
    }

    /** 표 본문에서 그 문구를 담은 행 하나만 잘라낸다. */
    static String rowContaining(String tableBody, String text) {
        int at = tableBody.indexOf(text);
        assertThat(at).as("표에 '%s' 를 담은 행이 있어야 한다", text).isGreaterThanOrEqualTo(0);
        int start = tableBody.lastIndexOf("<tr", at);
        int end = tableBody.indexOf("</tr>", at);
        return tableBody.substring(start, end);
    }
}

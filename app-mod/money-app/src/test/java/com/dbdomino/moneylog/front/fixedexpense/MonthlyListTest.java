package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.web.ModalParam;
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
 * 4.6 월별 고정지출 내역의 목록.
 *
 * <p>핵심은 <b>그 달 전체를 보는 화면</b>이라는 것이다 — 설정 하나를 여는 것이 아니라서
 * 모달 값에 대상 식별자가 붙지 않는다.
 *
 * <p>그리고 화면이 <b>스스로 계산하지 않는다</b>: 합계를 다시 더하지 않고 말일 보정도 다시
 * 하지 않는다. 둘 다 같은 규칙이 두 곳에 생기는 것을 막는 자리다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MonthlyListTest {

    private static final String LIST_URL = "/fixed-expenses";

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
    @DisplayName("모달 값에 대상 식별자가 붙지 않는다 — 그 달 전체를 보는 화면이다 (FR-1012)")
    void 모달_값에_대상_식별자가_붙지_않는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        int at = html.indexOf("page-header-actions");
        String actions = html.substring(at, html.indexOf("</div>", at));
        int label = actions.indexOf("월별 내역");
        String anchor = actions.substring(actions.lastIndexOf("<a", label), label);

        assertThat(anchor).contains("m=monthly");
        // id 가 붙으면 「그 고정지출의 달별 기록」이 된다. 붙는 것은 단건 수정에 들어갈 때뿐이다.
        assertThat(anchor).doesNotContain("id=");
    }

    @Test
    @DisplayName("연·월이 없으면 서버 시각의 이번 달이다")
    void 연월이_없으면_이번_달이다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "monthly").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "monthly"));

        // 브라우저 시각을 믿으면 시차가 있는 사용자가 다른 달을 본다.
        LocalDate today = LocalDate.now();
        assertThat(capturedMonthlyQuery())
                .containsEntry("year", today.getYear())
                .containsEntry("month", today.getMonthValue());
    }

    @Test
    @DisplayName("연·월이 바뀌면 그 달로 다시 조회한다 (FR-1011)")
    void 연월이_바뀌면_다시_조회한다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "monthly").param("y", "2026").param("mm", "7")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                // 세션에 두지 않으므로 뒤로 가기와 새 탭에서도 같은 달이 뜬다.
                .andExpect(model().attribute("monthlyQuery", new MonthlyQuery(2026, 7)));

        assertThat(capturedMonthlyQuery()).containsEntry("year", 2026).containsEntry("month", 7);
    }

    @Test
    @DisplayName("직접 고친 행만 「수정됨」으로 구분된다 (FR-1013·SC-1004)")
    void 직접_고친_행만_구분된다() throws Exception {
        String body = FixedExpenseTestSupport.monthlyBody(openMonthly());

        // 이 표시가 반영의 전제다. 구분이 없으면 반영을 누를 때 무엇이 보존되는지 모른다.
        assertThat(FixedExpenseTestSupport.rowContaining(body, "통신비")).contains("수정됨");
        // 양쪽에 다 말이 붙으면 표에서 눈에 띄어야 할 대비가 사라진다.
        assertThat(FixedExpenseTestSupport.rowContaining(body, "월세")).doesNotContain("수정됨");
    }

    @Test
    @DisplayName("상단 합계가 그 달 전체 기준이고 화면이 목록을 다시 더하지 않는다 (FR-1015)")
    void 합계를_다시_더하지_않는다() throws Exception {
        String head = FixedExpenseTestSupport.monthlyHead(openMonthly());

        // 자료의 행 합은 555,000 이고 그 달 합계는 900,000 이다. 화면이 다시 더하면
        // 여기서 두 값이 갈린다.
        assertThat(head).contains("900,000");
        assertThat(head).doesNotContain("555,000");
    }

    @Test
    @DisplayName("그 달 대상이 없으면 안내가 보인다 (FR-1016)")
    void 빈_달에는_안내가_보인다() throws Exception {
        FixedExpenseTestSupport.stubMonthly(backendApiClient, FixedExpenseFixture.monthlyEmpty());

        // 빈 목록만 두면 사용자는 불러오지 못한 것으로 읽는다.
        assertThat(FixedExpenseTestSupport.monthlyBody(openMonthly()))
                .contains("이 달에 해당하는 고정지출이 없습니다");
    }

    @Test
    @DisplayName("말일로 보정된 결제일을 그대로 보이고 화면이 계산하지 않는다")
    void 보정된_결제일을_그대로_보인다() throws Exception {
        FixedExpenseTestSupport.stubMonthly(backendApiClient,
                FixedExpenseFixture.monthlyAdjusted());

        String body = FixedExpenseTestSupport.monthlyBody(
                openMonthly("2026", "2"));

        // 매달 결제일이 31 이어도 그 달이 28일까지면 서버가 맞춘 값이 온다.
        assertThat(FixedExpenseTestSupport.rowContaining(body, "월세")).contains("2026-02-28");
        assertThat(body).doesNotContain("2026-02-31");
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private String openMonthly() throws Exception {
        return openMonthly("2026", "7");
    }

    private String openMonthly(String year, String month) throws Exception {
        return mockMvc.perform(get(LIST_URL).param("m", "monthly")
                        .param("y", year).param("mm", month)
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedMonthlyQuery() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq("/fixed-expenses/monthly"), captor.capture(),
                eq(MonthlyResult.class));
        return captor.getValue();
    }
}

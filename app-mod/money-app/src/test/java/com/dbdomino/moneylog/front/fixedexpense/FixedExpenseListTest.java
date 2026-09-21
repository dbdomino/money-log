package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

/**
 * 4.2 고정지출 설정 목록.
 *
 * <p>핵심은 <b>프로토타입과 다른 세 곳</b>이다 — 상태 필터가 없고, 적용 기간 열이 있으며,
 * 관리 열에 삭제가 있고 「월별」이 없다. <b>프로토타입을 그대로 옮긴 화면도 눈으로는
 * 멀쩡해 보여</b> 시험으로만 고정된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FixedExpenseListTest {

    private static final String LIST_URL = "/fixed-expenses";

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
    @DisplayName("목록에 적용 기간 열이 보인다")
    void 적용_기간_열이_보인다() throws Exception {
        String html = render();
        String body = FixedExpenseTestSupport.tableBody(html);

        assertThat(html).contains("적용 기간");
        // 저장은 정수 네 칸이지만 읽는 자리에서는 이어 붙인다.
        assertThat(FixedExpenseTestSupport.rowContaining(body, "월세"))
                .contains("2026-01 ~ 2026-12");
        assertThat(FixedExpenseTestSupport.rowContaining(body, "인터넷"))
                .contains("2026-07 ~ 2026-12");
    }

    @Test
    @DisplayName("상태 필터가 없다 — 백엔드에 상태 개념 자체가 없다")
    void 상태_필터가_없다() throws Exception {
        String html = render();

        // 프로토타입 도구줄의 「전체·진행·완료·중도상환」이 그대로 옮겨지면 아무것도
        // 걸러지지 않는 필터가 남는다. 「중도상환」은 할부(010)의 개념이기도 하다.
        assertThat(html).doesNotContain("진행").doesNotContain("중도상환");
        assertThat(html).doesNotContain("id=\"fx-status\"");
    }

    @Test
    @DisplayName("관리 열에 상세·수정·삭제가 있고 「월별」이 없다")
    void 관리_열에_월별이_없고_삭제가_있다() throws Exception {
        String body = FixedExpenseTestSupport.tableBody(render());
        String row = FixedExpenseTestSupport.rowContaining(body, "월세");

        assertThat(row).contains("상세").contains("수정").contains("삭제");

        // 「월별」이 행에 있으면 「이 고정지출의 월별」로 읽힌다 — 4.6 은 그 달 전체를
        // 보는 화면이다.
        assertThat(body).doesNotContain("월별");
    }

    @Test
    @DisplayName("「월별 내역」이 화면 상단에 하나 있다")
    void 월별_내역이_상단에_하나다() throws Exception {
        String html = render();

        // 버튼의 위치가 곧 범위의 선언이다.
        assertThat(html).contains("월별 내역");
        assertThat(html).contains("m=monthly");

        // 표 안에는 없다.
        assertThat(FixedExpenseTestSupport.tableBody(html)).doesNotContain("m=monthly");
    }

    @Test
    @DisplayName("목록 요청이 007 의 환산기로 만들어져 시작점이 개수의 배수다")
    void 시작점이_개수의_배수다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("page", "2").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"));

        Map<String, Object> query = capturedQuery();
        int offset = (int) query.get("offset");
        int limit = (int) query.get("limit");

        // 시작점이 개수의 배수가 아니면 목록이 통째로 실패한다. 환산기는 쪽 번호에서만
        // 시작점을 만들므로 어긋난 값이 생길 경로가 없다.
        assertThat(offset % limit).isZero();
        assertThat(offset).isEqualTo(2 * limit);
    }

    @Test
    @DisplayName("수단·유형 이름을 다시 조회해 덮어쓰지 않는다")
    void 이름을_덮어쓰지_않는다() throws Exception {
        String body = FixedExpenseTestSupport.tableBody(render());

        // 자료의 설정은 「국민카드」를 들고 있고 사용 중 목록은 「국민카드(새이름)」이다.
        // 화면이 덮어쓰면 표의 수단 열이 선택지의 이름으로 바뀐다.
        assertThat(FixedExpenseTestSupport.rowContaining(body, "월세")).contains("국민카드");
        assertThat(body).doesNotContain("국민카드(새이름)");

        // 이름을 받아 오려고 수단을 따로 조회하지도 않는다.
        verify(backendApiClient, never())
                .get(eq("/payment-methods/{paymentMethodId}"), any(), any(Object[].class));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private String render() throws Exception {
        return mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedQuery() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq("/fixed-expenses"), captor.capture(),
                eq(FixedExpenseListResult.class));
        return captor.getValue();
    }
}

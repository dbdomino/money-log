package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
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
 * 4.5 수정 모달의 <b>파급 범위 안내</b>.
 *
 * <p>기본값을 고치면 백엔드가 <b>미래 달이면서 직접 고치지 않은</b> 월별 내역을 함께
 * 갱신한다. <b>응답에 드러나지 않는 부작용</b>이라 화면이 적지 않으면 사용자는 지난달이
 * 바뀌지 않은 것을 <b>버그로 신고한다</b>.
 *
 * <p>작지만 P1 인 이유가 그것이다 — 이 기능에서 가장 오해하기 쉬운 지점이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UpdateScopeNoticeTest {

    private static final String LIST_URL = "/fixed-expenses";
    private static final String ITEM_PATH = "/fixed-expenses/{fixedExpenseId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        FixedExpenseTestSupport.stubCommon(backendApiClient);
        FixedExpenseTestSupport.stubFirstPage(backendApiClient);
        when(backendApiClient.get(eq(ITEM_PATH), eq(FixedExpenseView.class), eq(1L)))
                .thenReturn(FixedExpenseFixture.monthlyRent());
    }

    @Test
    @DisplayName("수정 모달에 「미래 달 중 직접 고치지 않은 내역에만 반영된다」가 보인다 (FR-1009·SC-1002)")
    void 미래_달에만_반영된다가_보인다() throws Exception {
        String notice = notice(openEditModal());

        assertThat(notice).as("파급 범위 안내가 수정 모달에 있어야 한다").isNotEmpty();
        assertThat(notice).contains("미래 달").contains("직접 고치지 않은").contains("반영");
    }

    @Test
    @DisplayName("안내가 저장 전에 보인다 — 제출 뒤 착지가 아니라 모달이 열릴 때다")
    void 안내가_저장_전에_보인다() throws Exception {
        // 아무것도 제출하지 않았다. 모달을 여는 것만으로 안내가 함께 온다.
        String html = openEditModal();

        // 뒤에 알리면 「그럴 줄 알았으면 안 고쳤다」가 되고 되돌릴 방법도 마땅치 않다.
        assertThat(notice(html)).isNotEmpty();

        // 안내는 폼보다 앞에 있다 — 값을 고치기 전에 읽는 자리다.
        String modal = FixedExpenseTestSupport.modalBody(html, "fixed-edit-body");
        assertThat(modal.indexOf("update-scope-notice"))
                .as("안내가 입력 폼보다 앞에 있어야 한다")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(modal.indexOf("name=\"name\""));
    }

    @Test
    @DisplayName("「직접 고친 달은 그대로다」가 함께 적혀 있다")
    void 직접_고친_달은_그대로다가_적혀_있다() throws Exception {
        // 4.6 에서 「수정됨」으로 지켜 둔 값이 보존된다는 뜻이며 그쪽의 반영과 이어진다.
        assertThat(notice(openEditModal())).contains("직접 고친 달").contains("그대로");
    }

    @Test
    @DisplayName("4.6 으로 가는 길이 같은 자리에 있다 (FR-1010·SC-1003)")
    void 사륙으로_가는_길이_안내_안에_있다() throws Exception {
        String notice = notice(openEditModal());

        // 제약만 적고 길을 적지 않으면 사용자는 「막혔다」고만 읽는다.
        assertThat(notice).contains("지난 달");
        // 길을 모달 안에 둔다. 닫고 찾아가게 하면 무엇을 하려 했는지 잊는다.
        assertThat(notice).contains("m=monthly");
    }

    @Test
    @DisplayName("화면이 「몇 건이 바뀝니다」를 계산해 보이지 않는다")
    void 건수를_계산하지_않는다() throws Exception {
        String html = openEditModal();

        // 숫자를 보여 주는 편이 친절해 보여 구현자가 더하고 싶어진다. 그런데 그 숫자를
        // 내려면 월별 내역을 전부 뒤져야 하고, 「무엇이 따라오는가」의 판정 규칙이 화면과
        // 백엔드 두 곳에 생긴다. 규칙을 말로 적는다.
        assertThat(notice(html)).doesNotContainPattern("\\d");

        // 건수를 내려고 월별 내역을 조회하지도 않는다.
        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        verify(backendApiClient, atLeastOnce()).getByQuery(paths.capture(), any(), any());
        assertThat(paths.getAllValues()).noneMatch(path -> path.contains("monthly"));
    }

    @Test
    @DisplayName("등록 모달에는 안내를 두지 않는다 — 새로 만드는 것에는 따라올 과거가 없다")
    void 등록_모달에는_안내가_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).param("m", "create")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(FixedExpenseTestSupport.modalBody(html, "fixed-create-body"))
                .doesNotContain("update-scope-notice");
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private String openEditModal() throws Exception {
        return mockMvc.perform(get(LIST_URL).param("m", "edit").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** 파급 범위 안내 한 덩이만 잘라낸다. 폼의 문구가 섞이지 않게 한다. */
    private static String notice(String html) {
        int start = html.indexOf("class=\"alert alert-info update-scope-notice\"");
        if (start < 0) {
            return "";
        }
        int end = html.indexOf("</div>", start);
        return end < 0 ? html.substring(start) : html.substring(start, end);
    }
}

package com.dbdomino.moneylog.front.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
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
 * 2.5 지출유형 목록.
 *
 * <p>수단 목록과 같은 규칙에 <b>아이콘과 기본 유형</b> 둘이 더 있다. 기본 유형은 화면이 미리
 * 막는 유일한 자리이며, 그 근거가 목록의 구분 열이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpendGroupListTest {

    private static final String LIST_URL = "/expend-groups";
    private static final String LIST_PATH = "/expend-groups";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_PATH), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(ExpendGroupFixture.page());
    }

    @Test
    @DisplayName("기본 유형과 직접 만든 유형이 구분 열로 갈려 보인다")
    void 구분_열이_기본과_직접_만듦을_가른다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"))
                .andReturn().getResponse().getContentAsString();

        // 기본 유형만 버튼이 다른 이유를 사용자가 이 열을 보고 안다.
        assertThat(html).contains("식비").contains("취미");
        assertThat(html).contains("기본").contains("직접 만듦");
    }

    @Test
    @DisplayName("기본 유형 행에는 삭제 버튼이 없다")
    void 기본_유형에는_삭제_버튼이_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 시도할 방법이 화면에 없어야 한다 — 막는 것이 아니라 길을 두지 않는 것이다.
        assertThat(html).doesNotContain("confirm-expend-group-delete-1");
        assertThat(html).contains("confirm-expend-group-delete-2");
    }

    @Test
    @DisplayName("삭제 표시된 유형이 목록에 남고 아이콘도 계속 보인다")
    void 삭제된_유형이_아이콘과_함께_남는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("없앤유형");
        assertThat(html).contains("삭제됨");
        // 행이 남으므로 아이콘도 남는다.
        assertThat(html).contains("/expend-groups/icons/1_4.gif");
    }

    @Test
    @DisplayName("삭제된 행에는 수정·삭제 버튼이 없고 상세만 있다")
    void 삭제된_행에는_상세만_있다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("m=edit&amp;id=2");
        assertThat(html).doesNotContain("m=edit&amp;id=4");
        assertThat(html).doesNotContain("confirm-expend-group-delete-4");
        assertThat(html).contains("m=detail&amp;id=4");
    }

    @Test
    @DisplayName("목록 요청에 조회 구간이 실리지 않는다")
    void 조회_구간을_싣지_않는다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq(LIST_PATH), query.capture(),
                eq(ExpendGroupListResult.class));
        assertThat(query.getValue()).doesNotContainKeys("offset", "limit");
    }

    @Test
    @DisplayName("응답이 비어 와도 빈 목록으로 그린다")
    void 응답이_비어도_빈_목록으로_그린다() throws Exception {
        when(backendApiClient.getByQuery(eq(LIST_PATH), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(null);

        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"));
    }
}

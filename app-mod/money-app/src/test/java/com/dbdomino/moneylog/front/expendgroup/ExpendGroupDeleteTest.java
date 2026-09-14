package com.dbdomino.moneylog.front.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 지출유형 삭제.
 *
 * <p>실패 넷 가운데 <b>사용 중인 유형만 대안을 제시한다</b>. 나머지는 사용자가 할 수 있는
 * 일이 없지만 이 경우는 목적을 이룰 다른 길이 있다 — 과거 지출이 그 유형을 가리켜야 하므로
 * 지울 수 없지만 앞으로 쓰지 않게 할 수는 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpendGroupDeleteTest {

    private static final String LIST_URL = "/expend-groups";
    private static final String ITEM_PATH = "/expend-groups/{expendGroupId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(ExpendGroupFixture.page());
    }

    @Test
    @DisplayName("삭제가 POST 로 나간다")
    void 삭제는_POST_로_나간다() throws Exception {
        // 되돌릴 수 없는 동작을 링크로 두면 브라우저가 미리 불러오는 것만으로 실행된다.
        mockMvc.perform(post("/expend-groups/2/delete").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"));

        verify(backendApiClient).delete(eq(ITEM_PATH), eq(2L));
    }

    @Test
    @DisplayName("확인 본문에 이름이 계속 점유된다는 것이 적혀 있다")
    void 확인_본문이_이름_점유를_알린다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 수단의 확인 본문과 다른 점이며, 지출유형에서 사용자가 가장 자주 부딪히는 사실이다.
        assertThat(html).contains("이름은 계속 점유되어 같은 이름으로 다시 만들 수 없습니다");
        assertThat(html).contains("되돌릴 수 없고");
        assertThat(html).contains("/expend-groups/2/delete");
    }

    @Test
    @DisplayName("사용 중인 유형의 삭제에는 사용 안 함으로 돌리라는 대안이 함께 뜬다")
    void 사용_중이면_대안을_제시한다() throws Exception {
        doThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_IN_USE.code(),
                ErrorCode.EXPEND_GROUP_IN_USE.message()))
                .when(backendApiClient).delete(eq(ITEM_PATH), eq(2L));

        String html = mockMvc.perform(post("/expend-groups/2/delete")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_IN_USE.message());
        assertThat(html)
                .as("대안을 적지 않으면 사용자는 막혔다고만 읽는다")
                .contains("「사용 안 함」으로 돌려 두세요");
    }

    @Test
    @DisplayName("기본 유형·재삭제·없는 유형은 오류 화면이 아니라 목록 안내로 보인다")
    void 나머지_실패는_목록_안내다() throws Exception {
        doThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_DEFAULT_UNDELETABLE.code(),
                ErrorCode.EXPEND_GROUP_DEFAULT_UNDELETABLE.message()))
                .when(backendApiClient).delete(eq(ITEM_PATH), eq(1L));

        String html = mockMvc.perform(post("/expend-groups/1/delete")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_DEFAULT_UNDELETABLE.message());
        assertThat(html).contains("식비");
        // 이 셋에는 대안이 없다. 사용자가 할 수 있는 일이 없기 때문이다.
        assertThat(html).doesNotContain("「사용 안 함」으로 돌려 두세요");
    }

    @Test
    @DisplayName("이미 삭제된 유형의 재삭제도 목록 안내로 보인다")
    void 재삭제도_목록_안내다() throws Exception {
        doThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_ALREADY_DELETED.code(),
                ErrorCode.EXPEND_GROUP_ALREADY_DELETED.message()))
                .when(backendApiClient).delete(eq(ITEM_PATH), eq(4L));

        // 이미 원하는 상태라 사용자가 할 일이 없다.
        String html = mockMvc.perform(post("/expend-groups/4/delete")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_ALREADY_DELETED.message());
    }
}

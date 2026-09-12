package com.dbdomino.moneylog.front.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.member.MemberView;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 회원 정지.
 *
 * <p>되돌릴 수 없는 동작이라 두 가지가 걸려 있다 — <b>{@code POST} 로 나갈 것</b>과
 * <b>누르기 전에 되돌릴 수 없다는 사실을 보일 것</b>이다. 앞의 것을 어기면 브라우저가 미리
 * 불러오는 것만으로 계정이 정지되고, 뒤의 것을 어기면 관리자는 언제든 풀 수 있다고 믿고
 * 누른다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberDeactivateTest {

    private static final String LIST_URL = "/admin/members";
    private static final String DEACTIVATE_PATH = "/admin/members/{memberId}/deactivate";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "admin", SessionUser.ROLE_ADMIN, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(AdminMemberListTest.page());
    }

    @Test
    @DisplayName("정지는 POST 로 나가고 본문을 싣지 않는다")
    void 정지는_POST_로_본문_없이_나간다() throws Exception {
        when(backendApiClient.patch(eq(DEACTIVATE_PATH), isNull(), eq(MemberView.class), eq("hong")))
                .thenReturn(new MemberView("hong", "홍길동", null, null, null,
                        SessionUser.ROLE_MEMBER, false));

        mockMvc.perform(post("/admin/members/hong/deactivate").session(LoggedInSessions.admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"));

        // 백엔드 정지는 본문 없는 수정 요청이다.
        verify(backendApiClient).patch(eq(DEACTIVATE_PATH), isNull(), eq(MemberView.class),
                eq("hong"));
    }

    @Test
    @DisplayName("확인 본문에 되돌릴 수 없다는 것과 재로그인 불가가 적혀 있다")
    void 확인_본문이_되돌릴_수_없음을_알린다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("확인").contains("정지");
        assertThat(html)
                .as("화면에서 정지를 푸는 길이 없다는 사실은 누르기 전에 보여야 한다")
                .contains("되돌릴 수 없습니다");
        assertThat(html)
                .as("로그아웃은 다시 로그인할 수 있지만 정지는 그렇지 않다")
                .contains("다시 로그인할 수 없습니다");
        // 되돌릴 수 없는 동작을 링크로 두면 브라우저가 미리 불러오는 것만으로 실행된다.
        assertThat(html).contains("/admin/members/hong/deactivate");
    }

    @Test
    @DisplayName("대상이 본인이면 그 사실이 확인 본문에 적힌다")
    void 본인이면_그_사실을_알린다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_ADMIN, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(new AdminMemberListResult(
                        List.of(new MemberView("hong", "홍길동", null, null, null,
                                SessionUser.ROLE_ADMIN, true)), 0, 10, 1));

        String html = mockMvc.perform(get(LIST_URL)
                        .session(LoggedInSessions.of("hong", SessionUser.ROLE_ADMIN)))
                .andReturn().getResponse().getContentAsString();

        // 관리자가 자기를 정지하면 다음 요청부터 자기도 막힌다.
        assertThat(html).contains("본인 계정입니다");
    }

    @Test
    @DisplayName("이미 정지된 회원이면 오류 화면이 아니라 목록의 안내로 보인다")
    void 이미_정지면_목록에서_알린다() throws Exception {
        when(backendApiClient.patch(eq(DEACTIVATE_PATH), isNull(), eq(MemberView.class), eq("parks")))
                .thenThrow(new BackendApiException(ErrorCode.BAD_REQUEST.code(),
                        "이미 정지된 회원입니다."));

        String html = mockMvc.perform(post("/admin/members/parks/deactivate")
                        .session(LoggedInSessions.admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andReturn().getResponse().getContentAsString();

        // 이미 원하는 상태라 관리자가 할 일이 없는데 오류 화면으로 보내면 걸음만 늘어난다.
        assertThat(html).contains("이미 정지된 회원입니다.");
        assertThat(html).contains("홍길동");
    }

    @Test
    @DisplayName("정지된 회원에게는 정지 버튼을 그리지 않는다")
    void 정지된_회원에게는_버튼이_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andReturn().getResponse().getContentAsString();

        // 목록에는 활성 hong 과 정지 parks 가 있다. parks 에게는 확인 다이얼로그도 없다.
        assertThat(html).contains("confirm-deactivate-hong");
        assertThat(html).doesNotContain("confirm-deactivate-parks");
    }
}

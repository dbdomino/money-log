package com.dbdomino.moneylog.front.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.member.MemberView;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.util.List;
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
 * 1.8 회원 목록.
 *
 * <p>검색이 두 칸인 것이 이 화면의 핵심이다. 한 값을 두 조건에 똑같이 실으면 "아이디에도 있고
 * 닉네임에도 있는" 회원만 걸려 사용자 기대와 정반대로 동작하는데, 그 오작동은 <b>결과가 비는
 * 형태</b>로 나타나 원인을 짐작하기 어렵다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberListTest {

    private static final String LIST_URL = "/admin/members";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "admin", SessionUser.ROLE_ADMIN, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(page());
    }

    @Test
    @DisplayName("관리자로 열면 행과 쪽 정보가 그려진다")
    void 목록이_그려진다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("hong").contains("홍길동").contains("hong@example.com");
        // 권한과 상태는 숫자 대신 말로 보인다.
        assertThat(html).contains("일반").contains("활성").contains("정지");
        assertThat(html).contains("전체");
    }

    @Test
    @DisplayName("아이디·닉네임 검색어가 백엔드 요청에 각각 실린다")
    void 검색어가_각각_실린다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin())
                        .param("memberId", "hong").param("nickname", "홍"))
                .andExpect(status().isOk());

        Map<String, Object> query = capturedQuery();
        assertThat(query).containsEntry("memberId", "hong");
        assertThat(query).containsEntry("nickname", "홍");
    }

    @Test
    @DisplayName("검색 폼에 쪽 번호가 없어 새 검색은 첫 쪽에서 시작한다")
    void 검색_폼에_쪽_번호가_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andReturn().getResponse().getContentAsString();

        // 싣고 다니면 3쪽에서 검색했을 때 결과가 한 쪽뿐인데도 3쪽을 요구해 빈 화면이 뜬다.
        int formStart = html.indexOf("<form class=\"page-toolbar\"");
        int formEnd = html.indexOf("</form>", formStart);
        assertThat(html.substring(formStart, formEnd)).doesNotContain("name=\"page\"");
    }

    @Test
    @DisplayName("쪽 이동 링크에 검색어 둘이 함께 실린다")
    void 쪽_이동이_검색어를_들고_간다() throws Exception {
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(new AdminMemberListResult(page().rows(), 0, 10, 35));

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin())
                        .param("memberId", "hong").param("nickname", "홍"))
                .andReturn().getResponse().getContentAsString();

        // 빠뜨리면 쪽을 넘기는 순간 검색이 풀려 전체 목록으로 돌아간다.
        int pagination = html.indexOf("class=\"pagination\"");
        String block = html.substring(pagination);
        assertThat(block).contains("page=1");
        assertThat(block).contains("memberId=hong");
        assertThat(block).contains("nickname=");
    }

    @Test
    @DisplayName("전체 쪽 수는 전체 건수로 환산한다")
    void 전체_쪽_수가_전체_건수로_환산된다() throws Exception {
        // 현재 쪽의 행 수로 계산하면 마지막 쪽에서 쪽 수가 줄어든다.
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(new AdminMemberListResult(page().rows(), 0, 10, 35));

        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .model().attribute("totalPages", 4));
    }

    @Test
    @DisplayName("응답 HTML 에 없는 열과 없는 버튼이 그려지지 않는다")
    void 없는_값을_그리지_않는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()))
                .andReturn().getResponse().getContentAsString();

        // 백엔드 목록 응답에 그 값이 없고, 정지를 되돌리는 API 도 없다.
        assertThat(html).doesNotContain("가입일");
        assertThat(html).doesNotContain(">해제<");
    }

    @Test
    @DisplayName("일반 권한으로 열면 권한 없음 화면으로 간다")
    void 일반_권한은_막힌다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        // 판정은 007 이 한다. 008 이 다시 판정하지 않는다.
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/error/forbidden"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedQuery() {
        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq(LIST_URL), query.capture(),
                eq(AdminMemberListResult.class));
        return query.getValue();
    }

    static AdminMemberListResult page() {
        List<MemberView> rows = List.of(
                new MemberView("hong", "홍길동", "hong@example.com", "01012345678", null,
                        SessionUser.ROLE_MEMBER, true),
                new MemberView("parks", "박성민", null, null, null,
                        SessionUser.ROLE_MEMBER, false));
        return new AdminMemberListResult(rows, 0, 10, 2);
    }
}

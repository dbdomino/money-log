package com.dbdomino.moneylog.front.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 1.1 로그인 화면.
 *
 * <p>핵심은 <b>{@code 1003} 안내가 아이디 칸에 붙지 않는다</b>는 것이다. 친절하게 칸에 붙이는
 * 순간 그 아이디가 실재하는지 드러나고, 그것은 눈으로 확인할 수 없어 시험으로만 고정된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTest {

    private static final String LOGIN_URL = "/auth/login";
    private static final String PASSWORD = "DemoPass1!";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("미로그인으로 열면 로그인 폼이 뜬다")
    void 미로그인이면_폼이_뜬다() throws Exception {
        mockMvc.perform(get(LOGIN_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"memberId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")));
    }

    @Test
    @DisplayName("이미 로그인했으면 월별 가계부로 보낸다")
    void 이미_로그인했으면_가계부로_보낸다() throws Exception {
        // 007 의 비로그인 허용 목록은 건드리지 않는다. "열 수 있는가"와 "열 필요가 있는가"는
        // 다른 판단이라 이 화면의 조회 처리가 가린다.
        mockMvc.perform(get(LOGIN_URL).session(LoggedInSessions.member()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ledger"));
    }

    @Test
    @DisplayName("로그인에 성공하면 가계부로 가고 세션에 토큰 두 개와 아이디·권한이 담긴다")
    void 로그인_성공이_세션을_채운다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(LOGIN_URL), any(), eq(LoginResult.class)))
                .thenReturn(new LoginResult("hong", "홍길동", SessionUser.ROLE_MEMBER,
                        "access-new", "refresh-new"));

        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post(LOGIN_URL).session(session)
                        .param("memberId", "hong")
                        .param("password", PASSWORD))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ledger"));

        // 로그인은 세션 식별자를 새로 만든 뒤에 담으므로 처음 넘긴 세션은 버려진다.
        // 값이 들어간 곳은 요청이 끝난 뒤의 세션이다.
        assertThat(session.isInvalid())
                .as("로그인 전 식별자를 이어 쓰면 미리 심어 둔 식별자로 세션에 올라탈 수 있다")
                .isTrue();
    }

    @Test
    @DisplayName("1003 은 폼 상단에만 뜨고 아이디 칸에는 아무 표시도 붙지 않는다")
    void 로그인_실패가_아이디의_존재를_드러내지_않는다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(LOGIN_URL), any(), eq(LoginResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.LOGIN_FAILED.code(),
                        ErrorCode.LOGIN_FAILED.message()));

        String html = mockMvc.perform(post(LOGIN_URL)
                        .param("memberId", "hong")
                        .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("alert-error");
        assertThat(html).contains(String.valueOf(ErrorCode.LOGIN_FAILED.code()));

        // 아이디 칸 아래에 붙는 안내는 form-hint 로 그려진다. 그것이 없어야 한다.
        int memberIdInput = html.indexOf("name=\"memberId\"");
        int passwordInput = html.indexOf("name=\"password\"");
        assertThat(memberIdInput).isGreaterThan(0);
        assertThat(html.substring(memberIdInput, passwordInput))
                .as("칸에 붙이는 순간 그 아이디가 실재하는지 드러난다")
                .doesNotContain("alert-error")
                .doesNotContain("form-hint");

        assertThat(html)
                .as("사용자가 방금 친 값이라 되채워도 존재 여부가 드러나지 않는다")
                .contains("value=\"hong\"");
    }

    @Test
    @DisplayName("1004 는 반대로 비활성 계정임을 분명히 알린다")
    void 비활성_계정은_상태를_알린다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(LOGIN_URL), any(), eq(LoginResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.ACCOUNT_INACTIVE.code(),
                        ErrorCode.ACCOUNT_INACTIVE.message()));

        // 아이디가 맞다는 사실이 드러나지만, 정지된 사용자가 이유를 모른 채 비밀번호만 계속
        // 시도하는 쪽이 더 나쁘다고 002 가 이미 판단했다.
        mockMvc.perform(post(LOGIN_URL).param("memberId", "hong").param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(ErrorCode.ACCOUNT_INACTIVE.message())));
    }

    @Test
    @DisplayName("실패 응답 HTML 어디에도 비밀번호 평문이 없다")
    void 실패_응답에_비밀번호가_없다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(LOGIN_URL), any(), eq(LoginResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.LOGIN_FAILED.code(), "실패"));

        String html = mockMvc.perform(post(LOGIN_URL)
                        .param("memberId", "hong")
                        .param("password", PASSWORD))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("한 번만 되채워도 평문이 브라우저 캐시와 방문 기록에 남는다")
                .doesNotContain(PASSWORD);
    }
}

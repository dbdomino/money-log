package com.dbdomino.moneylog.front.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * 1.7 본인 정보 화면.
 *
 * <p>핵심은 <b>빈 칸의 뜻이 칸마다 다르다</b>는 것이다. 새 비밀번호를 비우면 요청에 싣지
 * 않고, 이메일을 비우면 비우라는 값으로 싣는다. 두 시험을 나누면 "빈 칸을 어떻게 보내는가"가
 * 칸마다 다르다는 사실이 시험에서도 흩어지므로 한 자리에 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberProfileControllerTest {

    private static final String PROFILE_URL = "/member/profile";
    private static final String ME_PATH = "/members/me";
    private static final String NEW_PASSWORD = "NewPass1!";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.get(eq(ME_PATH), eq(MemberView.class))).thenReturn(current());
    }

    @Test
    @DisplayName("열면 현재 값이 채워지고 아이디 칸은 읽기 전용이다")
    void 현재_값이_채워지고_아이디는_읽기_전용이다() throws Exception {
        String html = mockMvc.perform(get(PROFILE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("member/profile"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("홍길동");
        assertThat(html).contains("hong@example.com");
        assertThat(html).contains("01012345678");

        // 백엔드 수정 API 에 아이디를 바꾸는 칸 자체가 없다. 화면은 그 사실을 보여 줄 뿐이다.
        int memberIdInput = html.indexOf("id=\"memberId\"");
        int nicknameInput = html.indexOf("id=\"nickname\"");
        assertThat(html.substring(memberIdInput, nicknameInput)).contains("readonly");
    }

    @Test
    @DisplayName("새 비밀번호 칸은 언제나 빈 칸이다")
    void 새_비밀번호는_언제나_빈_칸이다() throws Exception {
        String html = mockMvc.perform(get(PROFILE_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 비어 있는 것이 정상 상태라는 것을 사용자가 알아야 한다.
        int passwordInput = html.indexOf("id=\"newPassword\"");
        int buttonStart = html.indexOf("<button", passwordInput);
        assertThat(html.substring(passwordInput, buttonStart)).doesNotContain("value=");
        assertThat(html).contains("변경할 때만 입력");
    }

    @Test
    @DisplayName("새 비밀번호를 비우면 요청에 비밀번호가 실리지 않고, 이메일을 비우면 비우라는 값이 실린다")
    void 빈_칸의_뜻이_칸마다_다르다() throws Exception {
        when(backendApiClient.patch(eq(ME_PATH), any(), eq(MemberView.class)))
                .thenReturn(current());

        mockMvc.perform(post(PROFILE_URL).session(LoggedInSessions.member())
                        .param("nickname", "새닉네임")
                        .param("email", "")
                        .param("phone", "01012345678")
                        .param("intro", "소개")
                        .param("newPassword", ""))
                .andExpect(status().isOk());

        Map<String, Object> body = capturedPatchBody();

        assertThat(body)
                .as("그 칸만 현재 값이 채워지지 않은 채 뜨므로 비어 있는 것이 기본 상태다")
                .doesNotContainKey("password");
        assertThat(body)
                .as("현재 값이 채워진 채로 떴으니 사용자가 지웠다면 지우려는 뜻이다")
                .containsEntry("email", null);
        assertThat(body).containsEntry("nickname", "새닉네임");
    }

    @Test
    @DisplayName("새 비밀번호를 넣으면 요청에 실린다")
    void 새_비밀번호를_넣으면_실린다() throws Exception {
        when(backendApiClient.patch(eq(ME_PATH), any(), eq(MemberView.class)))
                .thenReturn(current());

        mockMvc.perform(post(PROFILE_URL).session(LoggedInSessions.member())
                        .param("nickname", "홍길동")
                        .param("newPassword", NEW_PASSWORD))
                .andExpect(status().isOk());

        assertThat(capturedPatchBody()).containsEntry("password", NEW_PASSWORD);
    }

    @Test
    @DisplayName("하이픈을 넣은 폰이 백엔드로는 숫자만 나간다")
    void 폰은_숫자만_나간다() throws Exception {
        when(backendApiClient.patch(eq(ME_PATH), any(), eq(MemberView.class)))
                .thenReturn(current());

        mockMvc.perform(post(PROFILE_URL).session(LoggedInSessions.member())
                        .param("nickname", "홍길동")
                        .param("phone", "010-1234-5678"))
                .andExpect(status().isOk());

        assertThat(capturedPatchBody()).containsEntry("phone", "01012345678");
    }

    @Test
    @DisplayName("2003 은 이메일 칸 가까이 붙는다")
    void 이메일_중복이_칸_가까이_붙는다() throws Exception {
        when(backendApiClient.patch(eq(ME_PATH), any(), eq(MemberView.class)))
                .thenThrow(new BackendApiException(ErrorCode.EMAIL_DUPLICATED.code(),
                        ErrorCode.EMAIL_DUPLICATED.message()));

        String html = mockMvc.perform(post(PROFILE_URL).session(LoggedInSessions.member())
                        .param("nickname", "홍길동")
                        .param("email", "taken@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("member/profile"))
                .andReturn().getResponse().getContentAsString();

        int emailInput = html.indexOf("id=\"email\"");
        int phoneInput = html.indexOf("id=\"phone\"");
        assertThat(html.substring(emailInput, phoneInput))
                .contains(ErrorCode.EMAIL_DUPLICATED.message());

        assertThat(html)
                .as("되받은 값으로 덮으면 사용자가 방금 친 것이 사라진다")
                .contains("taken@example.com");
    }

    @Test
    @DisplayName("실패 응답 HTML 어디에도 비밀번호 평문이 없다")
    void 실패_응답에_비밀번호가_없다() throws Exception {
        when(backendApiClient.patch(eq(ME_PATH), any(), eq(MemberView.class)))
                .thenThrow(new BackendApiException(ErrorCode.PASSWORD_RULE_VIOLATION.code(),
                        ErrorCode.PASSWORD_RULE_VIOLATION.message()));

        String html = mockMvc.perform(post(PROFILE_URL).session(LoggedInSessions.member())
                        .param("nickname", "홍길동")
                        .param("newPassword", NEW_PASSWORD))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain(NEW_PASSWORD);
    }

    @Test
    @DisplayName("로그인 후 껍데기가 상단바 로그아웃 폼을 들고 있다")
    void 상단바에_로그아웃이_있다() throws Exception {
        // 008 이 만드는 것이 아니라 007 의 껍데기가 주는 것이다. 로그인 후 화면이 처음
        // 생긴 지금에서야 그 버튼이 실제로 그려지는지 확인할 수 있다(FR-708).
        String html = mockMvc.perform(get(PROFILE_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("action=\"/auth/logout\"");
        assertThat(html).contains("method=\"post\"");
    }

    @Test
    @DisplayName("미로그인으로 열면 로그인 화면으로 간다")
    void 미로그인은_로그인_화면으로_간다() throws Exception {
        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isFound());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPatchBody() {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).patch(eq(ME_PATH), body.capture(), eq(MemberView.class));
        return (Map<String, Object>) body.getValue();
    }

    private static MemberView current() {
        return new MemberView("hong", "홍길동", "hong@example.com", "01012345678", null,
                SessionUser.ROLE_MEMBER, null);
    }
}

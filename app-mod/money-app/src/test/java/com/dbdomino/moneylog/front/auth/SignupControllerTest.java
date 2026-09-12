package com.dbdomino.moneylog.front.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.auth.form.SignupForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 1.2 회원가입 화면.
 *
 * <p>실패로 폼을 다시 그릴 때 <b>비밀번호 두 칸만 비우고 나머지는 남는</b> 것이 이 화면의
 * 약속이다. 전부 날리면 사용자가 일곱 칸을 다시 치고, 전부 남기면 평문이 HTML 에 실린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SignupControllerTest {

    private static final String SIGNUP_URL = "/auth/signup";
    private static final String PASSWORD = "TempPass1!";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("폼과 비밀번호 규칙 문구가 함께 뜬다")
    void 규칙_문구가_입력_전부터_보인다() throws Exception {
        // 다 채운 뒤에 규칙을 알려 주면 사용자가 처음부터 다시 친다.
        mockMvc.perform(get(SIGNUP_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "8자 이상, 영어 대·소문자·특수문자·숫자 중 3종류 이상")));
    }

    @Test
    @DisplayName("이미 로그인했으면 월별 가계부로 보낸다")
    void 이미_로그인했으면_가계부로_보낸다() throws Exception {
        mockMvc.perform(get(SIGNUP_URL).session(LoggedInSessions.member()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ledger"));
    }

    @Test
    @DisplayName("가입에 성공하면 로그인 화면으로 가고 방금 만든 아이디가 함께 넘어간다")
    void 가입_성공이_아이디를_넘긴다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(SIGNUP_URL), any(), eq(SignupResult.class)))
                .thenReturn(new SignupResult("leeyj", "이윤주", SessionUser.ROLE_MEMBER));

        // 주소가 아니라 flash 로 넘긴다 — 주소에 실으면 아이디가 주소창과 방문 기록에 남는다.
        mockMvc.perform(post(SIGNUP_URL)
                        .param("memberId", "leeyj")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD)
                        .param("nickname", "이윤주"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"))
                .andExpect(flash().attribute("memberId", "leeyj"));
    }

    @Test
    @DisplayName("2002 는 아이디 칸에 붙고 닉네임·이메일 입력이 남아 있다")
    void 아이디_중복이_칸에_붙고_입력이_남는다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(SIGNUP_URL), any(), eq(SignupResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.MEMBER_ID_DUPLICATED.code(),
                        ErrorCode.MEMBER_ID_DUPLICATED.message()));

        String html = mockMvc.perform(post(SIGNUP_URL)
                        .param("memberId", "leeyj")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD)
                        .param("nickname", "이윤주")
                        .param("email", "leeyj@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.MEMBER_ID_DUPLICATED.message());

        // 안내가 아이디 칸과 비밀번호 칸 사이에 있어야 그 칸에 붙은 것이다.
        int memberIdInput = html.indexOf("name=\"memberId\"");
        int passwordInput = html.indexOf("name=\"password\"");
        assertThat(html.substring(memberIdInput, passwordInput))
                .contains(ErrorCode.MEMBER_ID_DUPLICATED.message());

        assertThat(html)
                .as("오류 화면으로 튕기면 사용자가 채운 일곱 칸이 전부 사라진다")
                .contains("value=\"이윤주\"")
                .contains("value=\"leeyj@example.com\"");
    }

    @Test
    @DisplayName("2005 는 비밀번호 확인 칸에 붙고 비밀번호 두 칸만 비어 있다")
    void 확인_불일치는_비밀번호만_비운다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(SIGNUP_URL), any(), eq(SignupResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.PASSWORD_CONFIRM_MISMATCH.code(),
                        ErrorCode.PASSWORD_CONFIRM_MISMATCH.message()));

        String html = mockMvc.perform(post(SIGNUP_URL)
                        .param("memberId", "leeyj")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", "other")
                        .param("nickname", "이윤주"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int confirmInput = html.indexOf("name=\"passwordConfirm\"");
        int nicknameInput = html.indexOf("name=\"nickname\"");
        assertThat(html.substring(confirmInput, nicknameInput))
                .contains(ErrorCode.PASSWORD_CONFIRM_MISMATCH.message());

        assertThat(html).doesNotContain(PASSWORD);
        assertThat(html)
                .as("아이디·닉네임은 남고 비밀번호 두 칸만 비운다")
                .contains("value=\"leeyj\"")
                .contains("value=\"이윤주\"");
    }

    @Test
    @DisplayName("하이픈을 넣은 폰이 백엔드로는 숫자만 나간다")
    void 폰은_숫자만_나간다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(SIGNUP_URL), any(), eq(SignupResult.class)))
                .thenReturn(new SignupResult("leeyj", "이윤주", SessionUser.ROLE_MEMBER));

        mockMvc.perform(post(SIGNUP_URL)
                        .param("memberId", "leeyj")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD)
                        .param("nickname", "이윤주")
                        .param("phone", "010-9876-5432"))
                .andExpect(status().isFound());

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).postWithoutAuth(eq(SIGNUP_URL), body.capture(),
                eq(SignupResult.class));

        SignupForm.Request request = (SignupForm.Request) body.getValue();
        assertThat(request.phone())
                .as("브라우저 스크립트가 막힌 환경에서도 저장값이 같아야 한다")
                .isEqualTo("01098765432");
    }
}

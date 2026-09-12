package com.dbdomino.moneylog.front.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.auth.form.ResetPasswordForm;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 1.4 비밀번호 찾기와 1.5 비밀번호 변경을 한 시험에서 잇는다.
 *
 * <p>두 화면을 나눠 시험하면 <b>표식이 두 화면 사이에서 살아 있는가</b>를 아무도 보지 않게
 * 된다. 그것이 이 흐름의 전부다.
 *
 * <p>핵심은 둘이다. 표식 없이 1.5 에 들어오면 폼을 그리지 않는 것 — 그리면 사용자가 비밀번호를
 * 고른 뒤에 처음부터 다시 하라는 말을 듣는다. 그리고 저장에 실패해도 표식이 살아 있는 것 —
 * 지우면 규칙에 한 번 어긋난 사용자가 1.4 부터 다시 해야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetFlowTest {

    private static final String FIND_URL = "/auth/find-password";
    private static final String RESET_URL = "/auth/reset-password";
    private static final String NEW_PASSWORD = "NewPass1!";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("1.4 확인에 성공하면 1.5 로 가고 표식이 담긴다")
    void 확인_성공이_표식을_담고_보낸다() throws Exception {
        matchConfirmed();
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post(FIND_URL).session(session)
                        .param("memberId", "hong").param("nickname", "홍길동"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", RESET_URL));

        assertThat(session.getAttribute("resetMemberId")).isEqualTo("hong");
        assertThat(session.getAttribute("resetNickname")).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("1.5 는 아이디·닉네임을 다시 묻지 않는다")
    void 아이디를_다시_묻지_않는다() throws Exception {
        MockHttpSession session = markedSession();

        String html = mockMvc.perform(get(RESET_URL).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andReturn().getResponse().getContentAsString();

        // 확인된 아이디는 보이되(누구의 비밀번호를 바꾸는지 알아야 한다) 입력 칸은 없다.
        assertThat(html).contains("hong");
        assertThat(html).contains("name=\"memberId\"");
        assertThat(html)
                .as("사용자가 채우는 칸은 새 비밀번호 둘뿐이다")
                .doesNotContain("id=\"memberId\"")
                .doesNotContain("id=\"nickname\"");
    }

    @Test
    @DisplayName("표식 없이 1.5 에 들어오면 폼이 아니라 1.4 로 안내한다")
    void 표식이_없으면_찾기로_보낸다() throws Exception {
        // 폼을 그려 두고 저장에서 실패시키면 사용자는 비밀번호를 다 고른 뒤에
        // 처음부터 다시 하라는 말을 듣는다.
        mockMvc.perform(get(RESET_URL))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", FIND_URL));
    }

    @Test
    @DisplayName("2004 로 실패해도 표식이 살아 있어 곧바로 다시 시도할 수 있다")
    void 실패해도_표식이_남는다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(RESET_URL), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.PASSWORD_RULE_VIOLATION.code(),
                        ErrorCode.PASSWORD_RULE_VIOLATION.message()));

        MockHttpSession session = markedSession();

        String html = mockMvc.perform(post(RESET_URL).session(session)
                        .param("newPassword", "short")
                        .param("newPasswordConfirm", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andReturn().getResponse().getContentAsString();

        assertThat(session.getAttribute("resetMemberId"))
                .as("지우면 규칙에 한 번 어긋난 사용자가 1.4 부터 다시 해야 한다")
                .isEqualTo("hong");
        assertThat(html).contains(ErrorCode.PASSWORD_RULE_VIOLATION.message());
        assertThat(html).doesNotContain("비밀번호가 변경되었습니다");
    }

    @Test
    @DisplayName("저장에 성공하면 같은 화면에 완료가 보이고 표식이 사라진다")
    void 성공하면_완료를_보이고_표식을_지운다() throws Exception {
        MockHttpSession session = markedSession();

        String html = mockMvc.perform(post(RESET_URL).session(session)
                        .param("newPassword", NEW_PASSWORD)
                        .param("newPasswordConfirm", NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("비밀번호가 변경되었습니다");
        assertThat(html)
                .as("자동 이동이 막힌 환경에서 사용자가 갇히지 않게 버튼을 함께 둔다")
                .contains("로그인으로");
        assertThat(html).doesNotContain(NEW_PASSWORD);

        assertThat(session.getAttribute("resetMemberId"))
                .as("남겨 두면 뒤로 가기로 돌아와 다시 바꿀 수 있다")
                .isNull();

        // 표식의 두 값이 새 비밀번호와 함께 나간다.
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).postWithoutAuth(eq(RESET_URL), body.capture(), eq(Void.class));
        ResetPasswordForm.Request request = (ResetPasswordForm.Request) body.getValue();
        assertThat(request.memberId()).isEqualTo("hong");
        assertThat(request.nickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("1.4 의 2001 은 아이디와 닉네임 중 무엇이 틀렸는지 가르지 않는다")
    void 불일치가_어느_칸인지_가르지_않는다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(FIND_URL), any(), eq(FindPasswordResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.MEMBER_NOT_FOUND.code(),
                        ErrorCode.MEMBER_NOT_FOUND.message()));

        String html = mockMvc.perform(post(FIND_URL)
                        .param("memberId", "hong").param("nickname", "없는닉네임"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/find-password"))
                .andReturn().getResponse().getContentAsString();

        // 가르면 아이디만 바꿔 넣어 보며 실재하는 아이디를 추려 낼 수 있다.
        int memberIdInput = html.indexOf("name=\"memberId\"");
        int nicknameInput = html.indexOf("name=\"nickname\"");
        assertThat(html.substring(memberIdInput, nicknameInput))
                .doesNotContain(ErrorCode.MEMBER_NOT_FOUND.message());
        assertThat(html).contains("alert-error");
    }

    private void matchConfirmed() {
        when(backendApiClient.postWithoutAuth(eq(FIND_URL), any(), eq(FindPasswordResult.class)))
                .thenReturn(new FindPasswordResult(true, "hong"));
    }

    /** 1.4 를 밟은 뒤의 상태. 표식만 심어 1.5 를 곧바로 연다. */
    private static MockHttpSession markedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("resetMemberId", "hong");
        session.setAttribute("resetNickname", "홍길동");
        return session;
    }
}

package com.dbdomino.moneylog.front.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.member.MemberView;
import com.dbdomino.moneylog.front.session.SessionExpiredException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 세션이 끝났을 때 007 이 실어 보낸 안내 문구가 <b>로그인 화면까지 닿는지</b> 본다.
 *
 * <p>007 은 세션을 버리며 문구를 실어 로그인 화면으로 보내도록 만들어 두었지만, 그 시점에는
 * <b>로그인 화면이 없어</b> 경로를 끝까지 확인한 시험이 없었다. 008 이 그 화면을 만드는 첫
 * 기능이라 여기서 잇는다.
 *
 * <p>이 경로가 끊기면 다른 곳에서 로그인해 밀려난 사용자는 "가만히 있었는데 로그아웃됐다"로만
 * 보게 되고, 계정이 털렸다고 의심한다. 문구를 넘기는 것은 007 의 책임이므로 닿지 않으면
 * 화면이 맞추는 것이 아니라 007 의 전달 방식을 고쳐야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionExpiredNoticeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("세션이 끝나면 문구를 실어 로그인 화면으로 보낸다")
    void 세션_만료가_문구를_실어_보낸다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.get(eq("/members/me"), eq(MemberView.class)))
                .thenThrow(new SessionExpiredException(ErrorCode.SESSION_INVALID.code(),
                        ErrorCode.SESSION_INVALID.message()));

        mockMvc.perform(get("/member/profile").session(LoggedInSessions.member()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"))
                .andExpect(flash().attribute("message", ErrorCode.SESSION_INVALID.message()));
    }

    @Test
    @DisplayName("로그인 화면이 그 문구를 실제로 보여 준다")
    void 로그인_화면이_문구를_보여_준다() throws Exception {
        // 007 이 flash 로 넘긴 값은 모델에 들어온다. 화면이 그것을 그리는지가 이 경로의 끝이다.
        String html = mockMvc.perform(get("/auth/login")
                        .flashAttr("message", ErrorCode.SESSION_INVALID.message()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("사유가 없으면 사용자는 계정이 털렸다고 의심한다")
                .contains(ErrorCode.SESSION_INVALID.message());
    }

    @Test
    @DisplayName("평소 로그인 화면에는 그 안내가 뜨지 않는다")
    void 평소에는_안내가_없다() throws Exception {
        String html = mockMvc.perform(get("/auth/login"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain(ErrorCode.SESSION_INVALID.message());
        assertThat(html).doesNotContain("alert-info");
    }
}

package com.dbdomino.moneylog.front.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
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
 * 상단바 로그아웃(FR-708).
 *
 * <p><b>008 이 만들 것이 없어 시험으로만 고정한다.</b> 007 의 {@code layout/main.html} 이 이미
 * {@code POST /auth/logout} 폼을 들고 있고, 백엔드 토큰 비활성화와 세션 버리기의 순서도 007 이
 * 지킨다. 그 순서를 한 곳에서 지켜야 하므로 008 이 자기 버전을 만들지 않는다.
 *
 * <p>여기서는 <b>그 주소가 실제로 그렇게 도는지</b>만 확인한다. 버튼이 화면에 그려지는 것은
 * 로그인 후 껍데기를 쓰는 첫 화면인 1.7 본인 정보의 시험이 함께 본다 — 이 단계에는 로그인 후
 * 화면이 아직 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LogoutButtonTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("로그아웃은 POST 로 나가고 백엔드 토큰을 먼저 비활성화한 뒤 로그인 화면으로 간다")
    void 로그아웃이_007_의_주소로_간다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        mockMvc.perform(post("/auth/logout").session(LoggedInSessions.member()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"));

        // 세션을 먼저 비우면 비활성화 요청에 실을 토큰이 없다. 그 토큰은 만료 전까지 유효해서
        // 어딘가에 새어 있었다면 로그아웃한 뒤에도 쓸 수 있다.
        verify(backendApiClient).post(eq("/auth/revoke"), isNull(), eq(Void.class));
    }
}

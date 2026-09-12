package com.dbdomino.moneylog.front.web;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BinaryPayload;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * 진입 판정의 네 갈래를 고정한다.
 *
 * <p>가장 중요한 것은 <b>미로그인으로 관리자 주소에 들어왔을 때</b>다. 권한 없음 화면이
 * 아니라 로그인 화면으로 가야 한다 — 순서가 뒤집히면 그 주소가 실재한다는 사실이 드러난다.
 *
 * <p>로그인 화면과 월별 가계부는 008·010 이 만들 화면이라 지금은 없다. 그래서 착지 주소만
 * 확인하고 그 화면이 그려지는지는 보지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorTest {

    private static final String PROTECTED_URL = "/payments";
    private static final String ADMIN_URL = "/admin/members";
    private static final String ICON_URL = "/expend-groups/icons/1_2.png";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("세션이 비면 보호 주소는 로그인 화면으로 보낸다")
    void 미로그인은_로그인으로() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/auth/login"));
    }

    @Test
    @DisplayName("토큰이 살아 있으면 통과한다")
    void 유효한_세션은_통과한다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        // 통과하면 매핑된 화면이 없어 404 다. 판정에 걸렸다면 302 였을 것이다.
        mockMvc.perform(get(PROTECTED_URL).session(LoggedInSessions.member()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("일반 권한으로 관리자 주소에 들어가면 권한 없음으로 보낸다")
    void 일반_권한은_관리자_주소에서_막힌다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        mockMvc.perform(get(ADMIN_URL).session(LoggedInSessions.member()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/error/forbidden"));
    }

    @Test
    @DisplayName("미로그인으로 관리자 주소에 들어가면 권한 없음이 아니라 로그인으로 보낸다")
    void 미로그인은_관리자_주소에서도_로그인으로() throws Exception {
        mockMvc.perform(get(ADMIN_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/auth/login"))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.not("/error/forbidden")));

        // 세션이 비었으면 백엔드에 묻지 않는다. 인증 실패가 돌아올 것이 확정이라 왕복이 낭비다.
        verify(backendApiClient, never()).get(eq("/auth/validate"), eq(TokenValidateResult.class));
    }

    @Test
    @DisplayName("관리자는 관리자 주소를 통과한다")
    void 관리자는_통과한다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "admin", SessionUser.ROLE_ADMIN, 86_400));

        // 007 시점에는 이 주소에 화면이 없어 404 로 "판정을 통과했다"를 확인했다. 008 이
        // 회원 목록을 세운 뒤로는 그 화면이 실제로 그려지는 것이 통과의 증거다.
        mockMvc.perform(get(ADMIN_URL).session(LoggedInSessions.admin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("권한 없음 화면은 판정에 걸리지 않는다")
    void 권한_없음_화면은_누구나_들어온다() throws Exception {
        // 화면 자체는 US3 이 만든다. 여기서는 판정이 로그인으로 밀어내지 않는 것만 본다.
        mockMvc.perform(get("/error/forbidden"))
                .andExpect(status().is(org.hamcrest.Matchers.not(302)));

        verify(backendApiClient, never()).get(eq("/auth/validate"), eq(TokenValidateResult.class));
    }

    @Test
    @DisplayName("아이콘 프록시에는 토큰 검증이 나가지 않는다")
    void 아이콘_프록시는_검증을_부르지_않는다() throws Exception {
        when(backendApiClient.getBinary(IconProxyController.ICON_PATH, "1_2.png"))
                .thenReturn(new BinaryPayload(new byte[] {1}, "image/png", null));

        // 지출유형이 20개인 화면이면 아이콘 요청이 20건 나간다. 그때마다 검증이 따라붙으면
        // 화면 한 번에 백엔드 왕복이 40건이 된다.
        mockMvc.perform(get(ICON_URL)).andExpect(status().isOk());

        verify(backendApiClient, never()).get(eq("/auth/validate"), eq(TokenValidateResult.class));
    }

}

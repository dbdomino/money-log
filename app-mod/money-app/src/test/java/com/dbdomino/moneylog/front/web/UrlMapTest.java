package com.dbdomino.moneylog.front.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BinaryPayload;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 북마크와 옛 링크로 들어온 사용자가 지금의 화면에 닿는지 확인한다.
 *
 * <p>착지할 화면들은 008·010 이 만든다. 그래서 <b>보내는 곳만 확인하고</b> 그 화면이 그려지는지는
 * 보지 않는다 — 대상 화면이 없어도 이 규칙은 검증된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlMapTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
    }

    @Test
    @DisplayName("로그인한 사용자의 루트는 월별 가계부다")
    void 로그인하면_루트는_가계부다() throws Exception {
        mockMvc.perform(get("/").session(loggedIn()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ledger"));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자의 루트는 로그인 화면이다")
    void 미로그인_루트는_로그인이다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"));
    }

    @Test
    @DisplayName("옛 로그인 주소는 지금의 로그인 화면으로 간다")
    void 옛_로그인_주소는_로그인으로() throws Exception {
        mockMvc.perform(get("/mem/login"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/auth/login"));
    }

    @Test
    @DisplayName("옛 홈은 루트로 보내 분기를 다시 타게 한다")
    void 옛_홈은_루트로() throws Exception {
        mockMvc.perform(get("/mem/ind"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @DisplayName("구 수정 주소는 부모 목록 + 모달 딥링크로 간다")
    void 구_수정_주소는_부모와_딥링크로() throws Exception {
        mockMvc.perform(get("/payments/1/edit").session(loggedIn()))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/payments?m=edit&id=1"));
    }

    @Test
    @DisplayName("구 등록 주소는 식별자 없이 모달만 연다")
    void 구_등록_주소는_모달만_연다() throws Exception {
        mockMvc.perform(get("/ledger/expenses/new").session(loggedIn()))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/ledger?m=expense-create"));
    }

    @Test
    @DisplayName("고정지출 월별은 식별자 자리보다 먼저 잡힌다")
    void 고정지출_월별은_식별자보다_먼저다() throws Exception {
        mockMvc.perform(get("/fixed-expenses/monthly").session(loggedIn()))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/fixed-expenses?m=monthly"));
    }

    @Test
    @DisplayName("아이콘 주소가 상세 모달로 새지 않는다")
    void 아이콘_주소는_새지_않는다() throws Exception {
        when(backendApiClient.getBinary(IconProxyController.ICON_PATH, "1_2.png"))
                .thenReturn(new BinaryPayload(new byte[] {1}, "image/png", null));

        // 새면 증상이 "아이콘이 하나도 안 보인다"라 원인을 짐작하기 어렵다.
        mockMvc.perform(get("/expend-groups/icons/1_2.png").session(loggedIn()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("미로그인으로 구 주소에 오면 로그인 판정이 먼저다")
    void 구_주소도_판정을_먼저_탄다() throws Exception {
        mockMvc.perform(get("/payments/new"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/auth/login"));
    }

    @Test
    @DisplayName("목록에 없는 주소는 보내지 않는다")
    void 목록에_없으면_보내지_않는다() throws Exception {
        mockMvc.perform(get("/payments/1/archive").session(loggedIn()))
                .andExpect(status().isNotFound());
    }

    private static MockHttpSession loggedIn() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("accessToken", "access-1");
        session.setAttribute("refreshToken", "refresh-1");
        session.setAttribute("memberId", "hong");
        session.setAttribute("role", SessionUser.ROLE_MEMBER);
        return session;
    }
}

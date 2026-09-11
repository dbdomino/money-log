package com.dbdomino.moneylog.front.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 008~012 가 얹힐 껍데기가 실제로 서는지 확인한다.
 *
 * <p>1.6 권한 없음에는 모달이 없어 딥링크를 걸 대상이 없다. 그래서 껍데기만 쓰는 시험 전용
 * 화면을 함께 둔다 — 운영 화면이 아니므로 시험 소스에만 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScreenShellTest {

    private static final String TEST_SCREEN = "/shelltest";

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
    @DisplayName("로그인 후 화면에는 사이드바가 있고 지금 메뉴가 활성으로 표시된다")
    void 사이드바가_활성_메뉴를_표시한다() throws Exception {
        mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"sidebar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is-active")));
    }

    @Test
    @DisplayName("일반 권한 응답에는 회원 관리 주소가 아예 들어 있지 않다")
    void 일반_권한에는_회원_관리가_없다() throws Exception {
        String html = mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("숨김 스타일로 가리면 소스에서 주소가 그대로 읽힌다")
                .doesNotContain("/admin/members");
    }

    @Test
    @DisplayName("관리자 응답에는 회원 관리가 들어 있다")
    void 관리자에게는_회원_관리가_보인다() throws Exception {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "admin", SessionUser.ROLE_ADMIN, 86_400));

        mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_ADMIN)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/admin/members")));
    }

    @Test
    @DisplayName("아는 모달 값은 모델에 담긴다")
    void 아는_모달_값은_모델에_담긴다() throws Exception {
        mockMvc.perform(get(TEST_SCREEN).param("m", "create")
                        .session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"));
    }

    @Test
    @DisplayName("모르는 모달 값은 오류가 아니라 부모 페이지다")
    void 모르는_모달_값은_부모_페이지다() throws Exception {
        mockMvc.perform(get(TEST_SCREEN).param("m", "nonsense")
                        .session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));
    }

    @Test
    @DisplayName("모달 껍데기가 닫기 네 길을 모두 갖춘다")
    void 모달_껍데기가_닫기를_갖춘다() throws Exception {
        String html = mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andReturn().getResponse().getContentAsString();

        // 취소·닫기 버튼은 표시로, Esc·딤 클릭은 모달 스크립트가 문서 전체에 건 처리로 닫는다.
        assertThat(html).contains("data-modal-close");
        assertThat(html).contains("modal-backdrop");
        assertThat(html).contains("/js/modal.js");
    }

    @Test
    @DisplayName("확인 다이얼로그를 공용으로 쓴다")
    void 확인_다이얼로그를_공용으로_쓴다() throws Exception {
        String html = mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("confirm-shelltest-delete");
        assertThat(html)
                .as("되돌릴 수 없는 범위는 부르는 화면이 문구로 넘긴다")
                .contains("지우면 되돌릴 수 없습니다.");
    }

    @Test
    @DisplayName("권한 없음 화면은 미로그인으로도 열린다")
    void 권한_없음_화면은_누구나_열린다() throws Exception {
        mockMvc.perform(get("/error/forbidden"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("권한 없음")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("layout-auth")));
    }

    @Test
    @DisplayName("정적 자원 셋이 레이아웃에서 한 번씩 걸린다")
    void 정적_자원이_걸린다() throws Exception {
        String html = mockMvc.perform(get(TEST_SCREEN).session(loggedIn(SessionUser.ROLE_MEMBER)))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/css/tokens.css");
        assertThat(html).contains("/css/ui.css");
        assertThat(html).contains("/js/modal.js");
    }

    /** 로그인 상태를 세션에 직접 심는다. 로그인 화면(008)이 없어도 껍데기를 검증할 수 있다. */
    private static MockHttpSession loggedIn(int role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("accessToken", "access-1");
        session.setAttribute("refreshToken", "refresh-1");
        session.setAttribute("memberId", role == SessionUser.ROLE_ADMIN ? "admin" : "hong");
        session.setAttribute("role", role);
        return session;
    }

    /**
     * 껍데기만 쓰는 시험 전용 화면. 008~012 의 화면이 이 모양으로 얹힌다.
     *
     * <p>운영 화면이 아니므로 {@code src/test} 에만 둔다.
     */
    @TestConfiguration
    static class ShellTestScreen {

        /** 이 화면이 여는 모달. 화면별 목록은 007 이 정하지 않고 각 화면이 낸다. */
        private static final Set<String> MODALS = Set.of("create", "edit", "detail");

        // 설정 클래스 안의 @Controller 중첩 클래스는 스프링이 스스로 등록한다.
        // @Bean 으로 한 번 더 올리면 같은 주소가 두 번 매핑돼 기동이 멈춘다.
        @Controller
        static class ShellTestController {

            @GetMapping(TEST_SCREEN)
            String screen(@RequestParam(name = "m", required = false) String modal, Model model) {
                model.addAttribute("activeMenu", "payments");
                model.addAttribute("modalMap",
                        "{\"create\":\"modal-shelltest-create\"}");
                ModalParam.applyTo(model, modal, MODALS);
                return "shelltest/modal-screen";
            }
        }
    }
}

package com.dbdomino.moneylog.front.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 세션 속성을 읽고 쓰는 자리가 한 곳이라는 것과, 그 자리가 토큰을 해석하지 않는다는 것을
 * 고정한다.
 */
class LoginSessionTest {

    /** 형태만 JWT 를 닮았고 안에 권한이 들어 있는 값. 화면 모듈이 이것을 읽지 않아야 한다. */
    private static final String TOKEN_LOOKING_LIKE_ADMIN =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOjF9.signature";

    private LoginSession loginSession;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        loginSession = new LoginSession();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("로그인하면 네 값이 담긴다")
    void 로그인하면_네_값이_담긴다() {
        loginSession.login("access-1", "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        assertThat(loginSession.accessToken()).isEqualTo("access-1");
        assertThat(loginSession.refreshToken()).isEqualTo("refresh-1");
        assertThat(loginSession.user()).isEqualTo(new SessionUser("hong", SessionUser.ROLE_MEMBER));
        assertThat(loginSession.isLoggedIn()).isTrue();
        assertThat(loginSession.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("로그인 전에는 아무것도 없고 관리자도 아니다")
    void 로그인_전에는_비어_있다() {
        assertThat(loginSession.accessToken()).isNull();
        assertThat(loginSession.user()).isNull();
        assertThat(loginSession.isLoggedIn()).isFalse();
        assertThat(loginSession.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("재발급은 토큰 두 개를 함께 갈아 끼운다")
    void 재발급은_토큰_둘을_바꾼다() {
        loginSession.login("access-1", "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        loginSession.replaceTokens("access-2", "refresh-2");

        assertThat(loginSession.accessToken()).isEqualTo("access-2");
        assertThat(loginSession.refreshToken())
                .as("접근 토큰만 갈면 폐기된 재발급 토큰이 남아 다음 재발급이 실패한다")
                .isEqualTo("refresh-2");
        assertThat(loginSession.user().memberId()).isEqualTo("hong");
    }

    @Test
    @DisplayName("세션을 버리면 아무것도 남지 않는다")
    void 세션을_버리면_비워진다() {
        loginSession.login("access-1", "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        loginSession.invalidate();

        assertThat(loginSession.accessToken()).isNull();
        assertThat(loginSession.user()).isNull();
        assertThat(loginSession.isLoggedIn()).isFalse();
    }

    @Test
    @DisplayName("토큰을 해석해 아이디와 권한을 꺼내지 않는다")
    void 토큰을_해석하지_않는다() {
        // 토큰 안에는 관리자라고 적혀 있지만, 담은 값은 일반 회원이다.
        loginSession.login(TOKEN_LOOKING_LIKE_ADMIN, "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        assertThat(loginSession.isAdmin())
                .as("토큰을 읽어 권한을 정하면 서명 검증 책임이 화면 모듈로 넘어온다")
                .isFalse();
        assertThat(loginSession.user().memberId()).isEqualTo("hong");
    }

    @Test
    @DisplayName("만료 시각을 담지 않는다")
    void 만료_시각을_담지_않는다() {
        boolean hasExpiry = Arrays.stream(LoginSession.class.getMethods())
                .map(Method::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.contains("expire"));

        assertThat(hasExpiry)
                .as("화면 모듈이 만료를 미리 판정하면 시계가 어긋났을 때 서버만 멀쩡한 상태가 된다")
                .isFalse();
    }

    @Test
    @DisplayName("권한이 바뀌면 검증 응답을 따라간다")
    void 권한은_검증_응답을_따라간다() {
        loginSession.login("access-1", "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        loginSession.refreshUser("hong", SessionUser.ROLE_ADMIN);

        assertThat(loginSession.isAdmin()).isTrue();
    }
}

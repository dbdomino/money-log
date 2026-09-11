package com.dbdomino.moneylog.front.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendClientFixture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 재발급이 <b>한 번뿐</b>이고, 새 재발급 토큰이 세션에 반영된다는 것을 고정한다.
 *
 * <p>호출 횟수를 가짜 서버의 기대 건수로 센다 — 기대하지 않은 요청이 한 건이라도 더 나가면
 * 시험이 깨진다.
 */
class TokenRefreshTest {

    private static final String BASE = BackendClientFixture.BASE_URL;
    private static final String TARGET = "/payment-methods";
    private static final String REFRESH = "/auth/refresh";

    private MockRestServiceServer server;
    private BackendApiClient client;
    private LoginSession loginSession;

    /** 백엔드가 돌려주는 값을 담을 최소 타입. */
    record Listing(int totalCount) {
    }

    @BeforeEach
    void setUp() {
        BackendClientFixture.bindRequest();
        loginSession = new LoginSession();
        loginSession.login("access-1", "refresh-1", "hong", SessionUser.ROLE_MEMBER);

        RestClient.Builder builder = BackendClientFixture.builder();
        server = BackendClientFixture.bindServer(builder);

        AtomicReference<TokenRefresher> holder = new AtomicReference<>();
        client = BackendClientFixture.client(builder, loginSession, holder::get);
        holder.set(new TokenRefresher(client, loginSession));
    }

    @AfterEach
    void tearDown() {
        BackendClientFixture.unbindRequest();
    }

    @Test
    @DisplayName("인증 만료면 한 번 재발급하고 원래 요청을 다시 보낸다")
    void 만료되면_재발급하고_다시_보낸다() {
        server.expect(requestTo(BASE + TARGET))
                .andExpect(header("Authorization", "Bearer access-1"))
                .andRespond(unauthorized());
        server.expect(requestTo(BASE + REFRESH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(refreshed("access-2", "refresh-2"));
        server.expect(requestTo(BASE + TARGET))
                .andExpect(header("Authorization", "Bearer access-2"))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"totalCount":3}}""", MediaType.APPLICATION_JSON));

        Listing listing = client.get(TARGET, Listing.class);

        assertThat(listing.totalCount()).isEqualTo(3);
        server.verify();
    }

    @Test
    @DisplayName("재발급이 준 새 재발급 토큰이 세션에 반영된다")
    void 새_재발급_토큰이_세션에_반영된다() {
        server.expect(requestTo(BASE + TARGET)).andRespond(unauthorized());
        server.expect(requestTo(BASE + REFRESH)).andRespond(refreshed("access-2", "refresh-2"));
        server.expect(requestTo(BASE + TARGET)).andRespond(withSuccess("""
                {"resCode":200,"data":{"totalCount":0}}""", MediaType.APPLICATION_JSON));

        client.get(TARGET, Listing.class);

        assertThat(loginSession.accessToken()).isEqualTo("access-2");
        assertThat(loginSession.refreshToken())
                .as("접근 토큰만 갈면 다음 재발급이 폐기된 토큰으로 나가 실패한다")
                .isEqualTo("refresh-2");
    }

    @Test
    @DisplayName("재발급 직후에도 만료면 다시 재발급하지 않는다")
    void 재발급은_한_번뿐이다() {
        server.expect(requestTo(BASE + TARGET)).andRespond(unauthorized());
        server.expect(requestTo(BASE + REFRESH)).andRespond(refreshed("access-2", "refresh-2"));
        server.expect(requestTo(BASE + TARGET)).andRespond(unauthorized());
        // 네 번째 요청은 기대하지 않는다. 나가면 가짜 서버가 시험을 깨뜨린다.

        assertThatThrownBy(() -> client.get(TARGET, Listing.class))
                .isInstanceOf(SessionExpiredException.class);

        assertThat(loginSession.isLoggedIn()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("세션 무효는 재발급을 아예 시도하지 않는다")
    void 세션_무효는_재발급하지_않는다() {
        server.expect(requestTo(BASE + TARGET)).andRespond(withSuccess("""
                {"resCode":1006,"data":{"message":"세션이 만료되었습니다. 다시 로그인해 주세요."}}""",
                MediaType.APPLICATION_JSON));
        // 재발급 요청을 기대하지 않는다. 백엔드가 세션을 이미 버렸으므로 받을 근거가 없다.

        assertThatThrownBy(() -> client.get(TARGET, Listing.class))
                .isInstanceOf(SessionExpiredException.class)
                .extracting(e -> ((SessionExpiredException) e).getResCode())
                .isEqualTo(1006);

        assertThat(loginSession.isLoggedIn()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("재발급 토큰이 만료되면 세션을 버린다")
    void 재발급_토큰_만료면_세션을_버린다() {
        server.expect(requestTo(BASE + TARGET)).andRespond(unauthorized());
        server.expect(requestTo(BASE + REFRESH)).andRespond(withSuccess("""
                {"resCode":1005,"data":{"message":"다시 로그인해 주세요."}}""",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.get(TARGET, Listing.class))
                .isInstanceOf(SessionExpiredException.class)
                .extracting(e -> ((SessionExpiredException) e).getResCode())
                .isEqualTo(1005);

        assertThat(loginSession.isLoggedIn()).isFalse();
        server.verify();
    }

    @Test
    @DisplayName("로그인 전 호출에는 인증도 재발급도 붙지 않는다")
    void 로그인_전_호출은_재발급을_타지_않는다() {
        loginSession.invalidate();
        BackendClientFixture.bindRequest();

        server.expect(requestTo(BASE + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("""
                        {"resCode":1003,"data":{"message":"아이디 또는 비밀번호가 올바르지 않습니다."}}""",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.postWithoutAuth("/auth/login",
                java.util.Map.of("memberId", "hong", "password", "x"), Listing.class))
                .isNotInstanceOf(SessionExpiredException.class);

        server.verify();
    }

    private static org.springframework.test.web.client.response.DefaultResponseCreator unauthorized() {
        return withSuccess("""
                {"resCode":1001,"data":{"message":"로그인이 필요합니다."}}""",
                MediaType.APPLICATION_JSON);
    }

    private static org.springframework.test.web.client.response.DefaultResponseCreator refreshed(
            String accessToken, String refreshToken) {
        // 백엔드는 만료 초와 토큰 종류도 함께 준다. 화면 모듈이 받지 않는 필드가 섞여 있어도
        // 읽히는지 이 응답이 함께 확인한다.
        return withSuccess("""
                {"resCode":200,"data":{"accessToken":"%s","tokenType":"Bearer","expiresIn":86400,\
                "refreshToken":"%s","refreshExpiresIn":604800}}"""
                .formatted(accessToken, refreshToken), MediaType.APPLICATION_JSON);
    }
}

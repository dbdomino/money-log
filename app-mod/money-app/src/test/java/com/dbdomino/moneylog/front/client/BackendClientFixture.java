package com.dbdomino.moneylog.front.client;

import com.dbdomino.moneylog.front.session.LoginSession;
import com.dbdomino.moneylog.front.session.TokenRefresher;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

/**
 * 백엔드를 띄우지 않고 클라이언트를 조립하는 시험 도구.
 *
 * <p>가짜 응답을 물린 빌더로 클라이언트를 만드는 것이 요점이다 — 클라이언트가 스스로 요청
 * 팩터리를 세우면 물린 것을 덮어써 기대한 응답이 한 건도 걸리지 않는다.
 */
public final class BackendClientFixture {

    public static final String BASE_URL = "http://backend.test/api/v1";

    private BackendClientFixture() {
    }

    /** 요청 밖에서도 세션을 다룰 수 있게 가짜 요청을 현재 요청으로 세운다. */
    public static MockHttpServletRequest bindRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    public static void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** 빌더와 가짜 서버를 한 쌍으로 만든다. */
    public static RestClient.Builder builder() {
        return RestClient.builder().baseUrl(BASE_URL);
    }

    public static MockRestServiceServer bindServer(RestClient.Builder builder) {
        return MockRestServiceServer.bindTo(builder).build();
    }

    public static BackendApiClient client(RestClient.Builder builder, LoginSession loginSession,
            Supplier<TokenRefresher> refresher) {
        return new BackendApiClient(builder.build(), JsonMapper.builder().build(), loginSession,
                provider(refresher));
    }

    /**
     * 재발급기를 나중에 넘겨 주는 공급자.
     *
     * <p>클라이언트와 재발급기가 서로를 필요로 하므로 조립 순서를 한쪽에서 늦춘다. 운영에서
     * 스프링이 하는 일을 시험에서 손으로 하는 것뿐이다.
     */
    private static ObjectProvider<TokenRefresher> provider(Supplier<TokenRefresher> supplier) {
        return new ObjectProvider<>() {
            @Override
            public TokenRefresher getObject() {
                return supplier.get();
            }

            @Override
            public TokenRefresher getIfAvailable() {
                return supplier.get();
            }
        };
    }
}

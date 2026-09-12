package com.dbdomino.moneylog.front.client;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 백엔드로 나가는 {@link RestClient} 를 구성한다.
 *
 * <p>기준 주소와 타임아웃을 여기서만 건다. {@link BackendApiClient} 는 이미 구성된 것을
 * 받아 쓰기만 하는데, 그래야 시험이 {@code MockRestServiceServer} 를 물린 빌더로 같은
 * 클라이언트를 만들 수 있다 — 클라이언트가 스스로 요청 팩터리를 세우면 시험이 물린 것을
 * 덮어써 가짜 응답이 한 건도 걸리지 않는다.
 *
 * <p><b>{@code JdkClientHttpRequestFactory} 를 쓰는 이유는 PATCH 다.</b> 기본값인
 * {@code SimpleClientHttpRequestFactory} 는 {@code HttpURLConnection} 을 쓰는데 그쪽이
 * PATCH 메서드를 거부한다. 이 프로젝트의 수정 API 는 전부 PATCH 라 기본값으로는 수정이
 * 하나도 나가지 않는다.
 */
@Configuration
public class BackendClientConfig {

    /**
     * 빌더를 직접 만든다. Boot 4 의 웹 스타터는 {@code RestClient.Builder} 를 자동으로
     * 올려 주지 않고, 그것을 받으려면 HTTP 클라이언트 스타터를 하나 더 붙여야 한다 — 007 은
     * 의존을 빼는 기능이라 늘리지 않는다. 빌더 자체는 spring-web 에 들어 있다.
     */
    @Bean
    public RestClient.Builder backendRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient backendRestClient(RestClient.Builder builder, BackendApiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}

package com.dbdomino.moneylog.front.client;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 백엔드 API 연동 설정. {@code application.yml} 의 {@code moneylog.backend.*} 를 받는다.
 *
 * <p><b>주소를 코드에 적지 않는다.</b> 배포 환경이 바뀌면 프로퍼티만 고친다. 코드에 박아
 * 두면 환경마다 빌드가 달라지고, 어느 빌드가 어디를 보는지 파일을 열어야 알 수 있다.
 *
 * <p>타임아웃에 기본값을 둔 이유는 빠뜨렸을 때 무한 대기가 되기 때문이다. 백엔드가 죽어
 * 있는데 화면이 응답 없이 멈춰 있으면 사용자는 자기 브라우저를 의심한다.
 */
@Component
@ConfigurationProperties(prefix = "moneylog.backend")
public class BackendApiProperties {

    /** 백엔드 API 기준 주소. 예: {@code http://localhost:8081/api/v1} */
    private String baseUrl;

    /** 연결 타임아웃. 초과하면 "서버에 닿지 못했습니다"로 화면에 표시한다. */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /** 읽기 타임아웃. 엑셀 일괄 등록(010)이 가장 오래 걸린다. */
    private Duration readTimeout = Duration.ofSeconds(10);

    @PostConstruct
    void validate() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "moneylog.backend.base-url 이 비어 있다. 화면 모듈은 이 주소로만 데이터를 얻는다.");
        }
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalStateException("moneylog.backend 타임아웃은 0보다 커야 한다.");
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}

package com.dbdomino.moneylog.backend.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정. {@code application.yml}의 {@code jwt.*}를 받는다.
 *
 * <p>{@code secret}에 기본값을 두지 않는다. 대체값을 두면 주입을 잊어도 기동이 성공해서
 * 알려진 키가 그대로 운영에 나간다.
 *
 * <p><b>키 길이를 기동 시점에 검증한다.</b> HS256 은 256비트(32바이트) 이상을 요구하고
 * 짧으면 jjwt 가 거부하는데, 그 실패를 첫 로그인 요청까지 미루면 "기동은 됐는데 로그인만
 * 안 되는" 상태가 된다. 여기서 막으면 배포 직후에 드러난다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md §6</a>
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HS256 최소 키 길이(바이트). */
    private static final int MIN_SECRET_BYTES = 32;

    /** HS256 서명키. 환경변수로 주입한다. */
    private String secret;

    /** Access Token 수명(초). 기본 1일. */
    private long accessTokenValiditySeconds = 86_400L;

    /** Refresh Token 수명(초). 기본 7일. */
    private long refreshTokenValiditySeconds = 604_800L;

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret 이 비어 있다. JWT_SECRET 환경변수를 주입해야 한다.");
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 이 짧다(" + length + "바이트). HS256 은 "
                            + MIN_SECRET_BYTES + "바이트 이상을 요구한다.");
        }
        if (accessTokenValiditySeconds <= 0 || refreshTokenValiditySeconds <= 0) {
            throw new IllegalStateException("jwt 토큰 수명은 0보다 커야 한다.");
        }
        if (refreshTokenValiditySeconds < accessTokenValiditySeconds) {
            throw new IllegalStateException(
                    "Refresh 수명이 Access 수명보다 짧다. 갱신할 수 없는 조합이다.");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }
}

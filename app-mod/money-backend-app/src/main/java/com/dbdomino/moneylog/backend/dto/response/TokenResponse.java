package com.dbdomino.moneylog.backend.dto.response;

import com.dbdomino.moneylog.backend.service.MemberSessionService.IssuedTokens;

/**
 * 1.5 토큰 갱신 응답. 1.3 로그인 응답도 같은 다섯 필드를 싣는다.
 *
 * <p>두 API 의 <b>필드명을 동일하게</b> 맞춘다. 갈리면 프론트가 발급 경로마다 분기해야
 * 한다(api-contract.md §3).
 *
 * @param accessToken      JWT
 * @param tokenType        {@code Bearer} 고정
 * @param expiresIn        Access 만료까지 남은 초. 설정값이 아니라 발급 시각 기준으로 센다
 * @param refreshToken     불투명 랜덤 문자열
 * @param refreshExpiresIn Refresh 만료까지 남은 초
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                            String refreshToken, long refreshExpiresIn) {

    /** 응답의 {@code tokenType} 은 항상 이 값이다. */
    public static final String BEARER = "Bearer";

    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(tokens.accessToken(), BEARER, tokens.accessExpiresInSeconds(),
                tokens.refreshToken(), tokens.refreshExpiresInSeconds());
    }
}

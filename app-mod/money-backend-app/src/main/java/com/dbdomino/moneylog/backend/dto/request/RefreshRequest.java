package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 1.5 토큰 갱신 요청.
 *
 * <p>누락은 {@code 9001}, 값이 있으나 유효하지 않으면 {@code 1005} 다(검증 1단계와
 * 2~5단계의 차이 — auth-pipeline.md §3).
 *
 * @param refreshToken 로그인 때 발급받은 Refresh Token 원문
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}

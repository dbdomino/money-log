package com.dbdomino.moneylog.front.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 백엔드 {@code MemberLogin} 성공 응답 중 <b>화면이 쓰는 것</b>.
 *
 * <p>토큰 두 개는 세션으로 곧장 들어가고 모델을 거치지 않는다. 만료까지 남은 초는 받지만
 * 담지 않는다 — 화면이 만료를 미리 판정하면 시계가 어긋났을 때 서버는 멀쩡하다는데 화면만
 * 만료라고 믿는 상태가 생긴다.
 *
 * @param memberId 로그인한 아이디
 * @param nickname 닉네임
 * @param role 권한. {@code 1} 관리자 · {@code 3} 일반
 * @param accessToken 접근 토큰
 * @param refreshToken 재발급 토큰
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResult(
        String memberId,
        String nickname,
        int role,
        String accessToken,
        String refreshToken) {
}

package com.dbdomino.moneylog.backend.dto.response;

/**
 * 1.4 토큰 검증 응답.
 *
 * <p>{@code valid} 는 <b>항상 {@code true}</b> 다. 유효하지 않으면 이 응답이 아니라
 * 실패 코드({@code 1001}·{@code 1004}·{@code 1006})가 나가기 때문이다.
 *
 * <p>닉네임·이메일 같은 프로필은 싣지 않는다 — 이 API 는 토큰·세션 유효성만 확인하고,
 * 프로필은 1.7 이 준다.
 *
 * @param valid     항상 {@code true}
 * @param memberId  토큰의 {@code sub}
 * @param role      {@code 1} 관리자, {@code 3} 일반
 * @param expiresIn Access 만료까지 남은 초
 */
public record TokenValidateResponse(boolean valid, String memberId, short role, long expiresIn) {

    public static TokenValidateResponse of(String memberId, short role, long expiresIn) {
        return new TokenValidateResponse(true, memberId, role, expiresIn);
    }
}

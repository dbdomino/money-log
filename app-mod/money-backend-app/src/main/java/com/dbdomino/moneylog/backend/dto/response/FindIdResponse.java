package com.dbdomino.moneylog.backend.dto.response;

/**
 * 1.9 아이디 찾기 응답.
 *
 * <p>원문 아이디를 그대로 주지 않는다. 이메일만 알면 남의 아이디를 알아낼 수 있게 되므로
 * 일부를 가린 값을 돌려주고, 가렸다는 사실을 {@code masked} 로 함께 알린다.
 *
 * @param memberId 일부를 가린 아이디. 예: {@code use***01}
 * @param masked   마스킹 적용 여부. 항상 {@code true} 다
 */
public record FindIdResponse(String memberId, boolean masked) {
}

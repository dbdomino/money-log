package com.dbdomino.moneylog.backend.dto.response;

/**
 * 1.10 비밀번호 찾기 응답.
 *
 * <p>{@code matched} 는 <b>항상 {@code true}</b> 다 — 불일치는 이 응답이 아니라
 * {@code 2001} 로 나간다. 화면이 "확인됐습니다"를 띄우고 재설정 입력으로 넘어가는 신호다.
 *
 * @param matched  항상 {@code true}
 * @param memberId 확인된 아이디. 화면 표시용이라 가린 값을 준다
 */
public record FindPasswordResponse(boolean matched, String memberId) {
}

package com.dbdomino.moneylog.backend.dto.request;

/**
 * 5.3 기본 목표 등록·수정의 Body.
 *
 * <p><b>5.4 와 필드 이름이 다르다.</b> 5.3 은 {@code defaultTargetAmount}, 5.4 는
 * {@code monthlyTargetAmount} 다(각 설계 명세의 Body 표). 두 층이 독립이라 요청에서도
 * 어느 층을 건드리는지가 이름으로 드러난다 — 같은 이름을 쓰면 URL 만으로 층을 구분해야 하고
 * 프론트가 경로를 잘못 짚었을 때 몸통이 그대로 통과한다.
 *
 * <p><b>{@code @NotNull} 을 쓰지 않는다.</b> 누락도 범위 오류도
 * {@link com.dbdomino.moneylog.backend.service.ExpendTargetFieldRules} 가 {@code 3602}
 * 로 낸다 — 5.3 의 실패 표에 {@code 9001} 이 없고, 005 의 금액 규칙과 같은 처방이다.
 *
 * <p>{@code Long} 인 이유는 누락({@code null})과 {@code 0} 을 가르기 위해서다.
 * {@code long} 이면 빠뜨린 요청이 <b>0원 목표 저장</b>으로 조용히 성공한다 —
 * 0 은 이 기능에서 유효한 값이라 더 위험하다(FR-504).
 *
 * @param defaultTargetAmount 월 단위 기본 목표금액. {@code 0 ~ 100,000,000}
 */
public record ExpendTargetDefaultUpsertRequest(Long defaultTargetAmount) {
}

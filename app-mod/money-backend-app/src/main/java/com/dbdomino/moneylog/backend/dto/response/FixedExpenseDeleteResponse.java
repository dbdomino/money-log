package com.dbdomino.moneylog.backend.dto.response;

/**
 * 4.7 고정지출 삭제 결과.
 *
 * <p><b>{@code deleted} 플래그가 없다.</b> 003 의
 * {@link PaymentMethodDeleteResponse} 는 삭제 <b>표시</b>라 그 값을 실어야 의미가 있지만,
 * 고정지출은 <b>물리 삭제</b>여서 행 자체가 사라진다(FR-416). 항상 참인 값을 실으면
 * "false 일 수도 있다"는 오해를 준다.
 *
 * <p>그 고정지출의 월별 내역도 <b>지난 달 것까지 전부</b> 함께 사라진다
 * ({@code ON DELETE CASCADE}). 건수를 세어 돌려주지 않는 것은 설계 명세가 요구하지
 * 않아서다 — 필요해지면 그때 더한다.
 *
 * @param fixedExpenseId 지워진 설정의 PK
 * @param message        화면에 띄울 문구
 */
public record FixedExpenseDeleteResponse(Long fixedExpenseId, String message) {
}

package com.dbdomino.moneylog.backend.dto.response;

/**
 * 2.5 수단 삭제 응답.
 *
 * <p>삭제는 <b>표시</b>다(FR-206). 행은 남고 {@code deleted} 만 참이 되며, 관리 목록(2.2)에는
 * 계속 보이고 사용 중 목록(2.6)에서만 빠진다. 과거 지출·소득이 이 수단을 FK 로 참조하고
 * 있어 행을 지우면 그 기록이 함께 무너진다.
 *
 * @param paymentMethodId 삭제 표시된 수단의 대리키
 * @param deleted         항상 {@code true}
 * @param message         화면에 보여 줄 문구
 */
public record PaymentMethodDeleteResponse(Long paymentMethodId, boolean deleted, String message) {
}

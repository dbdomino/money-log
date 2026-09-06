package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.4 지출 삭제 응답.
 *
 * <p><b>{@code deleted} 필드가 없다.</b> 003 의 수단 삭제(2.5)에는 {@code deleted: true} 가
 * 있지만 그쪽은 <b>삭제 표시</b>라 그 값이 행의 상태를 뜻한다. 004 는 <b>물리 삭제</b>라
 * 행 자체가 사라지므로 실을 상태가 없다(data-model.md §0).
 *
 * <p>같은 이유로 "이미 삭제됨" 코드도 없다. 두 번째 요청은 행을 찾지 못해 {@code 3202} 다 —
 * 003 이 {@code 3004}·{@code 3108} 로 그것을 구분하는 것과 다른 점이다.
 *
 * @param expenseId 삭제된 지출의 대리키
 * @param message   화면에 보여 줄 문구
 */
public record ExpenseDeleteResponse(Long expenseId, String message) {
}

package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.10 소득 삭제 응답.
 *
 * <p>지출 삭제({@link ExpenseDeleteResponse})와 같이 <b>{@code deleted} 필드가 없다</b> —
 * 물리 삭제라 실을 상태가 없다(FR-308). 두 번째 요청은 행을 찾지 못해 {@code 3302} 다.
 *
 * @param incomeId 삭제된 소득의 대리키
 * @param message  화면에 보여 줄 문구
 */
public record IncomeDeleteResponse(Long incomeId, String message) {
}

package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.7 소득 등록 응답 — <b>생성된 PK 한 칸</b>이다.
 *
 * <p>지출 등록({@link ExpenseCreateResponse})과 같은 형태다. 상세는 3.8 로 조회한다.
 *
 * @param incomeId 생성된 소득 대리키({@code idx})
 */
public record IncomeCreateResponse(Long incomeId) {
}

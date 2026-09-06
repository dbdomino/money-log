package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.1 지출 등록 응답 — <b>생성된 PK 한 칸</b>이다.
 *
 * <p>등록이 전체 Expense 를 돌려주지 않는 것은 설계 명세 3.1 이 정한 형태다. 상세는
 * {@link ExpenseResponse} 를 쓰는 3.2 로 조회한다 — 방금 보낸 값을 그대로 되돌려 받는
 * 것보다 화면이 필요할 때 읽는 편이 낫다는 판단이다.
 *
 * <p>수정(3.3)은 반대로 <b>갱신된 Expense 전체</b>를 돌려준다. 스냅샷이 갱신됐는지를
 * 화면이 바로 확인해야 하기 때문이다.
 *
 * @param expenseId 생성된 지출 대리키({@code idx})
 */
public record ExpenseCreateResponse(Long expenseId) {
}

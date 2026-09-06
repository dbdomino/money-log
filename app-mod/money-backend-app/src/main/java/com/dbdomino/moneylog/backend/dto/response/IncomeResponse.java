package com.dbdomino.moneylog.backend.dto.response;

import java.time.LocalDate;

/**
 * 소득 1건. 상세 조회(3.8)와 수정(3.9)이 함께 쓴다.
 *
 * <p><b>지출({@link ExpenseResponse})에 있는 네 가지가 없다</b> — 장소·지출유형 참조·
 * 지출유형 이름 스냅샷·할부 3필드. {@code tbl_income} 에 그 컬럼이 <b>없기</b> 때문이며,
 * 비워 두는 것이 아니다(FR-306). 그래서 두 응답을 공통 상위 타입으로 묶지 않는다 —
 * 없는 필드를 {@code null} 로 채우면 "안 적었다"와 "그런 개념이 없다"가 섞인다.
 *
 * <p>참조 ID 와 이름 스냅샷을 둘 다 싣는 이유는 지출과 같다 — 앞은 "지금 이 수단은
 * 무엇인가", 뒤는 "<b>등록 당시</b> 뭐라고 불렸나"에 답한다(FR-303).
 *
 * @param incomeId          소득 대리키({@code idx})
 * @param paymentMethodId   수단의 현재 참조. {@code purpose=INCOME} 인 수단이다
 * @param paymentMethodName 수단 이름 스냅샷. 원본이 바뀌어도 이 값은 그대로다
 * @param amount            금액(원 단위 정수)
 * @param paymentDate       입금일. <b>DB 컬럼은 {@code payment_date} 지만 뜻은 입금일</b>
 *                          이며, 지출과 컬럼 이름을 맞춘 것이다(data-model.md §2)
 * @param content           내용. <b>{@code null} 일 수 있다</b>(FR-307)
 */
public record IncomeResponse(Long incomeId,
                             Long paymentMethodId, String paymentMethodName,
                             Long amount, LocalDate paymentDate, String content) {
}

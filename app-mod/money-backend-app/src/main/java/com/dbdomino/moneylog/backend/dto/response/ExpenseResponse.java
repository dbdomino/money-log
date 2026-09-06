package com.dbdomino.moneylog.backend.dto.response;

import java.time.LocalDate;

/**
 * 지출 1건. 상세 조회(3.2)와 수정(3.3)이 함께 쓴다.
 *
 * <h2>참조 ID 와 이름 스냅샷을 둘 다 싣는다</h2>
 *
 * <table border="1">
 *   <caption>같아 보이지만 다른 질문에 답한다(data-model.md §1)</caption>
 *   <tr><th>필드</th><th>답하는 질문</th></tr>
 *   <tr><td>{@code paymentMethodId}</td>
 *       <td>"<b>지금</b> 이 수단은 무엇인가" — 수정 화면이 원본을 찾고 통계가 수단별로 묶는다</td></tr>
 *   <tr><td>{@code paymentMethodName}</td>
 *       <td>"<b>등록 당시</b> 뭐라고 불렸나" — 과거 화면을 그때 모습으로 복원한다</td></tr>
 * </table>
 *
 * <p>수단 이름을 바꿔도 과거 지출은 옛 이름으로 남아야 하므로(FR-302) 둘 다 필요하다.
 * 스냅샷은 <b>참조가 실제로 바뀔 때만</b> 갱신된다(FR-304).
 *
 * <h2>할부 3필드는 일시불이면 {@code null} 이다</h2>
 *
 * <p>필드를 생략하지 않는다. 3.2 가 일시불과 할부 회차를 <b>같은 API 로</b> 읽으므로
 * (US3 시나리오 5) 응답 타입도 하나여야 하고, 화면은 {@code installmentIndex} 의 유무가
 * 아니라 값으로 분기한다.
 *
 * <p>Entity 를 그대로 내보내지 않는다(헌장 원칙 II) — 소유자 연관과 감사 컬럼이 API 로
 * 새어 나가지 않는다.
 *
 * @param expenseId           지출 대리키({@code idx})
 * @param paymentMethodId     수단의 현재 참조
 * @param paymentMethodName   수단 이름 스냅샷. 원본이 바뀌어도 이 값은 그대로다
 * @param expendGroupId       지출유형의 현재 참조
 * @param expendGroupName     지출유형 이름 스냅샷
 * @param amount              금액(원 단위 정수)
 * @param paymentDate         결제일
 * @param place               장소
 * @param content             내용
 * @param installmentGroupId  할부 그룹. 일시불이면 {@code null}
 * @param installmentIndex    할부 회차(1부터). 일시불이면 {@code null}
 * @param installmentTotal    총 할부 개월. 일시불이면 {@code null}
 */
public record ExpenseResponse(Long expenseId,
                              Long paymentMethodId, String paymentMethodName,
                              Long expendGroupId, String expendGroupName,
                              Long amount, LocalDate paymentDate,
                              String place, String content,
                              Long installmentGroupId, Integer installmentIndex,
                              Integer installmentTotal) {
}

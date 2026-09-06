package com.dbdomino.moneylog.backend.dto.response;

/**
 * 고정지출 설정 1건 — 4.1·4.2(목록 요소)·4.3·4.4 가 함께 쓴다.
 *
 * <h2>이름 두 개는 스냅샷이 아니다</h2>
 *
 * <p>{@code paymentMethodName}·{@code expendGroupName} 은 <b>조회 시점의 현재 이름</b>이다
 * (FR-405). {@code tbl_fixed_expense} 에는 이름 컬럼이 <b>아예 없고</b>, 매퍼가 연관을
 * 타고 원본에서 읽는다.
 *
 * <p><b>004 의 {@code ExpenseResponse} 와 정반대다.</b>
 *
 * <table border="1">
 *   <caption>성격이 달라 규칙이 갈린다</caption>
 *   <tr><th></th><th>004 지출·소득</th><th>005 고정지출 설정</th></tr>
 *   <tr><td>무엇인가</td><td><b>과거 기록</b></td><td><b>지금 유효한 설정</b></td></tr>
 *   <tr><td>이름</td><td>등록 당시 스냅샷</td><td>조회 시점 현재 이름</td></tr>
 * </table>
 *
 * <p>월세 수단을 "국민카드"에서 "국민체크"로 바꿨다면 이 응답도 새 이름으로 바뀐다.
 * 그것이 맞다 — 지금 그 카드로 나가는 설정이기 때문이다. 반면 3월에 이미 쓴 지출은
 * 그때 이름으로 남아야 한다.
 *
 * @param fixedExpenseId    PK
 * @param name              고정지출 이름
 * @param paymentMethodId   수단 참조
 * @param paymentMethodName 수단의 <b>현재</b> 이름(스냅샷 아님)
 * @param expendGroupId     지출유형 참조
 * @param expendGroupName   지출유형의 <b>현재</b> 이름(스냅샷 아님)
 * @param amount            기본 금액(원)
 * @param paymentDayOfMonth 매달 결제일 1~31. <b>말일 보정 전 설정값</b>이며 보정된 날짜는
 *                          월별 내역의 {@code paymentDate} 가 갖는다
 * @param content           내용
 * @param startYear         적용 시작 연
 * @param startMonth        적용 시작 월
 * @param endYear           적용 종료 연
 * @param endMonth          적용 종료 월
 */
public record FixedExpenseResponse(
        Long fixedExpenseId,
        String name,
        Long paymentMethodId,
        String paymentMethodName,
        Long expendGroupId,
        String expendGroupName,
        Long amount,
        Integer paymentDayOfMonth,
        String content,
        Integer startYear,
        Integer startMonth,
        Integer endYear,
        Integer endMonth) {
}

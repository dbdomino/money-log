package com.dbdomino.moneylog.backend.dto.response;

/**
 * 수단별 요약 한 행 (5.5·5.6).
 *
 * <p><b>0원 행이 있다 — 유형별과 정반대다</b>(FR-521a). "어느 카드를 얼마나 썼나"를 보는
 * 표라 안 쓴 카드도 있어야 비교가 된다.
 *
 * <p>모집단은 <b>두 집합의 합집합</b>이다.
 *
 * <pre>{@code
 * ① 그 달 지출이 1건 이상인 수단  → 삭제 표시·미사용이어도 전부
 * ② 지출 0원 행                  → 저장 시점 사용 중인 purpose=EXPENSE 수단만
 * }</pre>
 *
 * <p>②를 "회원 소유 전부"로 넓히면 버린 카드의 0원 행이 매달 쌓이고, ①을 빠뜨리면 그 달에
 * 실제로 쓴 카드가 나중에 정리됐다는 이유로 사라져 <b>수단별 합이 지출 총액과 맞지
 * 않는다</b>.
 *
 * <p>소득 수단은 들어오지 않는다 — 통계의 이 표는 지출 요약이고 소득은
 * {@code incomeTotal} 에 합산된다.
 *
 * @param paymentMethodId   수단 PK. FK 가 없어 <b>실재를 보장하지 않는다</b>(FR-519)
 * @param paymentMethodName <b>저장 시점 이름 스냅샷</b>
 * @param amount            그 수단의 지출 합계. <b>{@code 0} 이 정상 값이다</b>
 */
public record StatisticsPaymentMethodResponse(
        long paymentMethodId,
        String paymentMethodName,
        long amount) {
}

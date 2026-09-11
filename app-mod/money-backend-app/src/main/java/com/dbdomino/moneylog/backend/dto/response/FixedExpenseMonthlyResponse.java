package com.dbdomino.moneylog.backend.dto.response;

import java.time.LocalDate;

/**
 * 월별 고정지출 내역 1건 — 4.5 목록의 요소이자 4.6 수정의 응답.
 *
 * <h2>식별자가 세 조각이다</h2>
 *
 * <p>{@code fixedExpenseId} 하나로는 이 행이 특정되지 않는다 — 같은 고정지출이 여러 달에
 * 걸쳐 행을 갖기 때문이다. {@code year}·{@code month} 가 함께 있어야 하며, 유니크 제약
 * {@code ux_fixed_expense_monthly (fixed_expense_idx, year, month)} 와 같은 조합이다.
 * 4.6 의 Path 가 {@code /monthly/{year}/{month}/{fixedExpenseId}} 인 이유가 그것이다.
 *
 * <p><b>월별 내역의 PK({@code idx})를 싣지 않는다.</b> 그 값으로는 4.6 을 부를 수 없어
 * 프론트에 쓸모가 없고, 실으면 "이걸로 수정하면 되겠지"라는 오해를 준다.
 *
 * <h2>이름 셋 다 현재 값이다</h2>
 *
 * <p>{@code fixedExpenseName}·{@code paymentMethodName}·{@code expendGroupName} 은 전부
 * <b>조회 시점의 현재 이름</b>이다(FR-405). {@code tbl_fixed_expense_monthly} 에는 이름
 * 컬럼이 하나도 없다.
 *
 * @param fixedExpenseId    고정지출 관리 PK. {@code year}·{@code month} 와 함께 이 행의 식별자
 * @param year              연
 * @param month             월
 * @param fixedExpenseName  고정지출 이름. 관리 테이블의 <b>현재</b> 값
 * @param amount            그 달 금액. 설정의 기본 금액과 다를 수 있다
 * @param paymentDate       그 달 결제일. <b>말일 보정이 끝난</b> 실제 날짜다 — 결제일 31 인
 *                          고정지출의 2026-02 내역은 {@code 2026-02-28} 이다
 * @param content           그 달 내용
 * @param paymentMethodId   그 달 수단
 * @param paymentMethodName 수단의 <b>현재</b> 이름(스냅샷 아님)
 * @param expendGroupId     그 달 지출유형
 * @param expendGroupName   지출유형의 <b>현재</b> 이름(스냅샷 아님)
 * @param modified          사용자가 이 달 값을 직접 고쳤는가. 참이면 설정 수정(4.4)의
 *                          자동 반영이 이 달을 덮지 않는다
 */
public record FixedExpenseMonthlyResponse(
        Long fixedExpenseId,
        Integer year,
        Integer month,
        String fixedExpenseName,
        Long amount,
        LocalDate paymentDate,
        String content,
        Long paymentMethodId,
        String paymentMethodName,
        Long expendGroupId,
        String expendGroupName,
        Boolean modified) {
}

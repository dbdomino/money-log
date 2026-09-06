package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 4.5 월별 고정지출 내역 목록의 조회 조건.
 *
 * <p><b>페이징이 없다.</b> 한 회원의 한 달 고정지출은 많아야 수십 건이라 전부 돌려주고
 * 합계({@code total})를 대신 싣는다 — {@code _공통.md § 목록 응답 규칙} 의 적용 대상 표가
 * 그렇게 확정했다. 005 의 목록 넷 중 페이징이 있는 것은 4.2 뿐이다.
 *
 * <p><b>연·월 오류가 {@code 3403} 이다.</b> 같은 뜻인데 4.8(가계부 목록)은
 * {@code 3501} 이고 006 의 통계는 {@code 3603} 이다. 자원별 코드 블록 배정(고정지출
 * {@code 34xx} / 가계부 {@code 35xx})의 결과이며 <b>의도된 차이</b>다 — 실수처럼 보여
 * 통일하고 싶어지지만 통일하면 규칙이 깨진다.
 *
 * <h2>필터는 생성 대상을 좁히지 않는다</h2>
 *
 * <p>두 필터는 <b>생성이 끝난 뒤 결과를 좁히는 데만</b> 쓴다(FR-406). 생성 대상에
 * 적용하면 {@code paymentMethodId=5} 로 그 달을 처음 열었을 때 수단 5 의 고정지출만
 * 만들어지고, 나중에 필터 없이 같은 달을 열면 나머지가 그때 생성된다. 그러면
 * <b>같은 달의 내역이 "언제 어떤 필터로 처음 열었는가"에 따라 달라진다.</b>
 *
 * @param yearMonth        조회 연·월. 검증을 통과한 값이다
 * @param paymentMethodId  수단 필터(선택). {@code null} 이면 거르지 않는다
 * @param expendGroupId    지출유형 필터(선택)
 */
public record FixedExpenseMonthlyListQuery(YearMonthValue yearMonth,
                                           Long paymentMethodId,
                                           Long expendGroupId) {

    /**
     * 값을 검증해 조회 조건을 만든다.
     *
     * @throws com.dbdomino.moneylog.common.error.BusinessException {@code 3403} —
     *         연·월 누락 또는 범위 오류
     */
    public static FixedExpenseMonthlyListQuery of(Integer year, Integer month,
                                                  Long paymentMethodId, Long expendGroupId) {
        return new FixedExpenseMonthlyListQuery(
                YearMonthValue.require(year, month, ErrorCode.FIXED_EXPENSE_MONTH_INVALID),
                paymentMethodId,
                expendGroupId);
    }

    /** 필터가 하나라도 걸렸는가. 걸리지 않았으면 결과를 그대로 돌려준다. */
    public boolean hasFilter() {
        return paymentMethodId != null || expendGroupId != null;
    }
}

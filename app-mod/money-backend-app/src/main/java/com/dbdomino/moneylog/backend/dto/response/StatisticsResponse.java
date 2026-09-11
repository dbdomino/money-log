package com.dbdomino.moneylog.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 5.5 월별 통계 조회.
 *
 * <h2>배열 3종은 목록 응답이 아니다</h2>
 *
 * <p>{@code weeklyExpenses}·{@code expendGroupSummaries}·{@code paymentMethodSummaries} 는
 * <b>{@code data.list} 규칙의 적용 대상이 아니다</b>({@code _공통.md § 적용 제외}). 세
 * 배열이 한 통계 객체의 <b>구성 요소</b>이지 각각이 목록 API 의 결과가 아니기 때문이다 —
 * 셋 중 하나를 {@code list} 로 이름 붙이면 나머지 둘의 자리가 없어진다. 페이징도 없다.
 *
 * <h2>{@code source} 와 {@code savedAt} 을 함께 내리는 이유</h2>
 *
 * <table border="1">
 *   <caption>세 가지 상태를 두 필드로 구분한다</caption>
 *   <tr><th>상황</th><th>{@code source}</th><th>{@code savedAt}</th></tr>
 *   <tr><td>저장본을 그대로 돌려줌</td><td>{@code SAVED}</td><td>저장 시각</td></tr>
 *   <tr><td>저장본이 없어 즉석 계산</td><td>{@code CALCULATED}</td><td>{@code null}</td></tr>
 *   <tr><td>{@code view=live} — 저장본이 있는데 무시함</td>
 *       <td>{@code CALCULATED}</td><td><b>저장 시각</b></td></tr>
 * </table>
 *
 * <p>셋째 줄이 {@code savedAt} 이 따로 필요한 이유다(FR-515). {@code source} 만으로는
 * "저장본이 없다"와 "있는데 지금은 최신을 본다"가 같아 보인다 — 프론트가 "저장본 있음 /
 * 지금 최신"을 나란히 보여주는 화면을 그릴 수 없다.
 *
 * <p><b>{@code savedAt} 이 {@code null} 일 때 필드를 생략하지 않는다.</b>
 *
 * @param year                   조회 연도
 * @param month                  조회 월
 * @param source                 {@code SAVED} 또는 {@code CALCULATED}
 * @param savedAt                그 달의 <b>마지막 저장</b> 시각. 저장본이 없으면 {@code null}
 * @param incomeTotal            월 소득 합계
 * @param expenseTotal           월 지출 합계 — <b>일반 + 할부 + 고정</b>
 * @param fixedVsRegularRatio    고정 vs 일반 비율. 네 값을 한 객체로 묶는다
 * @param weeklyExpenses         주별 지출. 지출 0원인 주도 행이 있다
 * @param expendGroupSummaries   유형별 요약. <b>0원 유형은 없다</b>(FR-521)
 * @param paymentMethodSummaries 수단별 요약. <b>0원 행도 있다</b>(FR-521a) — 유형별과 반대다
 */
public record StatisticsResponse(
        int year,
        int month,
        String source,
        OffsetDateTime savedAt,
        long incomeTotal,
        long expenseTotal,
        FixedVsRegularRatio fixedVsRegularRatio,
        List<StatisticsWeeklyResponse> weeklyExpenses,
        List<StatisticsExpendGroupResponse> expendGroupSummaries,
        List<StatisticsPaymentMethodResponse> paymentMethodSummaries) {

    /** 저장본을 그대로 돌려준다. */
    public static final String SOURCE_SAVED = "SAVED";

    /** 지금 계산했다. {@code view=live} 는 저장본이 있어도 항상 이 값이다. */
    public static final String SOURCE_CALCULATED = "CALCULATED";

    /**
     * 고정 vs 일반 지출 비율.
     *
     * <p>네 값을 <b>한 객체로 묶는 것</b>은 넷이 함께여야 의미가 서기 때문이다 —
     * 금액만 있으면 비율을 화면이 다시 계산해야 하고, 비율만 있으면 원래 금액을 알 수 없다.
     *
     * @param fixedAmount    고정지출 합계. {@code tbl_fixed_expense_monthly} 에서 읽는다
     * @param regularAmount  일반·할부 지출 합계
     * @param fixedPercent   고정 비율 (0~100). <b>{@code expenseTotal} 이 0 이면 0</b> 이다
     * @param regularPercent 일반 비율 (0~100). 같은 규칙이다
     */
    public record FixedVsRegularRatio(
            long fixedAmount,
            long regularAmount,
            BigDecimal fixedPercent,
            BigDecimal regularPercent) {
    }
}

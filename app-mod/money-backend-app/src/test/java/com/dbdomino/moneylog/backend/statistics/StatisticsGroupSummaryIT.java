package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 유형별 요약 — quickstart #26·#30·#31 (FR-521·522).
 *
 * <h2>0원 유형은 표에 없다</h2>
 *
 * <p>모집단은 <b>그 달 지출이 1건 이상인 유형</b>뿐이다. 목표만 정해 두고 한 푼도 쓰지
 * 않은 유형은 나오지 않는다 — 수단별과 정반대다.
 *
 * <h2>#31 을 빠뜨리면 런타임에 DB 오류가 새어 나간다</h2>
 *
 * <p>{@code usage_rate} 가 {@code numeric(6,2)} 라 최대 {@code 9999.99} 다. 목표 1,000원에
 * 지출 1,000만원이면 1,000,000% 가 나오는데 자르지 않으면 저장(5.6)에서 DB 오류가 나고
 * 사용자에게는 <b>{@code 9000}</b> 으로 보인다. 조회는 저장하지 않으므로 여기서는 값만
 * 확인하지만, 계산기를 5.5·5.6 이 공유하므로 여기서 막으면 저장도 막힌다.
 */
class StatisticsGroupSummaryIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#26 그 달 지출이 0원인 유형은 유형별 요약에 없다")
    void excludesGroupsWithoutExpenses() throws Exception {
        Fixture fixture = prepare();
        long transportGroupId = defaultGroupId(fixture.member(), "교통");
        // 교통에는 목표만 정하고 지출은 만들지 않는다.
        putDefaultTarget(fixture.token(), transportGroupId, 100_000L);
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(groupSummaryOf(response, fixture.foodGroupId())).isNotNull();
        assertThat(groupSummaryOf(response, transportGroupId)).isNull();
    }

    @Test
    @DisplayName("유형별 행에 금액·목표·사용률·상태가 실린다")
    void carriesAllGroupFields() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        addExpense(fixture, "2026-07-03", 320_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("expendGroupName").asText()).isEqualTo("식비");
        assertThat(row.get("amount").asLong()).isEqualTo(320_000L);
        assertThat(row.get("target").asLong()).isEqualTo(400_000L);
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(row.get("status").asText()).isEqualTo("UNDER");
    }

    /** 목표는 <b>적용 금액</b>이다 — 월별이 있으면 그것, 없으면 기본(FR-507). */
    @Test
    @DisplayName("월별 목표가 있으면 그것이 적용 금액이다")
    void monthlyTargetWins() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        putMonthlyTarget(fixture.token(), FIXED_YEAR, FIXED_MONTH, fixture.foodGroupId(),
                800_000L);
        addExpense(fixture, "2026-07-03", 400_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("target").asLong()).isEqualTo(800_000L);
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("50.00");
    }

    /**
     * 월별 {@code 0} 은 "그 달엔 쓰지 않겠다"라 <b>그대로 적용된다</b>.
     *
     * <p>0 을 "미설정"으로 접으면 기본 목표가 살아나 사용률이 전혀 달라진다.
     */
    @Test
    @DisplayName("월별 목표 0원은 기본으로 떨어지지 않는다")
    void monthlyZeroDoesNotFallBackToDefault() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        putMonthlyTarget(fixture.token(), FIXED_YEAR, FIXED_MONTH, fixture.foodGroupId(), 0L);
        addExpense(fixture, "2026-07-03", 100_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("target").asLong()).isZero();
        // 목표가 0 이면 나눗셈이 성립하지 않아 사용률은 0 이다.
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("#30 목표가 0원인 유형의 사용률은 0 이다")
    void zeroTargetGivesZeroUsageRate() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 100_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        // 목표를 정한 적이 없으므로 적용 금액은 0 이다(기본이 없으면 0, FR-507).
        assertThat(row.get("target").asLong()).isZero();
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("#31 목표 1,000원에 지출 1,000만원이면 사용률이 9999.99 로 잘린다")
    void capsUsageRate() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 1_000L);
        addExpense(fixture, "2026-07-03", 10_000_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("9999.99");
        assertThat(row.get("status").asText()).isEqualTo("OVER");
    }

    /** 사용률은 100% 를 넘을 수 있어야 한다 — 목표 초과가 정상적인 상태다. */
    @Test
    @DisplayName("사용률은 100 을 넘을 수 있다")
    void allowsOverHundred() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 100_000L);
        addExpense(fixture, "2026-07-03", 250_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("250.00");
    }

    /** 한 유형의 지출 여러 건이 합쳐진다. */
    @Test
    @DisplayName("같은 유형의 지출 여러 건이 합산된다")
    void sumsExpensesPerGroup() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 10_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-07-15", 20_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("amount").asLong()).isEqualTo(30_000L);
    }

    /** 고정지출도 그 유형의 합계에 들어간다 — 유형별 합이 지출 총액과 맞아야 한다. */
    @Test
    @DisplayName("고정지출도 그 유형의 합계에 더해진다")
    void includesFixedExpense() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());
        long fixedExpenseId = createFixedExpense(fixture.token(), "월세",
                fixture.expenseMethodId(), fixture.foodGroupId(), 500_000L, 5,
                "2026-07", "2026-12");
        insertMonthlyRow(fixture.member(), fixedExpenseId, FIXED_YEAR, FIXED_MONTH,
                500_000L, "2026-07-05");

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("amount").asLong()).isEqualTo(530_000L);
    }
}

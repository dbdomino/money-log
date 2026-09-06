package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 즉석 계산의 기본 — quickstart #17·#32·#35·#36.
 *
 * <p>저장본이 없으면 지금 계산하고 {@code source=CALCULATED} 다.
 *
 * <h2>#36 이 005 와의 경계다</h2>
 *
 * <p>통계 조회는 월별 고정지출 내역의 <b>lazy 생성을 일으키지 않는다.</b> 그래서 한 번도
 * 열지 않은 달의 고정지출 합계는 <b>0</b> 이다. 놀랄 수 있지만 결정이며, 이 시험은
 * "합계가 0"만이 아니라 <b>조회 후에도 월별 행이 생기지 않았음</b>을 DB 에서 함께 센다 —
 * 합계만 보면 "만들었지만 그 달 결제일이 안 맞아 0"인 구현도 통과한다.
 */
class StatisticsCalculateIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#17 저장본이 없으면 즉석 계산이고 source 는 CALCULATED 다")
    void calculatesWhenNoSnapshot() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(data.get("savedAt").isNull()).isTrue();
        assertThat(data.get("year").asInt()).isEqualTo(FIXED_YEAR);
        assertThat(data.get("month").asInt()).isEqualTo(FIXED_MONTH);
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("#35 합계·비율 6값이 전부 실린다")
    void carriesAllSixTotals() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());
        addIncome(fixture, "2026-07-25", 3_500_000L);

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");
        JsonNode ratio = data.get("fixedVsRegularRatio");

        assertThat(data.get("incomeTotal").asLong()).isEqualTo(3_500_000L);
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(30_000L);
        assertThat(ratio.get("fixedAmount").asLong()).isZero();
        assertThat(ratio.get("regularAmount").asLong()).isEqualTo(30_000L);
        // 고정이 없으므로 일반이 100% 다.
        assertThat(ratio.get("fixedPercent").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(ratio.get("regularPercent").decimalValue()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("#32 지출 합계가 0인 달은 두 비율이 0 이다 — 0 으로 나누지 않는다")
    void zeroExpenseGivesZeroPercents() throws Exception {
        Fixture fixture = prepare();
        addIncome(fixture, "2026-07-25", 3_500_000L);

        JsonNode ratio = ratioOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH));

        assertThat(ratio.get("fixedPercent").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(ratio.get("regularPercent").decimalValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("거래가 한 건도 없어도 6값이 0 으로 채워진다")
    void emptyMonthIsAllZero() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.get("incomeTotal").asLong()).isZero();
        assertThat(data.get("expenseTotal").asLong()).isZero();
        assertThat(data.get("expendGroupSummaries")).isEmpty();
        // 주별은 그 달의 주 수만큼 0원 행이 남는다.
        assertThat(data.get("weeklyExpenses")).isNotEmpty();
    }

    /**
     * 지출은 <b>일반 + 할부 + 고정</b> 셋의 합이다(005 의 4.8 과 같은 정의).
     *
     * <p>여기서는 고정지출 월별 행을 JDBC 로 넣어 세 갈래가 다 더해지는지 본다.
     */
    @Test
    @DisplayName("지출 합계는 일반과 고정을 함께 더한다")
    void sumsRegularAndFixed() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());
        long fixedExpenseId = createFixedExpense(fixture.token(), "월세",
                fixture.expenseMethodId(), fixture.foodGroupId(), 500_000L, 5,
                "2026-07", "2026-12");
        insertMonthlyRow(fixture.member(), fixedExpenseId, FIXED_YEAR, FIXED_MONTH,
                500_000L, "2026-07-05");

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");
        JsonNode ratio = data.get("fixedVsRegularRatio");

        assertThat(data.get("expenseTotal").asLong()).isEqualTo(530_000L);
        assertThat(ratio.get("fixedAmount").asLong()).isEqualTo(500_000L);
        assertThat(ratio.get("regularAmount").asLong()).isEqualTo(30_000L);
    }

    /**
     * #36 — 005 와의 경계.
     *
     * <p>고정지출 <b>설정</b>은 있지만 그 달을 4.5·4.8·4.9 로 연 적이 없다. 통계 조회는
     * 월별 내역을 만들지 않으므로 고정지출 합계가 0 이어야 하고, 조회 뒤에도 월별 행이
     * 없어야 한다.
     */
    @Test
    @DisplayName("#36 한 번도 열지 않은 달은 고정지출 합계가 0 이고 조회가 행을 만들지 않는다")
    void doesNotTriggerLazyCreation() throws Exception {
        Fixture fixture = prepare();
        createFixedExpense(fixture.token(), "월세", fixture.expenseMethodId(),
                fixture.foodGroupId(), 500_000L, 5, "2026-07", "2026-12");
        assertThat(countMonthly(fixture.member(), FIXED_YEAR, FIXED_MONTH)).isZero();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.get("fixedVsRegularRatio").get("fixedAmount").asLong()).isZero();
        assertThat(data.get("expenseTotal").asLong()).isZero();
        // "만들지 않았다"를 직접 센다 — 합계만 보면 만들어 놓고 0 인 구현도 통과한다.
        assertThat(countMonthly(fixture.member(), FIXED_YEAR, FIXED_MONTH)).isZero();
    }

    @Test
    @DisplayName("남의 거래는 섞이지 않는다")
    void isolatesByOwner() throws Exception {
        Fixture mine = prepare();
        Fixture other = prepare();
        addExpense(other, "2026-07-03", 999_000L, other.foodGroupId());

        JsonNode data = statistics(mine, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.get("expenseTotal").asLong()).isZero();
    }

    @Test
    @DisplayName("다른 달의 거래는 섞이지 않는다")
    void isolatesByMonth() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-06-30", 10_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-08-01", 20_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-07-01", 30_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-07-31", 40_000L, fixture.foodGroupId());

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        // 말일과 1일이 포함되고 그 바깥은 빠진다 — 경계를 양쪽에서 건다.
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(70_000L);
    }
}

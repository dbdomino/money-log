package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 주 경계 — quickstart #24·#25 (FR-520).
 *
 * <pre>{@code
 * 1. 주는 월요일에 시작한다.
 * 2. 그 달 1일이 월요일이 아니면 첫 주는 1일부터 첫 일요일까지다 (짧은 주).
 * 3. 마지막 주는 말일에서 끊는다 (짧은 주).
 * }</pre>
 *
 * <h2>두 종류의 달을 함께 건다</h2>
 *
 * <p><b>1일이 월요일이 아닌 달</b>(2026-07, 수요일)과 <b>1일이 월요일인 달</b>(2026-06)을
 * 나란히 본다. 한쪽만 보면 "항상 1일부터 7일씩 끊는" 구현과 "항상 첫 일요일에서 끊는"
 * 구현 중 하나가 통과한다 — 두 규칙은 한쪽 달에서만 결과가 갈린다.
 *
 * <p>고정 연월을 쓴다. 1일의 요일이 달마다 달라 상대값으로는 기대값을 적을 수 없다.
 */
class StatisticsWeekBoundaryIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#24 1일이 수요일인 달의 첫 주는 1일부터 첫 일요일까지다")
    void firstWeekEndsOnFirstSunday() throws Exception {
        Fixture fixture = prepare();

        // 2026-07-01 은 수요일이다.
        JsonNode week = weekOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH), 1);

        assertThat(week).isNotNull();
        assertThat(week.get("weekStart").asText()).isEqualTo("2026-07-01");
        assertThat(week.get("weekEnd").asText()).isEqualTo("2026-07-05");
    }

    @Test
    @DisplayName("두 번째 주부터는 월요일에 시작한다")
    void laterWeeksStartOnMonday() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(weekOf(response, 2).get("weekStart").asText()).isEqualTo("2026-07-06");
        assertThat(weekOf(response, 2).get("weekEnd").asText()).isEqualTo("2026-07-12");
        assertThat(weekOf(response, 3).get("weekStart").asText()).isEqualTo("2026-07-13");
        assertThat(weekOf(response, 4).get("weekStart").asText()).isEqualTo("2026-07-20");
    }

    @Test
    @DisplayName("#25 마지막 주는 말일에서 끊긴다")
    void lastWeekStopsAtMonthEnd() throws Exception {
        Fixture fixture = prepare();

        JsonNode weeks = statistics(fixture, FIXED_YEAR, FIXED_MONTH)
                .get("data").get("weeklyExpenses");

        assertThat(weeks).hasSize(5);
        JsonNode last = weeks.get(weeks.size() - 1);
        assertThat(last.get("weekIndex").asInt()).isEqualTo(5);
        assertThat(last.get("weekStart").asText()).isEqualTo("2026-07-27");
        assertThat(last.get("weekEnd").asText()).isEqualTo("2026-07-31");
    }

    /**
     * 1일이 월요일이면 첫 주가 온전한 7일이다.
     *
     * <p>2026-06-01 은 월요일이다. 여기서 첫 주가 짧게 나오면 "무조건 첫 일요일까지"를
     * 예외로 잘못 구현한 것이다.
     */
    @Test
    @DisplayName("1일이 월요일인 달은 첫 주가 7일이다")
    void fullFirstWeekWhenMonthStartsOnMonday() throws Exception {
        Fixture fixture = prepare();

        JsonNode week = weekOf(statistics(fixture, 2026, 6), 1);

        assertThat(week.get("weekStart").asText()).isEqualTo("2026-06-01");
        assertThat(week.get("weekEnd").asText()).isEqualTo("2026-06-07");
    }

    @Test
    @DisplayName("주 경계가 그 달을 빠짐없이 덮고 겹치지 않는다")
    void weeksCoverTheMonthExactly() throws Exception {
        Fixture fixture = prepare();

        JsonNode weeks = statistics(fixture, FIXED_YEAR, FIXED_MONTH)
                .get("data").get("weeklyExpenses");

        assertThat(weeks.get(0).get("weekStart").asText()).isEqualTo("2026-07-01");
        assertThat(weeks.get(weeks.size() - 1).get("weekEnd").asText()).isEqualTo("2026-07-31");
        for (int i = 1; i < weeks.size(); i++) {
            // 앞 주의 끝 다음 날이 이 주의 시작이어야 한다 — 하루도 빠지거나 겹치지 않는다.
            java.time.LocalDate previousEnd =
                    java.time.LocalDate.parse(weeks.get(i - 1).get("weekEnd").asText());
            java.time.LocalDate start =
                    java.time.LocalDate.parse(weeks.get(i).get("weekStart").asText());
            assertThat(start).isEqualTo(previousEnd.plusDays(1));
            assertThat(weeks.get(i).get("weekIndex").asInt()).isEqualTo(i + 1);
        }
    }

    /** 지출이 주 경계대로 갈린다. 경계만 맞고 합계를 엉뚱한 주에 넣는 구현을 잡는다. */
    @Test
    @DisplayName("지출이 그 날짜가 속한 주에 더해진다")
    void assignsExpensesToTheirWeek() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-05", 10_000L, fixture.foodGroupId());  // 1주 마지막 날
        addExpense(fixture, "2026-07-06", 20_000L, fixture.foodGroupId());  // 2주 첫날
        addExpense(fixture, "2026-07-31", 40_000L, fixture.foodGroupId());  // 5주 말일

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(weekOf(response, 1).get("amount").asLong()).isEqualTo(10_000L);
        assertThat(weekOf(response, 2).get("amount").asLong()).isEqualTo(20_000L);
        assertThat(weekOf(response, 3).get("amount").asLong()).isZero();
        assertThat(weekOf(response, 5).get("amount").asLong()).isEqualTo(40_000L);
    }

    /** 고정지출도 결제일이 속한 주에 들어간다 — 주별 합계가 일반 지출만 세면 안 된다. */
    @Test
    @DisplayName("고정지출도 결제일이 속한 주에 더해진다")
    void includesFixedExpenseInItsWeek() throws Exception {
        Fixture fixture = prepare();
        long fixedExpenseId = createFixedExpense(fixture.token(), "월세",
                fixture.expenseMethodId(), fixture.foodGroupId(), 500_000L, 20,
                "2026-07", "2026-12");
        insertMonthlyRow(fixture.member(), fixedExpenseId, FIXED_YEAR, FIXED_MONTH,
                500_000L, "2026-07-20");

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(weekOf(response, 4).get("amount").asLong()).isEqualTo(500_000L);
        assertThat(weekOf(response, 3).get("amount").asLong()).isZero();
    }
}

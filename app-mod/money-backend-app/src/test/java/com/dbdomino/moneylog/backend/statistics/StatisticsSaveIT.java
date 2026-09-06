package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 통계 저장의 기본 — quickstart #37·#38.
 *
 * <p>저장 응답에 {@code savedAt}·{@code source=SAVED} 가 실려 <b>재조회 없이</b> 화면을
 * 갱신할 수 있다. 사용자는 5.5 로 그 달을 보다가 저장을 누르므로 화면에 숫자가 이미 있고,
 * 필요한 것은 "저장됨 · 그 시각" 배지뿐이다 — 두 필드가 없으면 화면이 5.5 를 한 번 더
 * 불러야 한다.
 *
 * <p>연월은 <b>상대값</b>으로 잡는다. 저장은 "지금이 언제인가"에 답이 달려 있어(FR-527)
 * 고정값을 박으면 그 날짜가 지나는 순간 시험이 조용히 반대를 검증한다.
 */
class StatisticsSaveIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#37 저장 응답에 savedAt 과 source=SAVED 가 실린다")
    void saveResponseCarriesSavedAtAndSource() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        JsonNode data = save(fixture, year, month).get("data");

        assertThat(data.get("year").asInt()).isEqualTo(year);
        assertThat(data.get("month").asInt()).isEqualTo(month);
        assertThat(data.get("source").asText()).isEqualTo("SAVED");
        assertThat(data.get("savedAt").isNull()).isFalse();
        assertThat(data.get("message").asText()).contains(String.valueOf(year));
    }

    @Test
    @DisplayName("#38 저장 후 기본 조회는 source=SAVED 다")
    void readsBackAsSaved() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());

        save(fixture, year, month);

        JsonNode data = statistics(fixture, year, month).get("data");
        assertThat(data.get("source").asText()).isEqualTo("SAVED");
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(30_000L);
    }

    /**
     * 저장 직후 5.5 가 <b>같은 숫자</b>를 낸다.
     *
     * <p>5.5 와 5.6 이 같은 계산기를 쓴다는 것을 값으로 확인한다 — 각자 계산하면 저장
     * 직후 숫자가 달라지고, 그게 이 기능이 방지하려는 상황이다.
     */
    @Test
    @DisplayName("저장 직후 조회가 저장 전 계산과 같은 값을 낸다")
    void saveMatchesLiveCalculation() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        addExpense(fixture, lastMonth().atDay(3).toString(), 320_000L, fixture.foodGroupId());
        addIncome(fixture, lastMonth().atDay(25).toString(), 3_500_000L);

        JsonNode before = statistics(fixture, year, month).get("data");
        save(fixture, year, month);
        JsonNode after = statistics(fixture, year, month).get("data");

        assertThat(before.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(after.get("source").asText()).isEqualTo("SAVED");
        assertThat(after.get("incomeTotal").asLong())
                .isEqualTo(before.get("incomeTotal").asLong());
        assertThat(after.get("expenseTotal").asLong())
                .isEqualTo(before.get("expenseTotal").asLong());
        assertThat(after.get("fixedVsRegularRatio").get("regularPercent").decimalValue())
                .isEqualByComparingTo(
                        before.get("fixedVsRegularRatio").get("regularPercent").decimalValue());
    }

    @Test
    @DisplayName("상세 3종이 함께 저장된다")
    void savesAllDetailTables() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());

        save(fixture, year, month);

        assertThat(countStatistics(fixture.member(), year, month)).isEqualTo(1);
        assertThat(countStatisticsDetails(fixture.member(), year, month, "tbl_statistics_weekly"))
                .isPositive();
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(1);
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_payment_method")).isPositive();
    }

    @Test
    @DisplayName("저장본의 유형별 요약이 저장 당시 이름·목표·상태를 담는다")
    void savesGroupSnapshotFields() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        addExpense(fixture, lastMonth().atDay(3).toString(), 320_000L, fixture.foodGroupId());

        save(fixture, year, month);

        JsonNode row = groupSummaryOf(statistics(fixture, year, month), fixture.foodGroupId());
        assertThat(row.get("expendGroupName").asText()).isEqualTo("식비");
        assertThat(row.get("target").asLong()).isEqualTo(400_000L);
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(row.get("status").asText()).isEqualTo("UNDER");
    }

    @Test
    @DisplayName("남의 달을 저장하지 않는다 — 회원마다 별개다")
    void savesPerOwner() throws Exception {
        Fixture mine = prepare();
        Fixture other = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(mine, year, month);

        assertThat(countStatistics(mine.member(), year, month)).isEqualTo(1);
        assertThat(countStatistics(other.member(), year, month)).isZero();
        assertThat(statistics(other, year, month).get("data").get("source").asText())
                .isEqualTo("CALCULATED");
    }

    @Test
    @DisplayName("연·월이 범위를 벗어나면 3603 이다")
    void rejectsBadYearMonth() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(save(fixture, 1999, 7))).isEqualTo(3603);
        assertThat(resCode(save(fixture, 2101, 7))).isEqualTo(3603);
        assertThat(resCode(save(fixture, 2026, 13))).isEqualTo(3603);
        assertThat(resCode(save(fixture, 2026, 0))).isEqualTo(3603);
    }
}

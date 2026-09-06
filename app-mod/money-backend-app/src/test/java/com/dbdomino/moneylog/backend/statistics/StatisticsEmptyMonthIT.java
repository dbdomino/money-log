package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 빈 달의 저장 — quickstart #47·#48·#49 (SC-511 · FR-528).
 *
 * <p>지출·소득이 <b>한 건도 없어도</b> 저장할 수 있다.
 *
 * <h2>거절하지 않는 이유</h2>
 *
 * <p>{@code tbl_statistics} 의 여섯 컬럼이 NOT NULL 이라 0 스냅샷을 남길 자리가 이미
 * 있고, 거절하면 <b>"이 달은 아무것도 쓰지 않았다"는 확정을 남길 방법이 없어진다</b> —
 * 화면이 그 상태를 저장본 없음({@code CALCULATED})과 구분하지 못한다.
 *
 * <h2>상세 3종이 서로 다르게 채워진다</h2>
 *
 * <table border="1">
 *   <caption>빈 달의 상세</caption>
 *   <tr><th>상세</th><th>내용</th><th>근거</th></tr>
 *   <tr><td>주별</td><td>그 달의 주 수만큼 <b>0원 행</b></td><td>주는 지출과 무관하게 있다</td></tr>
 *   <tr><td>유형별</td><td><b>빈 배열</b></td><td>0원 유형은 넣지 않는다(FR-521)</td></tr>
 *   <tr><td>수단별</td><td>사용 중 {@code EXPENSE} 수단의 <b>0원 행</b></td><td>FR-521a ②</td></tr>
 * </table>
 */
class StatisticsEmptyMonthIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#47 거래가 한 건도 없는 달도 저장된다")
    void savesEmptyMonth() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        JsonNode response = save(fixture, year, month);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("source").asText()).isEqualTo("SAVED");
        assertThat(countStatistics(fixture.member(), year, month)).isEqualTo(1);
    }

    @Test
    @DisplayName("#47 합계·비율 6값이 전부 0 이다")
    void allSixTotalsAreZero() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);

        JsonNode data = statistics(fixture, year, month).get("data");
        JsonNode ratio = data.get("fixedVsRegularRatio");
        assertThat(data.get("incomeTotal").asLong()).isZero();
        assertThat(data.get("expenseTotal").asLong()).isZero();
        assertThat(ratio.get("fixedAmount").asLong()).isZero();
        assertThat(ratio.get("regularAmount").asLong()).isZero();
        assertThat(ratio.get("fixedPercent").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(ratio.get("regularPercent").decimalValue()).isEqualByComparingTo("0.00");
    }

    /**
     * #48 — 저장 여부가 {@code source} 로 구분된다.
     *
     * <p>빈 달을 거절했다면 "0인데 저장 안 함"과 "0으로 확정함"이 둘 다
     * {@code CALCULATED} 라 구분되지 않는다.
     */
    @Test
    @DisplayName("#48 저장 전은 CALCULATED, 저장 후는 SAVED 다 — 값은 둘 다 0 이다")
    void distinguishesSavedZeroFromUnsavedZero() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        JsonNode before = statistics(fixture, year, month).get("data");
        assertThat(before.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(before.get("savedAt").isNull()).isTrue();

        save(fixture, year, month);

        JsonNode after = statistics(fixture, year, month).get("data");
        assertThat(after.get("source").asText()).isEqualTo("SAVED");
        assertThat(after.get("savedAt").isNull()).isFalse();
        assertThat(after.get("expenseTotal").asLong()).isZero();
    }

    @Test
    @DisplayName("#49 빈 달의 유형별 상세는 빈 배열이다")
    void groupSummariesAreEmpty() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);

        assertThat(statistics(fixture, year, month).get("data").get("expendGroupSummaries"))
                .isEmpty();
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isZero();
    }

    @Test
    @DisplayName("빈 달도 주별 상세는 그 달의 주 수만큼 0원 행으로 남는다")
    void weeklyRowsRemain() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);

        JsonNode weeks = statistics(fixture, year, month).get("data").get("weeklyExpenses");
        assertThat(weeks).isNotEmpty();
        for (JsonNode week : weeks) {
            assertThat(week.get("amount").asLong()).isZero();
        }
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_weekly")).isEqualTo(weeks.size());
    }

    /** 빈 달이어도 사용 중 {@code EXPENSE} 수단의 0원 행은 남는다(FR-521a ②). */
    @Test
    @DisplayName("빈 달도 사용 중 지출 수단의 0원 행은 남는다")
    void methodZeroRowsRemain() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);

        JsonNode row = methodSummaryOf(statistics(fixture, year, month),
                fixture.expenseMethodId());
        assertThat(row).isNotNull();
        assertThat(row.get("amount").asLong()).isZero();
        // 소득 수단은 들어오지 않는다.
        assertThat(methodSummaryOf(statistics(fixture, year, month),
                fixture.incomeMethodId())).isNull();
    }
}

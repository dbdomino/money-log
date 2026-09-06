package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 재저장 — quickstart #40·#41·#42·#46-2 (SC-505 · FR-516·517).
 *
 * <pre>{@code
 * 1. 통계 행은 갱신한다      → 행이 늘지 않고 saved_at 이 새로 써진다
 * 2. 상세 3종은 지웠다 넣는다 → 행 집합 자체가 달라지기 때문이다
 * }</pre>
 *
 * <h2>#42 가 핵심이다</h2>
 *
 * <p>상세를 <b>갱신으로</b> 구현하면 재저장 전후로 유형별 행 수가 <b>줄었을 때</b>
 * 없어진 유형의 행이 남는다. 늘어나는 쪽만 시험하면 갱신 구현도 통과한다 — 그래서
 * 지출을 지워 <b>줄어드는</b> 방향을 반드시 건다.
 *
 * <h2>#46-2 는 두 번째 저장에서만 터진다</h2>
 *
 * <p>상세 3종에 유니크 제약이 있고({@code ux_stat_group}·{@code ux_stat_method}·
 * {@code ux_stat_weekly}) <b>Hibernate 는 한 flush 안에서 INSERT 를 DELETE 보다 먼저
 * 실행한다.</b> 삭제 뒤 flush 를 명시하지 않으면 같은 키의 INSERT 가 먼저 나가
 * {@code 9000} 이 된다. 첫 저장만 거는 시험으로는 절대 드러나지 않는다.
 */
class StatisticsResaveIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#40 같은 달을 다시 저장해도 통계 행은 1건이다")
    void keepsExactlyOneStatisticsRow() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());

        save(fixture, year, month);
        save(fixture, year, month);

        assertThat(countStatistics(fixture.member(), year, month)).isEqualTo(1);
    }

    /**
     * #46-2 — 열 번 저장해도 전부 성공한다.
     *
     * <p>{@code resCode} 를 매번 확인하는 것이 요점이다. 행 수만 세면 두 번째가
     * {@code 9000} 으로 실패해도 1건이라 통과한다.
     */
    @Test
    @DisplayName("#46-2 같은 달을 열 번 저장해도 전부 200 이다")
    void repeatedSavesAllSucceed() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());

        for (int i = 1; i <= 10; i++) {
            JsonNode response = save(fixture, year, month);
            assertThat(resCode(response)).as("%d 번째 저장: %s", i, response).isEqualTo(200);
        }

        assertThat(countStatistics(fixture.member(), year, month)).isEqualTo(1);
    }

    @Test
    @DisplayName("#40 재저장이 savedAt 을 갱신한다")
    void refreshesSavedAt() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);
        OffsetDateTime first = savedAtOf(fixture.member(), year, month);

        JsonNode second = save(fixture, year, month);

        OffsetDateTime after = savedAtOf(fixture.member(), year, month);
        assertThat(after).isAfterOrEqualTo(first);
        assertThat(second.get("data").get("savedAt").isNull()).isFalse();
    }

    @Test
    @DisplayName("#41 재저장하면 이전 상세가 지워지고 새로 계산된 것으로 채워진다")
    void replacesDetails() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());
        save(fixture, year, month);

        addExpense(fixture, lastMonth().atDay(10).toString(), 70_000L, fixture.foodGroupId());
        save(fixture, year, month);

        JsonNode data = statistics(fixture, year, month).get("data");
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(100_000L);
        assertThat(groupSummaryOf(statistics(fixture, year, month), fixture.foodGroupId())
                .get("amount").asLong()).isEqualTo(100_000L);
        // 상세가 쌓이지 않고 갈아 끼워진다.
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(1);
    }

    @Test
    @DisplayName("#42 재저장으로 유형별 행 수가 늘어나면 늘어난 대로 반영된다")
    void detailRowsGrow() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());
        save(fixture, year, month);
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(1);

        long transportGroupId = defaultGroupId(fixture.member(), "교통");
        addExpense(fixture, lastMonth().atDay(10).toString(), 5_000L, transportGroupId);
        save(fixture, year, month);

        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(2);
    }

    /**
     * #42 의 <b>줄어드는</b> 방향 — 갱신 구현이 여기서 걸린다.
     *
     * <p>지출을 지워 유형별 행 수가 2 → 1 이 되어야 하는데, "있는 것만 갱신"하면 없어진
     * 유형의 행이 그대로 남아 2 가 된다.
     */
    @Test
    @DisplayName("#42 재저장으로 유형별 행 수가 줄어들면 없어진 유형의 행이 남지 않는다")
    void detailRowsShrink() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        long transportGroupId = defaultGroupId(fixture.member(), "교통");
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());
        long transportExpenseId =
                addExpense(fixture, lastMonth().atDay(10).toString(), 5_000L, transportGroupId);
        save(fixture, year, month);
        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(2);

        assertThat(resCode(deleteJson("/api/v1/expenses/" + transportExpenseId,
                fixture.token()))).isEqualTo(200);
        save(fixture, year, month);

        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_expend_group")).isEqualTo(1);
        assertThat(groupSummaryOf(statistics(fixture, year, month), transportGroupId)).isNull();
    }

    /** 주별 상세도 갈아 끼워진다 — 유니크 제약이 {@code (statistics_idx, week_index)} 다. */
    @Test
    @DisplayName("주별 상세도 재저장에서 쌓이지 않는다")
    void weeklyRowsAreReplaced() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        save(fixture, year, month);
        int first = countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_weekly");
        save(fixture, year, month);

        assertThat(countStatisticsDetails(fixture.member(), year, month,
                "tbl_statistics_weekly")).isEqualTo(first);
    }
}

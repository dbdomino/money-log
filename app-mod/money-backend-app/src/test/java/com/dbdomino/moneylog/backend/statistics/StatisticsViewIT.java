package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * {@code view} 분기 — quickstart #18·#19·#20·#21·#22·#23 (SC-507 · FR-514·515).
 *
 * <pre>{@code
 * 생략 · saved  →  저장본 있음 → 저장본(SAVED) / 없음 → 즉석 계산(CALCULATED)
 * live          →  저장본을 읽지도 쓰지도 않고 즉석 계산. source 는 항상 CALCULATED
 * 그 밖의 값     →  3603
 * }</pre>
 *
 * <h2>저장본을 JDBC 로 만든다</h2>
 *
 * <p>5.6 을 부르지 않는 것은 <b>US2 가 US3 없이 완결되어야 하기</b> 때문이다. 그리고
 * 합계를 <b>일부러 계산값과 다르게</b> 넣는다 — {@code source} 필드만 보면 분기는 맞는데
 * 숫자를 다른 데서 읽는 구현이 통과하기 때문이다.
 */
class StatisticsViewIT extends AbstractStatisticsIT {

    /** 저장본의 소득 합계. 실제 거래에서 나올 수 없는 값이라 출처가 숫자로 드러난다. */
    private static final long SNAPSHOT_INCOME = 7_777_777L;

    /** 저장본의 지출 합계. */
    private static final long SNAPSHOT_EXPENSE = 1_111_111L;

    /** 저장본과 <b>다른</b> 실제 거래 하나를 만든 뒤 저장본을 심는다. */
    private Fixture prepareWithSnapshot() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());
        insertStatistics(fixture.member(), FIXED_YEAR, FIXED_MONTH,
                SNAPSHOT_INCOME, SNAPSHOT_EXPENSE);
        return fixture;
    }

    @Test
    @DisplayName("#18 저장본이 있으면 기본 조회는 저장본이다")
    void defaultViewPrefersSnapshot() throws Exception {
        Fixture fixture = prepareWithSnapshot();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.get("source").asText()).isEqualTo("SAVED");
        assertThat(data.get("incomeTotal").asLong()).isEqualTo(SNAPSHOT_INCOME);
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(SNAPSHOT_EXPENSE);
        assertThat(data.get("savedAt").isNull()).isFalse();
    }

    @Test
    @DisplayName("#19 view=saved 는 생략과 같은 동작이다")
    void savedViewMatchesDefault() throws Exception {
        Fixture fixture = prepareWithSnapshot();

        JsonNode omitted = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");
        JsonNode explicit = statistics(fixture, FIXED_YEAR, FIXED_MONTH, "saved").get("data");

        // saved 가 명시적 값으로도 존재한다 — 알 수 없는 값으로 보고 거절하면 여기서 걸린다.
        assertThat(explicit.get("source").asText()).isEqualTo(omitted.get("source").asText());
        assertThat(explicit.get("incomeTotal").asLong())
                .isEqualTo(omitted.get("incomeTotal").asLong());
        assertThat(explicit.get("expenseTotal").asLong())
                .isEqualTo(omitted.get("expenseTotal").asLong());
    }

    @Test
    @DisplayName("#20 view=live 는 저장본을 무시하고 즉석 계산한다")
    void liveViewIgnoresSnapshot() throws Exception {
        Fixture fixture = prepareWithSnapshot();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH, "live").get("data");

        assertThat(data.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(30_000L);
        assertThat(data.get("incomeTotal").asLong()).isZero();
    }

    /**
     * #21 — {@code source} 만으로는 부족하다.
     *
     * <p>"저장본이 없다"와 "있는데 지금은 최신을 본다"가 둘 다 {@code CALCULATED} 라,
     * {@code savedAt} 이 있어야 프론트가 "저장본 있음 / 지금 최신"을 나란히 보여준다.
     */
    @Test
    @DisplayName("#21 view=live 인데 저장본이 있으면 savedAt 이 함께 실린다")
    void liveViewCarriesSavedAt() throws Exception {
        Fixture fixture = prepareWithSnapshot();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH, "live").get("data");

        assertThat(data.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(data.has("savedAt")).isTrue();
        assertThat(data.get("savedAt").isNull()).isFalse();
    }

    @Test
    @DisplayName("저장본이 없으면 view=live 의 savedAt 은 null 이다 — 필드는 남는다")
    void liveViewWithoutSnapshotHasNullSavedAt() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH, "live").get("data");

        assertThat(data.get("source").asText()).isEqualTo("CALCULATED");
        assertThat(data.has("savedAt")).isTrue();
        assertThat(data.get("savedAt").isNull()).isTrue();
    }

    /**
     * #22 — <b>{@code view=live} 가 저장본을 "쓰지도" 않는다</b>(SC-507).
     *
     * <p>"최신 값을 봤으니 저장해 두자"는 편의는 넣지 않는다 — 저장은 5.6 의 명시적
     * 행위여야 한다. 조회 뒤 저장본을 다시 확인해 <b>수치와 저장 시각이 그대로인지</b>
     * 본다. 행 수만 세면 갱신한 구현이 통과한다.
     */
    @Test
    @DisplayName("#22 view=live 조회는 저장본을 바꾸지 않는다")
    void liveViewDoesNotWrite() throws Exception {
        Fixture fixture = prepareWithSnapshot();
        OffsetDateTime before = savedAtOf(fixture.member(), FIXED_YEAR, FIXED_MONTH);

        statistics(fixture, FIXED_YEAR, FIXED_MONTH, "live");

        assertThat(countStatistics(fixture.member(), FIXED_YEAR, FIXED_MONTH)).isEqualTo(1);
        assertThat(savedAtOf(fixture.member(), FIXED_YEAR, FIXED_MONTH)).isEqualTo(before);
        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");
        assertThat(data.get("incomeTotal").asLong()).isEqualTo(SNAPSHOT_INCOME);
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(SNAPSHOT_EXPENSE);
    }

    /** 기본 조회도 저장본을 만들지 않는다 — 저장본 없는 달을 열어도 행이 생기지 않는다. */
    @Test
    @DisplayName("기본 조회도 저장본을 만들지 않는다")
    void defaultViewDoesNotCreateSnapshot() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());

        statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(countStatistics(fixture.member(), FIXED_YEAR, FIXED_MONTH)).isZero();
    }

    /**
     * #23 — 모르는 값을 조용히 무시하지 않는다.
     *
     * <p>{@code view=lives} 를 기본 동작으로 넘기면 사용자는 <b>최신값을 본다고 믿는데
     * 저장본을 보고 있다.</b> 화면에는 아무 이상이 없어 알아챌 방법이 없다.
     */
    @Test
    @DisplayName("#23 view 에 saved·live 밖의 값을 주면 3603 이다")
    void rejectsUnknownView() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(statistics(fixture, FIXED_YEAR, FIXED_MONTH, "lives")))
                .isEqualTo(3603);
        assertThat(resCode(statistics(fixture, FIXED_YEAR, FIXED_MONTH, "latest")))
                .isEqualTo(3603);
        assertThat(resCode(statistics(fixture, FIXED_YEAR, FIXED_MONTH, "SAVED_")))
                .isEqualTo(3603);
    }

    /**
     * 값 없이 온 {@code ?view=} 는 생략과 같다.
     *
     * <p>둘 다 "지정하지 않았다"이므로 결과가 갈리면 안 된다.
     */
    @Test
    @DisplayName("view= 로 값 없이 오면 생략과 같다")
    void emptyViewMeansSaved() throws Exception {
        Fixture fixture = prepareWithSnapshot();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH, "").get("data");

        assertThat(data.get("source").asText()).isEqualTo("SAVED");
    }

    @Test
    @DisplayName("저장본은 저장된 주 경계를 그대로 쓴다 — 다시 계산하지 않는다")
    void savedViewKeepsStoredWeekBoundaries() throws Exception {
        Fixture fixture = prepare();
        long statisticsIdx = insertStatistics(fixture.member(), FIXED_YEAR, FIXED_MONTH,
                SNAPSHOT_INCOME, SNAPSHOT_EXPENSE);
        // 규칙대로라면 1주는 07-01~07-05 다. 일부러 다른 경계를 저장해 둔다.
        insertStatisticsWeekly(fixture.member(), statisticsIdx, 1,
                "2026-07-01", "2026-07-07", 123_000L);

        JsonNode week = weekOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH), 1);

        // 저장본을 다시 계산하는 구현이면 여기서 07-05 가 나온다.
        assertThat(week.get("weekEnd").asText()).isEqualTo("2026-07-07");
        assertThat(week.get("amount").asLong()).isEqualTo(123_000L);
    }
}

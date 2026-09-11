package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 목표 대비 상태 — quickstart #30-1 (FR-523).
 *
 * <pre>{@code
 * 사용률 90 미만        → UNDER
 * 사용률 90 이상 110 이하 → OK
 * 사용률 110 초과        → OVER
 * }</pre>
 *
 * <h2>경계 넷을 다 건다</h2>
 *
 * <p>{@code 89.99} · <b>{@code 90.00}</b> · <b>{@code 110.00}</b> · {@code 110.01} 이다.
 * 가운데 둘이 요점 — <b>양쪽 경계가 다 {@code OK}</b> 다. {@code <} 를 {@code <=} 로
 * 잘못 쓰면 정확히 90% 또는 110% 에 걸린 유형이 반대로 분류되는데, <b>응답은 성공이라
 * 조용히 틀린다.</b>
 *
 * <p>목표 10,000원을 기준으로 지출을 맞춰 사용률을 정확한 값으로 만든다 — 나눗셈이
 * 딱 떨어져야 경계를 정확히 짚을 수 있다.
 *
 * <h2>채우지 않으면 저장이 통째로 실패한다</h2>
 *
 * <p>{@code tbl_statistics_expend_group.status} 가 {@code varchar(10) NOT NULL} 이고
 * {@code ck_stat_group_status} 가 이 세 값만 허용한다. 다른 문자열을 넣으면 5.6 에서
 * DB 오류가 {@code 9000} 으로 새어 나간다.
 */
class StatisticsStatusIT extends AbstractStatisticsIT {

    private static final long TARGET = 10_000L;

    /** 목표 10,000원에 그 금액을 쓰고 나온 {@code status}. */
    private String statusFor(long amount) throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), TARGET);
        addExpense(fixture, "2026-07-03", amount, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());
        return row.get("status").asText();
    }

    @Test
    @DisplayName("#30-1 사용률 89.99 는 UNDER 다")
    void justUnderIsUnder() throws Exception {
        // 8,999 / 10,000 = 89.99%
        assertThat(statusFor(8_999L)).isEqualTo("UNDER");
    }

    @Test
    @DisplayName("#30-1 사용률 90.00 은 OK 다 — 경계 포함")
    void exactlyNinetyIsOk() throws Exception {
        assertThat(statusFor(9_000L)).isEqualTo("OK");
    }

    @Test
    @DisplayName("#30-1 사용률 110.00 은 OK 다 — 경계 포함")
    void exactlyOneTenIsOk() throws Exception {
        assertThat(statusFor(11_000L)).isEqualTo("OK");
    }

    @Test
    @DisplayName("#30-1 사용률 110.01 은 OVER 다")
    void justOverIsOver() throws Exception {
        // 11,001 / 10,000 = 110.01%
        assertThat(statusFor(11_001L)).isEqualTo("OVER");
    }

    @Test
    @DisplayName("사용률 100 은 OK 다")
    void exactlyHundredIsOk() throws Exception {
        assertThat(statusFor(10_000L)).isEqualTo("OK");
    }

    /**
     * 목표가 0 이면 사용률이 0 이므로 {@code UNDER} 다.
     *
     * <p>"목표가 없으니 판정할 수 없다"고 {@code null} 이나 빈 문자열을 넣으면 저장에서
     * NOT NULL·CHECK 에 걸린다.
     */
    @Test
    @DisplayName("#30-1 목표가 0 이면 사용률 0 이라 UNDER 다")
    void zeroTargetIsUnder() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 100_000L, fixture.foodGroupId());

        JsonNode row = groupSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.foodGroupId());

        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(row.get("status").asText()).isEqualTo("UNDER");
    }

    /** 세 값 밖의 문자열이 절대 나오지 않는다 — CHECK 제약과 같은 집합이어야 한다. */
    @Test
    @DisplayName("status 는 UNDER·OK·OVER 셋 중 하나뿐이다")
    void statusIsAlwaysOneOfThree() throws Exception {
        Fixture fixture = prepare();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), TARGET);
        long transportGroupId = defaultGroupId(fixture.member(), "교통");
        addExpense(fixture, "2026-07-03", 5_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-07-04", 900_000L, transportGroupId);

        JsonNode summaries = statistics(fixture, FIXED_YEAR, FIXED_MONTH)
                .get("data").get("expendGroupSummaries");

        assertThat(summaries).isNotEmpty();
        for (JsonNode row : summaries) {
            assertThat(row.get("status").asText()).isIn("UNDER", "OK", "OVER");
        }
    }
}

package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 연·월 범위 — quickstart #33·#34 (SC-510 · FR-525).
 *
 * <p>연은 <b>2000~2100</b>, 월은 1~12 다. 벗어나면 {@code 3603} 이다.
 *
 * <h2>연도는 애플리케이션이 판정한다</h2>
 *
 * <p>{@code ck_statistics_month} 는 <b>월만</b> 검사하므로(덤프 확인) DB 가 대신 막아주지
 * 않는다. {@code year=0} 이나 {@code year=999999} 가 그대로 들어와 조회는 빈 결과,
 * 저장은 이상한 행을 남긴다.
 *
 * <h2>다섯 곳에 같은 규칙이 걸린다</h2>
 *
 * <p>5.1 · 5.2 · 5.4 · 5.5 · 5.6 이다. <b>한 곳만 빠뜨리기 쉬워서</b> 여기서 함께 건다 —
 * 통계 시험이지만 목표금액 API 도 같이 확인한다. 상수를 한 곳에 가두지 않고 각 DTO 가
 * 숫자를 직접 적으면 이 시험이 그것을 잡는다.
 *
 * <p><b>현재 연도 기준 상대 범위를 쓰지 않는다</b> — 경계가 해마다 움직이면 이 시험이
 * 시간에 의존한다.
 */
class StatisticsYearMonthIT extends AbstractStatisticsIT {

    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    @Test
    @DisplayName("#33 월에 0 이나 13 을 주면 3603 이다")
    void rejectsMonthOutOfRange() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(statistics(fixture, FIXED_YEAR, 0))).isEqualTo(3603);
        assertThat(resCode(statistics(fixture, FIXED_YEAR, 13))).isEqualTo(3603);
        assertThat(resCode(statistics(fixture, FIXED_YEAR, -1))).isEqualTo(3603);
    }

    @Test
    @DisplayName("#34 연에 1999 나 2101 을 주면 3603 이다")
    void rejectsYearOutOfRange() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(statistics(fixture, MIN_YEAR - 1, FIXED_MONTH))).isEqualTo(3603);
        assertThat(resCode(statistics(fixture, MAX_YEAR + 1, FIXED_MONTH))).isEqualTo(3603);
        assertThat(resCode(statistics(fixture, 0, FIXED_MONTH))).isEqualTo(3603);
        assertThat(resCode(statistics(fixture, 999_999, FIXED_MONTH))).isEqualTo(3603);
    }

    @Test
    @DisplayName("경계 — 2000 과 2100 은 성공이다")
    void acceptsBoundaryYears() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(statistics(fixture, MIN_YEAR, 1))).isEqualTo(200);
        assertThat(resCode(statistics(fixture, MAX_YEAR, 12))).isEqualTo(200);
    }

    /**
     * 5.1 · 5.2 · 5.4 도 같은 규칙이다.
     *
     * <p>상수를 한 곳에 가두지 않고 각 DTO 가 숫자를 직접 적으면 한 곳만 고쳐도 나머지가
     * 갈린다 — 이 시험이 그것을 잡는다.
     */
    @Test
    @DisplayName("목표금액 API 셋도 같은 연 범위를 쓴다")
    void targetApisShareTheSameRange() throws Exception {
        Fixture fixture = prepare();
        String token = fixture.token();
        long groupId = fixture.foodGroupId();

        // 5.1 목록
        assertThat(resCode(getJson("/api/v1/expend-targets?year=1999&month=7&offset=0&limit=10",
                token))).isEqualTo(3603);
        assertThat(resCode(getJson("/api/v1/expend-targets?year=2101&month=7&offset=0&limit=10",
                token))).isEqualTo(3603);

        // 5.2 단건
        assertThat(resCode(getJson("/api/v1/expend-targets/1999/7/" + groupId, token)))
                .isEqualTo(3603);
        assertThat(resCode(getJson("/api/v1/expend-targets/2101/7/" + groupId, token)))
                .isEqualTo(3603);

        // 5.4 월별 저장
        assertThat(resCode(putMonthlyTarget(token, 1999, 7, groupId, 10_000L))).isEqualTo(3603);
        assertThat(resCode(putMonthlyTarget(token, 2101, 7, groupId, 10_000L))).isEqualTo(3603);
    }

    @Test
    @DisplayName("목표금액 API 셋도 경계 2000·2100 을 받는다")
    void targetApisAcceptBoundaryYears() throws Exception {
        Fixture fixture = prepare();
        String token = fixture.token();
        long groupId = fixture.foodGroupId();

        assertThat(resCode(getJson(
                "/api/v1/expend-targets?year=2000&month=1&offset=0&limit=10", token)))
                .isEqualTo(200);
        assertThat(resCode(getJson("/api/v1/expend-targets/2100/12/" + groupId, token)))
                .isEqualTo(200);
        assertThat(resCode(putMonthlyTarget(token, MIN_YEAR, 1, groupId, 10_000L)))
                .isEqualTo(200);
        assertThat(resCode(putMonthlyTarget(token, MAX_YEAR, 12, groupId, 10_000L)))
                .isEqualTo(200);
    }

    /**
     * <b>연·월 오류가 {@code view} 오류보다 먼저 걸린다.</b>
     *
     * <p>둘 다 {@code 3603} 이라 사용자에게 보이는 결과는 같지만, 순서가 뒤집혀 있으면
     * 나중에 코드가 갈릴 때 드러난다.
     */
    @Test
    @DisplayName("연·월과 view 가 둘 다 어긋나도 3603 이다")
    void yearMonthAndViewBothInvalid() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(statistics(fixture, 1999, 13, "lives"))).isEqualTo(3603);
    }
}

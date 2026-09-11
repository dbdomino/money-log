package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 미래 월 거절 — quickstart #45·#46 (FR-527).
 *
 * <p>저장 대상이 서버 기준 현재 연월을 <b>초과</b>하면 {@code 3604} 다.
 *
 * <h2>경계가 "초과"다 — 이번 달은 저장할 수 있다</h2>
 *
 * <p>{@code >=} 로 잡으면 이번 달 저장이 막힌다. 이번 달은 진행 중이지만 실제 데이터가
 * 있어 저장할 가치가 있고, 이후 5.6 을 다시 불러 덮어쓴다.
 *
 * <p>미래를 막는 이유는 <b>저장본이 불변이기</b> 때문이다 — 데이터가 아직 없어 합계 0 인
 * 스냅샷이 굳으면 나중에 그 달 지출이 쌓여도 조회는 계속 0 을 보여준다.
 *
 * <h2>연월을 고정값으로 박지 않는다</h2>
 *
 * <p>"미래 월"의 답이 <b>지금이 언제인가</b>에 달려 있다. 고정값을 박으면 그 날짜가
 * 지나는 순간 "미래 월"이 "지난 달"이 되어 <b>시험이 조용히 반대를 검증한다.</b>
 * 005 의 {@code AbstractSyncIT} 와 같은 처방이다.
 */
class StatisticsFutureMonthIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#45 다음 달 저장 시도는 3604 다")
    void rejectsNextMonth() throws Exception {
        Fixture fixture = prepare();
        YearMonth next = nextMonth();

        assertThat(resCode(save(fixture, next.getYear(), next.getMonthValue())))
                .isEqualTo(3604);
        assertThat(countStatistics(fixture.member(), next.getYear(), next.getMonthValue()))
                .isZero();
    }

    @Test
    @DisplayName("#45 먼 미래도 3604 다")
    void rejectsFarFuture() throws Exception {
        Fixture fixture = prepare();
        YearMonth far = YearMonth.now().plusYears(3);

        assertThat(resCode(save(fixture, far.getYear(), far.getMonthValue()))).isEqualTo(3604);
    }

    /** 연이 같고 월만 큰 경우와, 연이 큰 경우를 함께 건다 — {@code 연 × 12 + 월} 비교다. */
    @Test
    @DisplayName("#45 해가 바뀌는 경계에서도 미래는 3604 다")
    void rejectsAcrossYearBoundary() throws Exception {
        Fixture fixture = prepare();
        YearMonth nextJanuary = YearMonth.of(YearMonth.now().getYear() + 1, 1);

        // 다음 해 1월은 월 숫자만 보면 지금보다 작을 수 있다 — 연을 함께 봐야 걸린다.
        assertThat(resCode(save(fixture, nextJanuary.getYear(), nextJanuary.getMonthValue())))
                .isEqualTo(3604);
    }

    @Test
    @DisplayName("#46 이번 달은 저장할 수 있다 — 초과가 아니다")
    void acceptsCurrentMonth() throws Exception {
        Fixture fixture = prepare();
        YearMonth current = thisMonth();

        assertThat(resCode(save(fixture, current.getYear(), current.getMonthValue())))
                .isEqualTo(200);
        assertThat(countStatistics(fixture.member(), current.getYear(),
                current.getMonthValue())).isEqualTo(1);
    }

    @Test
    @DisplayName("지난 달은 저장할 수 있다")
    void acceptsPastMonth() throws Exception {
        Fixture fixture = prepare();
        YearMonth last = lastMonth();

        assertThat(resCode(save(fixture, last.getYear(), last.getMonthValue())))
                .isEqualTo(200);
    }

    /**
     * <b>연·월 범위({@code 3603})가 미래 판정({@code 3604})보다 먼저다.</b>
     *
     * <p>2101년 1월은 미래이면서 동시에 범위 밖이다. 순서가 뒤집히면 {@code 3604} 가
     * 나가는데, 사용자가 취할 조치는 "허용 범위 안의 연도를 쓴다"이지 "기다린다"가 아니다.
     */
    @Test
    @DisplayName("범위 밖이면서 미래인 연월은 3603 이다 — 3604 가 아니다")
    void rangeCheckComesFirst() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(save(fixture, 2101, 1))).isEqualTo(3603);
    }
}

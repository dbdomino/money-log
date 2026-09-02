package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.repository.UserRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 통계 스냅샷의 그 달 1건 강제 — quickstart.md §3 시나리오 #14·#19.
 *
 * <p>확인하려는 것: 한 회원의 한 연·월에 통계 행이 <b>둘 이상 생길 수 없는가</b>다
 * (FR-074). 재저장할 때마다 행이 쌓이면 어느 것이 최신인지 판단이 붙고, 조회가
 * "가장 나중 것"을 고르는 규칙에 의존하게 된다.
 *
 * <p>재저장은 새 행이 아니라 <b>기존 행의 UPDATE</b>다. {@code savedAt}이 갱신되고
 * 나머지 값도 새로 계산된 것으로 바뀌는지 함께 본다.
 *
 * <p>저장 시점의 값이 보존되는지(FR-075)는 여기서 보지 않는다 — 그것은 원본을 고쳐도
 * 이 행이 변하지 않는다는 뜻이고, 통계 테이블에 원본으로 가는 FK 자체가 없어
 * 구조적으로 성립한다. {@link StatisticsBrokenRefIT}가 그 점을 본다.
 */
class StatisticsUniqueIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatisticsRepository statisticsRepository;

    @Test
    @DisplayName("#14 같은 회원의 같은 연·월 통계를 두 번 만들면 두 번째가 거부된다")
    void rejectsDuplicateStatisticsForSameMonth() {
        User user = inTx(() -> userRepository.save(newUser()));

        inTx(() -> statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)));

        assertViolatesConstraint(() ->
                statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)),
                "ux_statistics");
    }

    @Test
    @DisplayName("#14 회원이 다르면 같은 연·월 통계를 각자 가질 수 있다")
    void allowsSameMonthAcrossDifferentUsers() {
        User first = inTx(() -> userRepository.save(newUser()));
        User second = inTx(() -> userRepository.save(newUser()));

        inTx(() -> statisticsRepository.saveAndFlush(newStatistics(first, 2026, 12)));
        inTx(() -> statisticsRepository.saveAndFlush(newStatistics(second, 2026, 12)));

        assertThat(inTx(() -> statisticsRepository.findByUserIdKeyAndYearAndMonth(
                first.getIdKey(), 2026, 12))).isPresent();
        assertThat(inTx(() -> statisticsRepository.findByUserIdKeyAndYearAndMonth(
                second.getIdKey(), 2026, 12))).isPresent();
    }

    @Test
    @DisplayName("재저장은 행을 늘리지 않고 기존 행과 savedAt 을 갱신한다")
    void resaveUpdatesInPlace() {
        User user = inTx(() -> userRepository.save(newUser()));

        UserStatistics first = inTx(() ->
                statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)));
        OffsetDateTime firstSavedAt = first.getSavedAt();

        inTx(() -> {
            UserStatistics target = statisticsRepository
                    .findByUserIdKeyAndYearAndMonth(user.getIdKey(), 2026, 12).orElseThrow();
            target.setSavedAt(firstSavedAt.plusHours(1));
            target.setExpenseTotal(1_500_000L);
            target.setFixedAmount(500_000L);
            target.setRegularAmount(1_000_000L);
            target.setFixedPercent(new BigDecimal("33.33"));
            target.setRegularPercent(new BigDecimal("66.67"));
            statisticsRepository.saveAndFlush(target);
        });

        UserStatistics found = inTx(() -> statisticsRepository
                .findByUserIdKeyAndYearAndMonth(user.getIdKey(), 2026, 12).orElseThrow());

        assertThat(found.getIdx()).isEqualTo(first.getIdx());
        assertThat(found.getExpenseTotal()).isEqualTo(1_500_000L);
        assertThat(found.getSavedAt()).isAfter(firstSavedAt);
        // NUMERIC(5,2) 라 소수점 둘째 자리까지 그대로 돌아온다
        assertThat(found.getFixedPercent()).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(found.getRegularPercent()).isEqualByComparingTo(new BigDecimal("66.67"));
    }

    @Test
    @DisplayName("#19 월 13인 통계는 CHECK 제약에 막힌다")
    void rejectsMonthOutOfRange() {
        User user = inTx(() -> userRepository.save(newUser()));

        assertViolatesConstraint(() -> {
            UserStatistics statistics = newStatistics(user, 2026, 12);
            statistics.setMonth(13);
            statisticsRepository.saveAndFlush(statistics);
        }, "ck_statistics_month");
    }
}

package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.repository.UserRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsWeeklyRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 통계 스냅샷 삭제의 파급 — quickstart.md §3 시나리오 #16.
 *
 * <p>확인하려는 것: 스냅샷을 지우면 상세 3종이 <b>모두</b> 함께 사라지는가다.
 * 상세만 남으면 어느 달 통계인지 알 수 없는 고아 행이 된다.
 *
 * <p>Hibernate는 {@code @JoinColumn}만으로 {@code ON DELETE CASCADE}를 DDL에 내지
 * 않아 상세 3종 모두에 {@code @OnDelete}가 필요하다. 셋 중 하나만 빠져도 그 테이블만
 * 조용히 남으므로 <b>세 테이블을 각각</b> 확인한다.
 *
 * <p>재저장(FR-074)도 상세를 지웠다 다시 넣는다. 그쪽은 부모 행을 남긴 채 상세만
 * 갈아 끼우므로 CASCADE가 아니라 Repository의 일괄 삭제를 쓴다 — 그 경로도 함께 본다.
 */
class StatisticsCascadeIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserStatisticsRepository statisticsRepository;

    @Autowired
    private UserStatisticsWeeklyRepository weeklyRepository;

    @Autowired
    private UserStatisticsExpendGroupRepository statExpendGroupRepository;

    @Autowired
    private UserStatisticsPaymentMethodRepository statPaymentMethodRepository;

    @Test
    @DisplayName("#16 통계 스냅샷을 지우면 상세 3종이 모두 함께 사라진다")
    void deletingSnapshotRemovesAllThreeDetailKinds() {
        UserStatistics statistics = givenSnapshotWithDetails();

        assertThat(countWeekly(statistics)).isEqualTo(2);
        assertThat(countExpendGroup(statistics)).isEqualTo(2);
        assertThat(countPaymentMethod(statistics)).isEqualTo(2);

        inTx(() -> statisticsRepository.deleteById(statistics.getIdx()));

        assertThat(inTx(() -> statisticsRepository.findById(statistics.getIdx()))).isEmpty();
        assertThat(countWeekly(statistics)).isZero();
        assertThat(countExpendGroup(statistics)).isZero();
        assertThat(countPaymentMethod(statistics)).isZero();
    }

    @Test
    @DisplayName("#16 한 스냅샷을 지워도 다른 달 스냅샷의 상세는 남는다")
    void deletingOneSnapshotLeavesOtherMonthsIntact() {
        UserStatistics december = givenSnapshotWithDetails();
        User user = december.getUser();

        UserStatistics january = inTx(() ->
                statisticsRepository.saveAndFlush(newStatistics(user, 2027, 1)));
        inTx(() -> weeklyRepository.saveAndFlush(newStatisticsWeekly(
                january, 1, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3))));

        inTx(() -> statisticsRepository.deleteById(december.getIdx()));

        assertThat(countWeekly(december)).isZero();
        assertThat(countWeekly(january)).isEqualTo(1);
    }

    @Test
    @DisplayName("재저장 경로는 부모를 남긴 채 상세만 갈아 끼운다")
    void resaveReplacesDetailsWithoutTouchingParent() {
        UserStatistics statistics = givenSnapshotWithDetails();

        long removed = inTx(() -> weeklyRepository.deleteByStatisticsIdx(statistics.getIdx())
                + statExpendGroupRepository.deleteByStatisticsIdx(statistics.getIdx())
                + statPaymentMethodRepository.deleteByStatisticsIdx(statistics.getIdx()));

        assertThat(removed).isEqualTo(6);
        // 부모는 그대로다 — 재저장은 새 스냅샷을 만들지 않는다(FR-074)
        assertThat(inTx(() -> statisticsRepository.findById(statistics.getIdx()))).isPresent();

        inTx(() -> weeklyRepository.saveAndFlush(newStatisticsWeekly(
                statistics, 1, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 6))));

        assertThat(countWeekly(statistics)).isEqualTo(1);
    }

    /** 스냅샷 1건과 상세 3종을 각각 2행씩 저장하고 스냅샷을 돌려준다. */
    private UserStatistics givenSnapshotWithDetails() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserStatistics statistics = inTx(() ->
                statisticsRepository.saveAndFlush(newStatistics(user, 2026, 12)));

        inTx(() -> {
            weeklyRepository.saveAndFlush(newStatisticsWeekly(
                    statistics, 1, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 6)));
            weeklyRepository.saveAndFlush(newStatisticsWeekly(
                    statistics, 2, LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 13)));

            statExpendGroupRepository.saveAndFlush(
                    newStatisticsExpendGroup(statistics, 9001L, "식비"));
            statExpendGroupRepository.saveAndFlush(
                    newStatisticsExpendGroup(statistics, 9002L, "교통"));

            statPaymentMethodRepository.saveAndFlush(
                    newStatisticsPaymentMethod(statistics, 8001L, "국민카드"));
            statPaymentMethodRepository.saveAndFlush(
                    newStatisticsPaymentMethod(statistics, 8002L, "월급통장"));
        });

        return statistics;
    }

    private int countWeekly(UserStatistics statistics) {
        return countBySnapshot("tbl_statistics_weekly", statistics);
    }

    private int countExpendGroup(UserStatistics statistics) {
        return countBySnapshot("tbl_statistics_expend_group", statistics);
    }

    private int countPaymentMethod(UserStatistics statistics) {
        return countBySnapshot("tbl_statistics_payment_method", statistics);
    }

    /**
     * 상세 행 수를 센다.
     *
     * <p>Repository가 아니라 {@code JdbcTemplate}으로 세는 이유는, CASCADE 가 실제로
     * <b>DB에서</b> 일어났는지를 보려는 것이기 때문이다. 영속성 컨텍스트를 거치면
     * Hibernate가 지운 것과 DB가 지운 것을 구분할 수 없다.
     *
     * <p>{@code tableName}은 위 세 메서드가 넘기는 리터럴뿐이라 외부 값이 들어올 자리가
     * 없다. 테이블명은 바인딩할 수 없어 이어 붙이는 것이 불가피하다.
     */
    private int countBySnapshot(String tableName, UserStatistics statistics) {
        return inTx(() -> jdbc.queryForObject(
                "SELECT count(*) FROM " + tableName + " WHERE statistics_idx = ?",
                Integer.class, statistics.getIdx()));
    }
}

package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserStatisticsWeekly;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 통계 주별 지출 조회·삭제 — {@code tbl_user_statistics_weekly}.
 *
 * <p>재저장이 "상세를 지웠다 다시 넣는" 방식이라 일괄 삭제가 필요하다. 부모를 지우면
 * FK CASCADE가 알아서 지우지만, 재저장은 부모 행을 남긴 채 상세만 갈아 끼운다.
 */
public interface UserStatisticsWeeklyRepository extends JpaRepository<UserStatisticsWeekly, Long> {

    /** 한 스냅샷의 주별 행을 주차 순으로 읽는다. */
    List<UserStatisticsWeekly> findByStatisticsIdxOrderByWeekIndexAsc(Long statisticsIdx);

    /** 재저장 전에 한 스냅샷의 주별 행을 모두 지운다. 지워진 행 수를 돌려준다. */
    long deleteByStatisticsIdx(Long statisticsIdx);
}

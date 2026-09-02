package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserStatistics;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 월별 통계 스냅샷 조회 — {@code tbl_user_statistics}.
 *
 * <p>저장본이 있는지가 응답의 {@code source}를 가른다 — 있으면 {@code SAVED}(FR-079).
 * {@code view=live} 요청은 이 테이블에 쓰지 않고, 저장본이 있을 때 {@code savedAt}만
 * 꺼내 쓴다.
 */
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    /**
     * 그 달 저장본. 유일 제약 {@code ux_user_statistics}와 같은 조합이라 최대 1건이다.
     *
     * <p>재저장은 이 행을 찾아 UPDATE하고 상세 3종을 지웠다 다시 넣는다. 새 행을
     * 만들지 않는다(FR-074).
     */
    Optional<UserStatistics> findByUserIdKeyAndYearAndMonth(Long idKey, int year, int month);
}

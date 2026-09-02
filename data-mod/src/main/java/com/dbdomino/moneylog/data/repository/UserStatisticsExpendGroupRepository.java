package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserStatisticsExpendGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 통계 지출유형별 요약 조회·삭제 — {@code tbl_user_statistics_expend_group}.
 *
 * <p>{@code expendGroupIdx}는 FK 없는 값이라(FR-078a) 실재하지 않는 유형을 가리킬 수
 * 있다. 이 인터페이스는 그 값으로 원본을 되짚지 않는다 — 화면 복원은 함께 저장된
 * 이름이 맡는다.
 */
public interface UserStatisticsExpendGroupRepository
        extends JpaRepository<UserStatisticsExpendGroup, Long> {

    /** 한 스냅샷의 유형별 행 전체. */
    List<UserStatisticsExpendGroup> findByStatisticsIdxOrderByIdxAsc(Long statisticsIdx);

    /** 재저장 전에 한 스냅샷의 유형별 행을 모두 지운다. 지워진 행 수를 돌려준다. */
    long deleteByStatisticsIdx(Long statisticsIdx);
}

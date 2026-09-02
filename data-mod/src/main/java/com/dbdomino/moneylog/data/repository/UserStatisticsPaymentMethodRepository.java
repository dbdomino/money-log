package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserStatisticsPaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 통계 수단별 요약 조회·삭제 — {@code tbl_statistics_payment_method}.
 *
 * <p>{@code paymentMethodIdx}도 FK 없는 값이다(FR-078a). 유형별 요약과 달리 지출이
 * 0원인 수단도 행으로 들어 있다(FR-076).
 */
public interface UserStatisticsPaymentMethodRepository
        extends JpaRepository<UserStatisticsPaymentMethod, Long> {

    /** 한 스냅샷의 수단별 행 전체. */
    List<UserStatisticsPaymentMethod> findByStatisticsIdxOrderByIdxAsc(Long statisticsIdx);

    /** 재저장 전에 한 스냅샷의 수단별 행을 모두 지운다. 지워진 행 수를 돌려준다. */
    long deleteByStatisticsIdx(Long statisticsIdx);
}

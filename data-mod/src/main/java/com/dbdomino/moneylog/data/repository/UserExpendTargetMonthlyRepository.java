package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpendTargetMonthly;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 월별 목표금액 조회 — {@code tbl_user_expend_target_monthly}.
 *
 * <p><b>행이 없는 것과 {@code targetAmount = 0}은 다른 상태다</b>(FR-073). 그래서
 * 조회가 {@code Optional}을 돌려주고, 비어 있음을 0으로 접지 않는다 — 응답
 * {@code monthlyTargetAmount}는 비어 있으면 {@code null}이다.
 */
public interface UserExpendTargetMonthlyRepository
        extends JpaRepository<UserExpendTargetMonthly, Long> {

    /** 그 달 목표 목록(5.3) — 통계가 적용 금액을 정할 때 한 번에 읽는다. */
    List<UserExpendTargetMonthly> findByUserIdKeyAndYearAndMonth(Long idKey, int year, int month);

    /**
     * upsert 판정용 단건 조회. 유일 제약 {@code ux_user_target_monthly}와 같은 조합이다.
     *
     * <p>비어 있음이 곧 "그 달은 따로 정하지 않았다"이며, 이때 통계는 기본 목표금액을
     * 적용한다.
     */
    Optional<UserExpendTargetMonthly> findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
            Long idKey, int year, int month, Long expendGroupIdx);
}

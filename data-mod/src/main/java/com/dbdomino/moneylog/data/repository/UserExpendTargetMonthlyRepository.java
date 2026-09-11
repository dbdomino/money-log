package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpendTargetMonthly;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 월별 목표금액 조회 — {@code tbl_expend_target_monthly}.
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
     * upsert 판정용 단건 조회. 유일 제약 {@code ux_target_monthly}와 같은 조합이다.
     *
     * <p>비어 있음이 곧 "그 달은 따로 정하지 않았다"이며, 이때 통계는 기본 목표금액을
     * 적용한다.
     */
    Optional<UserExpendTargetMonthly> findByUserIdKeyAndYearAndMonthAndExpendGroupIdx(
            Long idKey, int year, int month, Long expendGroupIdx);

    /**
     * 월별 목표 upsert — <b>있으면 갱신하고 없으면 만든다</b>(FR-512).
     *
     * <p>규칙과 이유는 {@code UserExpendTargetDefaultRepository.upsert}와 같다. 다른
     * 것은 충돌 대상뿐이며 여기는 {@code ux_target_monthly (id_key, year, month,
     * expend_group_idx)}다.
     *
     * <p><b>{@code targetAmount = 0}도 그대로 저장한다.</b> 0은 "그 달엔 쓰지 않겠다"라
     * 행이 없는 상태와 다르다(FR-506) — 0을 "지우기"로 해석해 DELETE로 바꾸면 그 구분이
     * 사라진다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tbl_expend_target_monthly
                (id_key, year, month, expend_group_idx, target_amount,
                 created_at, updated_at, created_by, updated_by)
            VALUES
                (:idKey, :year, :month, :expendGroupIdx, :targetAmount,
                 now(), now(), :auditorIdKey, :auditorIdKey)
            ON CONFLICT ON CONSTRAINT ux_target_monthly DO UPDATE
               SET target_amount = EXCLUDED.target_amount,
                   updated_at = now(),
                   updated_by = EXCLUDED.updated_by
            """, nativeQuery = true)
    int upsert(@Param("idKey") Long idKey,
               @Param("year") int year,
               @Param("month") int month,
               @Param("expendGroupIdx") Long expendGroupIdx,
               @Param("targetAmount") Long targetAmount,
               @Param("auditorIdKey") Long auditorIdKey);
}

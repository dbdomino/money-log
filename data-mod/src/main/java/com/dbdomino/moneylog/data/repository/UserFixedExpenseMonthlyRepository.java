package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 월별 고정지출 내역 조회·생성 — {@code tbl_user_fixed_expense_monthly}.
 *
 * <p>이 저장 단위는 조회가 <b>쓰기를 일으킨다</b>. 그 연·월을 처음 볼 때 설정에서
 * 복사해 만들기 때문이다(lazy 생성, FR-054). 그래서 여기에 INSERT가 들어 있다.
 */
public interface UserFixedExpenseMonthlyRepository
        extends JpaRepository<UserFixedExpenseMonthly, Long> {

    /** 그 달 내역 목록(4.5·4.8). {@code ix_user_fixed_monthly_ym}을 탄다. */
    List<UserFixedExpenseMonthly> findByUserIdKeyAndYearAndMonth(Long idKey, int year, int month);

    /** 한 고정지출의 그 달 내역. 이미 만들어졌는지 확인하는 데 쓴다. */
    Optional<UserFixedExpenseMonthly> findByFixedExpenseIdxAndYearAndMonth(
            Long fixedExpenseIdx, int year, int month);

    /**
     * 수동 재작성(FR-060) — 지정한 연·월에서 주어진 고정지출들의 행을 지운다.
     *
     * <p>적용 기간이 그 연·월을 더는 포함하지 않게 된 행을 걷어내는 용도다
     * ({@code deletedCount}). 재작성은 한 트랜잭션 안에서 일어나므로 이 삭제와
     * 뒤이은 INSERT/UPDATE가 함께 커밋되거나 함께 되돌아간다.
     */
    long deleteByFixedExpenseIdxInAndYearAndMonth(
            Collection<Long> fixedExpenseIdxes, int year, int month);

    /**
     * 그 달 내역을 만든다 — <b>이미 있으면 아무 일도 하지 않는다</b>(FR-054).
     *
     * <p>네이티브 쿼리인 이유는 {@code ON CONFLICT DO NOTHING}이 JPQL에 없기
     * 때문이다. "조회해서 없으면 INSERT"로는 부족하다 — 월별 내역 조회(4.5)와 월별
     * 가계부 목록(4.8)이 동시에 열리면 두 트랜잭션이 같은 순간에 "없음"을 보고 둘 다
     * INSERT를 시도한다. 유일 제약이 두 번째를 막긴 하지만 그쪽은 예외로 끝나
     * 사용자 화면이 실패한다. 충돌을 <b>정상 흐름으로</b> 흡수하려면 DB 쪽 구문이
     * 필요하다.
     *
     * <p>반환값은 실제로 들어간 행 수다 — 새로 만들었으면 1, 이미 있었으면 0.
     * 수동 재작성의 {@code createdCount}가 이 값을 센다.
     *
     * <p>감사 컬럼을 직접 채운다. {@code AuditingEntityListener}는 Entity를 거칠 때만
     * 동작하는데 이 경로는 Entity를 만들지 않는다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tbl_user_fixed_expense_monthly
                (id_key, fixed_expense_idx, year, month, amount, payment_date, content,
                 payment_method_idx, expend_group_idx, modified,
                 created_at, updated_at, created_by, updated_by)
            VALUES
                (:idKey, :fixedExpenseIdx, :year, :month, :amount, :paymentDate, :content,
                 :paymentMethodIdx, :expendGroupIdx, false,
                 now(), now(), :auditorIdKey, :auditorIdKey)
            ON CONFLICT (fixed_expense_idx, year, month) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("idKey") Long idKey,
                       @Param("fixedExpenseIdx") Long fixedExpenseIdx,
                       @Param("year") int year,
                       @Param("month") int month,
                       @Param("amount") Long amount,
                       @Param("paymentDate") LocalDate paymentDate,
                       @Param("content") String content,
                       @Param("paymentMethodIdx") Long paymentMethodIdx,
                       @Param("expendGroupIdx") Long expendGroupIdx,
                       @Param("auditorIdKey") Long auditorIdKey);
}

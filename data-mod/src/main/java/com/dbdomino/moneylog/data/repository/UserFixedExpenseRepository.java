package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 고정지출 관리 조회 — {@code tbl_fixed_expense}.
 *
 * <p>주 용도는 "이 연·월에 적용되는 고정지출은 무엇인가"다. 월별 내역을 만들 때
 * (FR-054·056)와 수동 재작성(FR-060)이 같은 질문을 한다.
 */
public interface UserFixedExpenseRepository extends JpaRepository<UserFixedExpense, Long> {

    /** 관리 목록 — 본인 고정지출 전체. 고정지출은 삭제 표시가 아니라 물리 삭제다. */
    List<UserFixedExpense> findByUserIdKeyOrderByIdxAsc(Long idKey);

    /**
     * 관리 목록(4.2)의 한 페이지. {@code totalCount} 는 {@code Page#getTotalElements()} 다.
     *
     * <p>{@code offset}·{@code limit} 을 {@code PageRequest.of(offset / limit, limit)} 으로
     * 옮긴다. {@code offset} 이 {@code limit} 의 배수임을 {@code FixedExpenseListQuery} 가
     * 이미 검증했으므로({@code 9001}) 나누어떨어진다 — 002 의 관리자 목록(1.13)과 같은 방식이다.
     *
     * <p>정렬은 호출자가 {@code Pageable} 에 담는다. 목록이 페이지마다 흔들리지 않으려면
     * <b>결정적인 정렬</b>이 필요하고 {@code idx} 오름차순이 그것이다.
     */
    Page<UserFixedExpense> findByUserIdKey(Long idKey, Pageable pageable);

    /**
     * 적용 기간이 주어진 연·월을 포함하는 고정지출(FR-056). 양 끝을 포함한다.
     *
     * <p>연과 월을 따로 비교하지 않고 {@code year * 12 + month} 합성값 하나로 본다.
     * 따로 비교하면 "2026년 11월 ~ 2027년 2월"처럼 해를 넘기는 구간에서 조건이
     * 어긋난다 — 2027년 1월은 시작 월(11)보다 작아 빠져 버린다.
     *
     * <p>{@code :yearMonth}는 {@link UserFixedExpense#yearMonthValue(int, int)}가 만든
     * 값이다. 호출부가 직접 곱셈을 적으면 CHECK {@code ck_fixed_expense_period}와
     * 식이 갈라질 수 있어 한 곳에서만 계산한다.
     */
    @Query("select f from UserFixedExpense f "
            + "where f.user.idKey = :idKey "
            + "and (f.startYear * 12 + f.startMonth) <= :yearMonth "
            + "and (f.endYear * 12 + f.endMonth) >= :yearMonth "
            + "order by f.idx asc")
    List<UserFixedExpense> findApplicableTo(@Param("idKey") Long idKey,
                                            @Param("yearMonth") int yearMonth);

    /** 소유자 확인을 겸한 단건 조회. */
    Optional<UserFixedExpense> findByIdxAndUserIdKey(Long idx, Long idKey);

    /**
     * 이 수단을 쓴 고정지출 관리이 하나라도 있는가. 수단의 {@code purpose} 변경 판정({@code 3005})이 쓴다.
     *
     * <p><b>{@code count} 가 아니라 {@code exists} 다.</b> 필요한 답이 "있느냐"인데 세면 전
     * 행을 훑는다. 참조 검사는 네 테이블 전부를 봐야 하므로(FR-205) 네 곳에 같은 메서드가 있다.
     */
    boolean existsByPaymentMethodIdx(Long paymentMethodIdx);
}

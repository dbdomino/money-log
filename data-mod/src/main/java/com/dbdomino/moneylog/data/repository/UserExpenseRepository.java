package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpense;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 지출 내역 조회·삭제 — {@code tbl_expense}.
 *
 * <p>소유자 조건이 {@code userIdKey}로 나가는 것은 소유자를 {@code @ManyToOne} 연관으로
 * 매핑했기 때문이다 — 컬럼은 규칙대로 {@code id_key} 하나다.
 */
public interface UserExpenseRepository extends JpaRepository<UserExpense, Long> {

    /**
     * 월별 가계부 목록(005 의 4.8)·통계 집계(006 의 5.5)가 쓰는 기간 조회. 양 끝을 포함한다.
     *
     * <p>연·월이 아니라 날짜 범위로 받는다. 저장 구조가 연·월을 따로 갖지 않고
     * {@code payment_date} 하나만 갖기 때문이다(data-model.md §6). 호출부가 그 달의
     * 1일~말일을 만들어 넘긴다. {@code ix_expense_date}가 이 조합을 덮는다.
     */
    List<UserExpense> findByUserIdKeyAndPaymentDateBetween(Long idKey, LocalDate from, LocalDate to);

    /**
     * 한 할부의 전 회차를 순번대로 읽는다. <b>소유자 조건이 없다.</b>
     *
     * <p>그래서 API 요청 처리에는 쓰지 않는다 — 남의 그룹까지 집어온다. 요청이 준
     * {@code installmentGroupId} 를 다룰 때는
     * {@link #findByInstallmentGroupIdAndUserIdKeyOrderByInstallmentIndexAsc} 를 쓴다.
     */
    List<UserExpense> findByInstallmentGroupIdOrderByInstallmentIndexAsc(Long installmentGroupId);

    /**
     * 중도상환(3.6)이 쓰는 그룹 조회 — <b>소유자까지 함께 건다</b>.
     *
     * <p>{@code installmentGroupId} 는 요청이 주는 값이라 남의 그룹을 가리킬 수 있다.
     * 없는 그룹과 남의 그룹을 같은 {@code 3206} 으로 묶으려면(api-contract.md §1) 조회
     * 자체에 회원을 걸어야 한다 — 먼저 꺼내 놓고 비교하는 방식은 비교를 빠뜨린 자리가
     * 곧 구멍이 된다.
     */
    List<UserExpense> findByInstallmentGroupIdAndUserIdKeyOrderByInstallmentIndexAsc(
            Long installmentGroupId, Long idKey);

    /**
     * 중도상환(3.6)에서 <b>지우기 전에</b> 남은 회차를 센다 — {@code 3207} 판정용.
     *
     * <p>삭제 결과가 0건인 것을 보고 판정할 수도 있지만, 그러면 "지울 게 없었다"를
     * 확인하려고 DELETE 를 먼저 실행하게 된다. 세는 쿼리를 따로 두어 <b>판정과 삭제의
     * 순서</b>를 계약대로 지킨다(FR-315).
     *
     * <p>경계는 {@code deleteBy...After} 와 <b>같아야 한다</b>. 둘이 갈리면 "0건이라
     * 3207 을 냈는데 실제로는 지울 게 있었다"가 된다.
     */
    long countByInstallmentGroupIdAndPaymentDateAfter(Long installmentGroupId, LocalDate baseDate);

    /**
     * 중도상환(3.6) — 결제일이 기준일보다 <b>뒤인</b> 회차만 지운다(FR-315).
     *
     * <p>기준일 당일과 그 이전 회차는 이미 결제된 것이라 남긴다. 경계가
     * {@code >=}가 되면 오늘 결제된 회차까지 사라져 <b>이번 달 합계가 소급해 줄어든다</b> —
     * 사용자가 이미 본 숫자가 바뀐다.
     *
     * <p>파생 삭제라 대상 행을 먼저 읽어 온 뒤 건별로 지운다. 할부 최대 개월 수가
     * 크지 않아 문제되지 않고, 영속성 컨텍스트와 상태가 어긋나지 않는 이점이 있다.
     * 반환값은 실제로 지워진 회차 수다.
     */
    long deleteByInstallmentGroupIdAndPaymentDateAfter(Long installmentGroupId, LocalDate baseDate);

    /**
     * 할부 그룹 식별자를 채번한다(FR-312). <b>그룹당 한 번만</b> 부른다 — 행마다 부르면
     * 그룹이 흩어져 중도상환이 아무것도 찾지 못한다.
     *
     * <p>{@code @GeneratedValue} 로 쓸 수 없다. {@code installment_group_id} 는
     * {@code tbl_expense} 의 PK 가 아니라 <b>N개 행이 공유하는 일반 컬럼</b>이라 어느
     * Entity 의 식별자 생성기도 아니다 — 001 이 이 시퀀스를
     * {@code AdditionalMappingContributor} 로 따로 만든 이유가 그것이다.
     *
     * <p>스키마 이름을 문자열로 적는다. 001 의 {@code MoneylogSchemaContributor} 는 DDL
     * 기여 시점에 스키마 이름이 비어 있어 {@code ${schema}} 자리표시자를 써야 했지만,
     * 이건 <b>런타임 조회</b>라 그 문제가 없다.
     */
    @Query(value = "select nextval('moneylog.seq_installment_group')", nativeQuery = true)
    Long nextInstallmentGroupId();

    /**
     * 지출유형 삭제 가능 판정({@code 3106}, 003 의 2.12) — 그 유형을 쓴 지출이 한 건이라도 있는가.
     *
     * <p>지출유형 삭제가 DELETE가 아니라 삭제 표시(UPDATE)라 FK RESTRICT가 대신
     * 막아주지 못한다. 애플리케이션이 이 검사로 대신한다(003 FR-211).
     */
    boolean existsByExpendGroupIdx(Long expendGroupIdx);

    /**
     * 이 수단을 쓴 지출이 하나라도 있는가. 수단의 {@code purpose} 변경 판정({@code 3005})이 쓴다.
     *
     * <p><b>{@code count} 가 아니라 {@code exists} 다.</b> 필요한 답이 "있느냐"인데 세면 전
     * 행을 훑고, 오래 쓴 회원일수록 느려진다.
     *
     * <p>참조 검사는 <b>네 테이블 전부</b>를 본다(003 FR-205) — 지출·소득·고정지출·월별 고정지출.
     * 하나라도 빠뜨리면 "소득 수단으로 낸 지출"이 만들어져 월별 집계와 통계 수단별 요약이
     * 어긋난다.
     */
    boolean existsByPaymentMethodIdx(Long paymentMethodIdx);

    /** 소유자 확인을 겸한 단건 조회. 남의 지출을 집어오지 않도록 회원까지 함께 건다. */
    Optional<UserExpense> findByIdxAndUserIdKey(Long idx, Long idKey);
}

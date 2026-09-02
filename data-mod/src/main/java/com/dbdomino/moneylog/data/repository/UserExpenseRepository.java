package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpense;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지출 내역 조회·삭제 — {@code tbl_expense}.
 *
 * <p>소유자 조건이 {@code userIdKey}로 나가는 것은 소유자를 {@code @ManyToOne} 연관으로
 * 매핑했기 때문이다 — 컬럼은 규칙대로 {@code id_key} 하나다.
 */
public interface UserExpenseRepository extends JpaRepository<UserExpense, Long> {

    /**
     * 월별 가계부 목록(4.8)·통계 집계(5.5)가 쓰는 기간 조회. 양 끝을 포함한다.
     *
     * <p>연·월이 아니라 날짜 범위로 받는다. 저장 구조가 연·월을 따로 갖지 않고
     * {@code payment_date} 하나만 갖기 때문이다(data-model.md §6). 호출부가 그 달의
     * 1일~말일을 만들어 넘긴다. {@code ix_expense_date}가 이 조합을 덮는다.
     */
    List<UserExpense> findByUserIdKeyAndPaymentDateBetween(Long idKey, LocalDate from, LocalDate to);

    /** 한 할부의 전 회차를 순번대로 읽는다(3.6). */
    List<UserExpense> findByInstallmentGroupIdOrderByInstallmentIndexAsc(Long installmentGroupId);

    /**
     * 중도상환(3.7) — 결제일이 기준일보다 <b>뒤인</b> 회차만 지운다(FR-045).
     *
     * <p>기준일 당일과 그 이전 회차는 이미 결제된 것이라 남긴다. 경계가
     * {@code >=}가 되면 오늘 결제된 회차까지 사라져 가계부 금액이 틀어진다.
     *
     * <p>파생 삭제라 대상 행을 먼저 읽어 온 뒤 건별로 지운다. 할부 최대 개월 수가
     * 크지 않아 문제되지 않고, 영속성 컨텍스트와 상태가 어긋나지 않는 이점이 있다.
     * 반환값은 실제로 지워진 회차 수다.
     */
    long deleteByInstallmentGroupIdAndPaymentDateAfter(Long installmentGroupId, LocalDate baseDate);

    /**
     * 지출유형 삭제 가능 판정({@code 3106}) — 그 유형을 쓴 지출이 한 건이라도 있는가.
     *
     * <p>지출유형 삭제가 DELETE가 아니라 삭제 표시(UPDATE)라 FK RESTRICT가 대신
     * 막아주지 못한다. 애플리케이션이 이 검사로 대신한다(FR-037).
     */
    boolean existsByExpendGroupIdx(Long expendGroupIdx);

    /** 소유자 확인을 겸한 단건 조회. 남의 지출을 집어오지 않도록 회원까지 함께 건다. */
    Optional<UserExpense> findByIdxAndUserIdKey(Long idx, Long idKey);
}

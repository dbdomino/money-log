package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 월별 통계 스냅샷(머리) — {@code tbl_statistics}.
 *
 * <p><b>저장 시점의 계산 결과를 그대로 보존한다</b>(FR-075). 이후 그 달의 지출을
 * 고치거나 지워도 이 행은 변하지 않는다. 통계는 "그때 이렇게 보였다"는 기록이지
 * 원본을 다시 집계하는 뷰가 아니다.
 *
 * <p>이 행이 있다는 것 자체가 응답의 {@code source: SAVED}를 뜻한다(FR-079).
 * {@code view=live} 요청은 이 테이블을 <b>읽지도 쓰지도 않고</b> 즉석 계산만 한다 —
 * 다만 저장본이 있으면 {@code savedAt}만 꺼내 응답에 싣는다.
 *
 * <p>재저장은 행을 늘리지 않는다(FR-074). 이 행을 UPDATE하고 {@code savedAt}을
 * 갱신하며, 상세 3종은 지웠다 다시 넣는다. 유일 제약 {@code ux_statistics}가
 * 그 규칙을 DB에서 강제한다 — 없으면 재저장할 때마다 같은 달 통계가 쌓여 어느
 * 것이 최신인지 판단이 붙는다.
 *
 * <p>비율 2개는 {@code NUMERIC(5,2)}다. 0.00~100.00을 소수점 둘째 자리까지 담는다.
 * 부동소수로 두지 않는 것은 저장한 값과 읽은 값이 달라지는 일을 막기 위해서다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §12</a>
 */
@Entity
@Table(
        name = "tbl_statistics",
        comment = "월별 통계 스냅샷(머리). 저장 시점 계산 결과를 보존하며 재저장해도 행이 늘지 않는다",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_statistics",
                columnNames = {"id_key", "year", "month"}
        ),
        check = @CheckConstraint(name = "ck_statistics_month",
                constraint = "month between 1 and 12")
)
@Getter
@Setter
@NoArgsConstructor
public class UserStatistics extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_statistics_user")
    )
    private User user;

    /** 연. */
    @Column(name = "year", nullable = false)
    private Integer year;

    /** 월(1~12). */
    @Column(name = "month", nullable = false)
    private Integer month;

    /**
     * 저장 시각. 재저장하면 갱신된다. 응답 {@code savedAt}이 이 값이다.
     *
     * <p>감사 컬럼 {@code updatedAt}과 별개다. 감사 컬럼은 행이 마지막으로 바뀐
     * 시각이고 이쪽은 <b>통계를 계산해 넣은 시각</b>이라, 나중에 다른 이유로 행이
     * 갱신돼도 사용자에게 보이는 시각은 흔들리지 않아야 한다.
     */
    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    /** 월 소득 합계(원). */
    @Column(name = "income_total", nullable = false)
    private Long incomeTotal;

    /** 월 지출 합계(원) — 일반 + 할부 + 고정. */
    @Column(name = "expense_total", nullable = false)
    private Long expenseTotal;

    /** 고정지출 합계(원). */
    @Column(name = "fixed_amount", nullable = false)
    private Long fixedAmount;

    /** 일반·할부 지출 합계(원). */
    @Column(name = "regular_amount", nullable = false)
    private Long regularAmount;

    /** 고정 비율 %. 0.00~100.00. */
    @Column(name = "fixed_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal fixedPercent;

    /** 일반 비율 %. 0.00~100.00. */
    @Column(name = "regular_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal regularPercent;
}

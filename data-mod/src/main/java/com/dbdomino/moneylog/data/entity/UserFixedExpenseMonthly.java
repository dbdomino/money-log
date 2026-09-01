package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 월별 고정지출 내역 — {@code tbl_user_fixed_expense_monthly}.
 *
 * <p>고정지출 설정({@link UserFixedExpense})이 그 달에 실제로 얼마였는지를 담는다.
 * <b>이 행 자체가 그 달의 값이다</b> — 별도의 "월별 예외" 테이블을 두지 않는다.
 * 예외 테이블 방식이면 조회할 때마다 설정과 예외를 합쳐야 하고, 합치는 규칙이
 * 코드 여러 곳에 흩어진다.
 *
 * <p><b>이 테이블은 {@code tbl_user_expense}와 섞이지 않는다</b>(FR-052). 고정지출은
 * 사용자가 매달 입력하는 것이 아니라 설정에서 파생되는 것이라, 같은 테이블에 넣으면
 * "직접 적은 지출"과 "자동 생성분"을 구분하는 조건이 모든 조회에 붙는다.
 *
 * <h2>언제 만들어지는가</h2>
 *
 * <p>그 연·월을 <b>처음 조회할 때</b> 설정에서 복사해 만든다(lazy 생성, FR-054).
 * 월별 내역 조회(4.5)와 월별 가계부 목록(4.8) <b>양쪽</b>이 이 생성을 일으킨다.
 * 두 화면이 동시에 열리면 같은 행을 두 번 만들려 하므로, 유일 제약
 * {@code ux_user_fixed_expense_monthly}와 {@code INSERT ... ON CONFLICT DO NOTHING}을
 * 함께 써서 행이 늘어나지 않게 한다. 애플리케이션의 "있으면 건너뛴다" 검사만으로는
 * 두 트랜잭션이 동시에 "없음"을 보는 순간을 막을 수 없다.
 *
 * <p>{@code paymentDate}는 <b>말일 보정이 끝난 완전한 날짜</b>다(FR-055). 설정의
 * {@code paymentDayOfMonth}가 31이어도 2월에는 28·29일이 된다. 보정 결과를 저장해
 * 두므로 조회할 때마다 다시 계산하지 않는다 — 계산이 조회 경로에 남아 있으면 화면마다
 * 다른 답이 나올 여지가 생긴다.
 *
 * <p>{@code modified}는 <b>그 달 값을 사용자가 직접 고쳤는지</b>다(FR-057). 설정을
 * 나중에 바꿨을 때 이 표시가 붙은 달은 건드리지 않는다 — 사용자가 일부러 넣은 값을
 * 설정 변경이 덮으면 안 된다.
 *
 * <p>부모가 삭제되면 이 행도 함께 사라진다({@code ON DELETE CASCADE}, FR-059).
 * 지난 달 내역까지 전부다 — 고정지출을 지웠다는 것은 그 항목 자체를 없앤다는 뜻이다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §9</a>
 */
@Entity
@Table(
        name = "tbl_user_fixed_expense_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_fixed_expense_monthly",
                columnNames = {"fixed_expense_idx", "year", "month"}
        ),
        indexes = @Index(
                name = "ix_user_fixed_monthly_ym",
                columnList = "id_key, year, month"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserFixedExpenseMonthly extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. 부모를 타고 가지 않아도 회원의 그 달 내역을 바로 고를 수 있게 둔다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_monthly_user")
    )
    private User user;

    /**
     * 어느 고정지출의 그 달 내역인가.
     *
     * <p>{@code @OnDelete}가 있어야 DDL에 {@code ON DELETE CASCADE}가 붙는다.
     * {@code @JoinColumn}만으로는 {@code NO ACTION}이 되어 부모 삭제가 막힌다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "fixed_expense_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_monthly_fixed_expense")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserFixedExpense fixedExpense;

    /** 연. */
    @Column(name = "year", nullable = false)
    private Integer year;

    /** 월(1~12). */
    @Column(name = "month", nullable = false)
    private Integer month;

    /** 그 달 금액(원). 설정의 기본 금액과 다를 수 있다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 그 달 결제일. 말일 보정이 끝난 완전한 날짜다. */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** 그 달 내용. */
    @Column(name = "content", nullable = false, length = 255)
    private String content;

    /** 그 달 수단. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_method_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_monthly_payment_method")
    )
    private UserPaymentMethod paymentMethod;

    /** 그 달 지출유형. 단건 수정(4.6)으로는 바꾸지 않는다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "expend_group_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_monthly_expend_group")
    )
    private UserExpendGroup expendGroup;

    /** 사용자가 그 달 값을 직접 고쳤는지. 참이면 설정 변경이 이 달을 덮지 않는다. */
    @Column(name = "modified", nullable = false)
    @ColumnDefault("false")
    private Boolean modified = false;
}

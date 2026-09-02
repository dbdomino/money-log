package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.CheckConstraint;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고정지출 관리 — {@code tbl_user_fixed_expense}.
 *
 * <p>매달 반복되는 지출의 <b>설정</b>이다. 실제 그 달의 내역은
 * {@link UserFixedExpenseMonthly}가 따로 담는다. 둘을 나눈 이유는 한 달치 금액만
 * 고치는 일이 흔하기 때문이다 — 한 테이블이었다면 "3월만 5만원"을 표현할 자리가 없다.
 *
 * <p><b>이름 스냅샷 컬럼을 두지 않는다</b>(FR-050). 지출·수입과 반대다. 고정지출은
 * 과거 기록이 아니라 <b>지금 유효한 설정</b>이라 조회 시점의 현재 이름을 읽는 것이
 * 맞다. 카드 이름을 바꿨으면 이 설정도 새 이름으로 보여야 한다.
 *
 * <p>적용 기간은 연·월 4개 컬럼으로 갖는다. 비교는 {@code year * 12 + month} 합성값으로
 * 한다(FR-051) — 연과 월을 따로 비교하면 "2026년 11월 ~ 2027년 2월"처럼 해를 넘기는
 * 구간에서 조건이 어긋난다. {@link #yearMonthValue(int, int)}가 그 합성값을 만든다.
 *
 * <p>이 행이 삭제되면 그 고정지출의 월별 내역이 <b>지난 달 것까지 전부</b> 함께
 * 사라진다(FR-059). 자식 쪽 FK가 CASCADE라서 DB가 처리한다.
 *
 * <p>값을 바꿨을 때 이미 만들어진 월별 내역 중 <b>미래 달이면서 사용자가 손대지 않은
 * 것만</b> 따라간다(FR-058). 이 판정은 애플리케이션의 몫이다 — DB가 "미래"를 알 수 없다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §8</a>
 */
@Entity
@Table(
        name = "tbl_user_fixed_expense",
        comment = "고정지출 관리(설정). 매달 반복되는 지출의 기준값과 적용 기간. 이름 스냅샷을 두지 않는다",
        indexes = @Index(
                name = "ix_user_fixed_expense_period",
                columnList = "id_key, start_year, start_month, end_year, end_month"
        ),
        check = {
                @CheckConstraint(name = "ck_fixed_expense_amount", constraint = "amount > 0"),
                @CheckConstraint(name = "ck_fixed_expense_day",
                        constraint = "payment_day_of_month between 1 and 31"),
                @CheckConstraint(name = "ck_fixed_expense_start_month",
                        constraint = "start_month between 1 and 12"),
                @CheckConstraint(name = "ck_fixed_expense_end_month",
                        constraint = "end_month between 1 and 12"),
                @CheckConstraint(name = "ck_fixed_expense_period",
                        constraint = "end_year * 12 + end_month >= start_year * 12 + start_month")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserFixedExpense extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_expense_user")
    )
    private User user;

    /** 고정지출 이름 (예: 월세). 회원 안에서 중복을 막지 않는다. */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 지출 수단. 이름은 스냅샷하지 않고 조회 시 원본의 현재 이름을 읽는다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_method_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_expense_payment_method")
    )
    private UserPaymentMethod paymentMethod;

    /** 기본 금액(원). 그 달 금액이 다르면 월별 내역 쪽이 다른 값을 갖는다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /**
     * 매달 결제일(1~31).
     *
     * <p>31을 넣어도 2월에는 그런 날이 없다. <b>말일 보정은 월별 내역을 만들 때</b>
     * 하고 그 결과를 {@code payment_date}에 저장한다(FR-055). 여기 값은 보정 전
     * 설정값이다.
     */
    @Column(name = "payment_day_of_month", nullable = false)
    private Integer paymentDayOfMonth;

    /** 내용. */
    @Column(name = "content", nullable = false, length = 255)
    private String content;

    /** 지출유형. 이름은 스냅샷하지 않는다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "expend_group_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_fixed_expense_expend_group")
    )
    private UserExpendGroup expendGroup;

    /** 적용 시작 연. */
    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    /** 적용 시작 월(1~12). */
    @Column(name = "start_month", nullable = false)
    private Integer startMonth;

    /** 적용 종료 연. */
    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    /** 적용 종료 월(1~12). */
    @Column(name = "end_month", nullable = false)
    private Integer endMonth;

    /**
     * 연·월을 하나의 비교 가능한 정수로 접는다 — {@code year * 12 + month}.
     *
     * <p>기간 비교를 이 값 하나로 하기 위한 것이다. 연과 월을 각각 비교하면 해를
     * 넘기는 구간에서 조건이 어긋난다. CHECK {@code ck_fixed_expense_period}와
     * 기간 조회 JPQL이 모두 같은 식을 쓴다.
     */
    public static int yearMonthValue(int year, int month) {
        return year * 12 + month;
    }

    /** 이 고정지출의 적용 기간이 주어진 연·월을 포함하는가(FR-056). 양 끝을 포함한다. */
    public boolean coversYearMonth(int year, int month) {
        int target = yearMonthValue(year, month);
        return yearMonthValue(startYear, startMonth) <= target
                && target <= yearMonthValue(endYear, endMonth);
    }
}

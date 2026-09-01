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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 월별 지출 내역 — {@code tbl_user_expense}.
 *
 * <p><b>고정지출 행은 여기 들어오지 않는다</b>(FR-052). 고정지출은
 * {@code tbl_user_fixed_expense_monthly}가 따로 담는다. 둘을 한 테이블에 섞으면
 * "이번 달 지출"을 셀 때마다 어느 쪽을 빼야 하는지 판단이 붙는다.
 *
 * <p><b>이름 스냅샷 2개를 둔다</b>({@code paymentMethodName}·{@code expendGroupName}).
 * 등록 당시의 수단·유형 이름을 그대로 보존하기 위한 것이다(FR-042). 원본 이름이 나중에
 * 바뀌어도 과거 건은 그때 이름으로 남아야 한다 — 3월에 "국민카드"로 적은 지출이
 * 카드 이름을 바꿨다고 소급해서 달라지면 그때의 가계부가 아니게 된다. 스냅샷은
 * <b>참조 자체가 바뀔 때만</b> 새 참조의 현재 이름으로 갱신한다.
 *
 * <p>참조는 FK로 남겨 둔다. 수단·유형이 삭제 표시(행 보존)로 관리되므로(FR-031·037)
 * FK RESTRICT를 걸어도 삭제가 막히지 않는다.
 *
 * <h2>일시불과 할부</h2>
 *
 * <p>할부 3개 컬럼({@code installmentGroupId}·{@code installmentIndex}·
 * {@code installmentTotal})이 <b>모두 비어 있으면 일시불</b>, 모두 채워지면 할부다
 * (FR-043). 12개월 할부는 12개의 행이 되고 같은 {@code installmentGroupId}를 공유하며
 * 한 트랜잭션에 저장된다 — 일부만 남으면 개월 수가 맞지 않는 할부가 된다(FR-044).
 *
 * <p>{@code installmentGroupId}는 시퀀스 {@code seq_installment_group}이 발급한다.
 * 이 Entity의 {@code @GeneratedValue}가 아니다 — 한 행이 아니라 <b>N개 행이 공유하는</b>
 * 값이라 행마다 새로 발급되면 안 된다. 값을 미리 받아 12개 행에 같이 넣는다.
 *
 * <p>중도상환은 {@code installment_group_id}가 같고 결제일이 오늘보다 <b>뒤인</b>
 * 회차만 지우는 물리 삭제다(FR-045). 오늘·과거 회차는 이미 결제된 것이라 남긴다.
 * {@code ix_user_expense_installment}가 이 조건을 그대로 덮는다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §6</a>
 */
@Entity
@Table(
        name = "tbl_user_expense",
        indexes = {
                @Index(
                        name = "ix_user_expense_date",
                        columnList = "id_key, payment_date"
                ),
                @Index(
                        name = "ix_user_expense_installment",
                        columnList = "installment_group_id, payment_date"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserExpense extends BaseAuditEntity {

    /** 할부 그룹 식별자를 발급하는 시퀀스 이름. 보조 DDL {@code sql/04_constraints.sql}이 만든다. */
    public static final String INSTALLMENT_GROUP_SEQUENCE = "seq_installment_group";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_expense_user")
    )
    private User user;

    /** 지출 수단. 삭제 표시된 수단도 계속 가리킬 수 있다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_method_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_expense_payment_method")
    )
    private UserPaymentMethod paymentMethod;

    /** 등록 당시 수단 이름. 원본 이름이 바뀌어도 따라가지 않는다. */
    @Column(name = "payment_method_name", nullable = false, length = 50)
    private String paymentMethodName;

    /** 금액(원 단위 정수). 0 이하는 {@code ck_expense_amount}가 막는다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 결제일. 할부는 회차마다 다른 날짜를 갖는다. */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** 장소. */
    @Column(name = "place", nullable = false, length = 100)
    private String place;

    /** 내용. */
    @Column(name = "content", nullable = false, length = 255)
    private String content;

    /** 지출유형. 삭제 표시된 유형도 계속 가리킬 수 있다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "expend_group_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_expense_expend_group")
    )
    private UserExpendGroup expendGroup;

    /** 등록 당시 지출유형 이름. 원본 이름이 바뀌어도 따라가지 않는다. */
    @Column(name = "expend_group_name", nullable = false, length = 30)
    private String expendGroupName;

    /**
     * 할부 그룹 식별자. 일시불이면 비어 있다.
     *
     * <p>같은 할부의 모든 회차가 이 값을 공유한다. 발급은 시퀀스
     * {@link #INSTALLMENT_GROUP_SEQUENCE}가 한다.
     */
    @Column(name = "installment_group_id")
    private Long installmentGroupId;

    /** 할부 회차 번호(1부터). 일시불이면 비어 있다. */
    @Column(name = "installment_index")
    private Integer installmentIndex;

    /** 총 할부 개월 수(2 이상). 일시불이면 비어 있다. */
    @Column(name = "installment_total")
    private Integer installmentTotal;

    /**
     * 할부 건인지 여부 — 세 컬럼이 모두 채워졌을 때만 참이다(FR-043).
     *
     * <p>세 컬럼 중 일부만 채워진 상태는 <b>정의되지 않은 상태</b>다. CHECK 제약이
     * 각 컬럼의 값 범위만 보고 셋의 동시 유무는 보지 않으므로, 읽는 쪽이 한 곳에서
     * 판정하도록 여기에 둔다.
     */
    public boolean isInstallment() {
        return installmentGroupId != null && installmentIndex != null && installmentTotal != null;
    }
}

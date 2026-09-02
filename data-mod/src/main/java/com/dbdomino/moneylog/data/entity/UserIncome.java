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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 월별 수입 내역 — {@code tbl_user_income}.
 *
 * <p><b>지출과 별도 테이블이다</b>(FR-046). 장소·지출유형·할부 컬럼이 없다 — 수입에는
 * 해당 개념이 없다. 한 테이블에 합치면 그 컬럼들이 전부 nullable이 되고, "지출이면
 * 반드시 있어야 하고 수입이면 반드시 없어야 한다"를 DB가 강제할 수 없게 된다.
 *
 * <p>{@code paymentMethodName}은 <b>등록 당시</b> 수단 이름이다. 지출과 같은 규칙으로,
 * 원본 수단 이름이 바뀌거나 그 수단이 삭제 표시돼도 과거 수입은 그때 이름을 유지한다.
 *
 * <p>{@code content}만 비어 있을 수 있다. 나머지는 전부 필수다.
 *
 * <p>엑셀 일괄 등록은 이 테이블과 {@code tbl_user_expense}에만 행을 만든다. 업로드
 * 이력은 저장하지 않는다(FR-049).
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §7</a>
 */
@Entity
@Table(
        name = "tbl_user_income",
        comment = "월별 수입 내역. 장소·지출유형·할부가 없어 지출과 별도 테이블로 둔다",
        indexes = @Index(
                name = "ix_user_income_date",
                columnList = "id_key, payment_date"
        ),
        check = @CheckConstraint(name = "ck_income_amount", constraint = "amount > 0")
)
@Getter
@Setter
@NoArgsConstructor
public class UserIncome extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_income_user")
    )
    private User user;

    /**
     * 소득 수단. 용도가 {@code INCOME}인 수단이어야 한다.
     *
     * <p>이 판정은 <b>애플리케이션의 몫이다</b>. 용도는 수단 쪽 컬럼이라 DB CHECK으로는
     * 다른 테이블의 값을 볼 수 없다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_method_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_income_payment_method")
    )
    private UserPaymentMethod paymentMethod;

    /** 등록 당시 수단 이름. 원본 이름이 바뀌어도 따라가지 않는다. */
    @Column(name = "payment_method_name", nullable = false, length = 50)
    private String paymentMethodName;

    /** 금액(원 단위 정수). 0 이하는 {@code ck_income_amount}가 막는다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 수입일. */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** 내용. 비어 있을 수 있다 — 지출의 {@code content}와 달리 선택 항목이다. */
    @Column(name = "content", length = 255)
    private String content;
}

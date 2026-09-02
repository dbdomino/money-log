package com.dbdomino.moneylog.data.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 통계 수단별 요약 — {@code tbl_statistics_payment_method}.
 *
 * <p>{@link UserStatisticsExpendGroup}과 같은 규칙이다 — {@code paymentMethodIdx}에
 * <b>FK를 걸지 않고</b> 값으로만 보관한다(FR-078a). 연관으로 매핑하면 Hibernate가 FK를
 * 만들어, 원본 수단이 사라질 때 과거 통계가 함께 훼손된다. 화면 복원은 함께 저장한
 * {@code paymentMethodName}이 맡는다.
 *
 * <p>유형별 요약과 <b>한 가지가 다르다</b>: 지출이 0원인 수단도 포함한다(FR-076).
 * 수단별 화면은 "이 카드는 이번 달에 한 번도 안 썼다"를 보여주는 것이 의미가 있어서다.
 * 유형은 반대로 0원이면 빼기 때문에, 두 상세의 행 수가 다른 것은 정상이다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §15</a>
 */
@Entity
@Table(
        name = "tbl_statistics_payment_method",
        comment = "통계 수단별 요약. 수단 참조에 FK 가 없다. 지출 0원인 수단도 행으로 남긴다",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_stat_method",
                columnNames = {"statistics_idx", "payment_method_idx"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserStatisticsPaymentMethod extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stat_method_user")
    )
    private User user;

    /** 소속 통계 스냅샷. 부모가 지워지면 이 행도 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "statistics_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stat_method_statistics")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserStatistics statistics;

    /**
     * 수단 대리키 — <b>값으로만</b> 보관한다. FK가 없다(FR-078a).
     *
     * <p>연관으로 매핑하지 말 것.
     */
    @Column(name = "payment_method_idx", nullable = false)
    private Long paymentMethodIdx;

    /** 저장 당시 수단 이름. 원본이 사라져도 화면은 이 이름으로 복원한다. */
    @Column(name = "payment_method_name", nullable = false, length = 50)
    private String paymentMethodName;

    /** 그 수단의 지출 합계(원). 0원인 수단도 저장한다. */
    @Column(name = "amount", nullable = false)
    private Long amount;
}

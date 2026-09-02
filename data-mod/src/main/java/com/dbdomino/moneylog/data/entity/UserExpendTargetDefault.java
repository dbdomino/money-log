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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기본 목표금액 — {@code tbl_user_expend_target_default}.
 *
 * <p>지출유형마다 "평소 이만큼 쓴다"를 정해 두는 값이다. 특정 달에만 다른 값을 쓰고
 * 싶으면 {@link UserExpendTargetMonthly}가 따로 담는다. 두 값은 <b>독립</b>이다 —
 * 여기를 고쳐도 이미 만들어진 월별 목표는 변하지 않는다(FR-072).
 *
 * <p>통계가 쓰는 적용 금액은 {@code 월별 값 ?? 기본 값}이다. 그 판정은 통계를 만들
 * 때 하고, 결과를 {@link UserStatisticsExpendGroup#getTargetAmount()}에 박아 둔다.
 *
 * <p><b>지출유형이 삭제 표시돼도 이 행과 참조를 그대로 유지한다</b>(FR-038). 유형이
 * 물리 삭제였다면 FK RESTRICT가 유형 삭제를 막거나 이 행을 함께 지워야 했을 텐데,
 * 삭제 표시(UPDATE)라 둘 다 일어나지 않는다.
 *
 * <p>금액 상한 1억은 목표금액에만 있다(contracts §5). 지출·소득·고정지출 금액에는
 * 상한을 두지 않는다 — 실제 거래액에 상한을 걸 근거가 없다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §10</a>
 */
@Entity
@Table(
        name = "tbl_user_expend_target_default",
        comment = "기본 목표금액. 지출유형별 평상시 목표. 월별 목표와 독립이다",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_target_default",
                columnNames = {"id_key", "expend_group_idx"}
        ),
        check = @CheckConstraint(name = "ck_target_default_amount",
                constraint = "target_amount between 0 and 100000000")
)
@Getter
@Setter
@NoArgsConstructor
public class UserExpendTargetDefault extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_target_default_user")
    )
    private User user;

    /** 지출유형. 삭제 표시된 유형도 계속 가리킬 수 있다(FR-038). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "expend_group_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_target_default_expend_group")
    )
    private UserExpendGroup expendGroup;

    /**
     * 기본 목표 금액(원). 0원~1억.
     *
     * <p>0은 유효한 값이다 — "이 유형에는 쓰지 않겠다"는 뜻이라 행이 없는 것과 다르다.
     * 범위는 {@code ck_target_default_amount}가 강제한다.
     */
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;
}

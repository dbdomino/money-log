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

/**
 * 월별 목표금액 — {@code tbl_user_expend_target_monthly}.
 *
 * <p>특정 연·월에만 적용되는 목표다. {@link UserExpendTargetDefault}와 <b>독립</b>이며,
 * 기본값을 여기 복사해 두지도 않는다(FR-072). 복사해 두면 기본값을 고쳤을 때 어느
 * 달까지 따라가야 하는지가 매번 판단거리가 된다.
 *
 * <p><b>행이 없는 것과 {@code targetAmount = 0}은 다른 상태다</b>(FR-073). 앞은
 * "그 달은 따로 정하지 않았다"(응답 {@code monthlyTargetAmount}가 {@code null})이고,
 * 뒤는 "그 달은 0원으로 정했다"이다. 없음을 0으로 접으면 기본값을 무시하겠다는
 * 의사 표시를 표현할 수 없다.
 *
 * <p>통계가 쓰는 적용 금액 = {@code 월별 값 ?? 기본 값}.
 *
 * <p>지출유형이 삭제 표시돼도 이 행과 참조는 유지된다(FR-038).
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §11</a>
 */
@Entity
@Table(
        name = "tbl_user_expend_target_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_target_monthly",
                columnNames = {"id_key", "year", "month", "expend_group_idx"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserExpendTargetMonthly extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_target_monthly_user")
    )
    private User user;

    /** 연. */
    @Column(name = "year", nullable = false)
    private Integer year;

    /** 월(1~12). */
    @Column(name = "month", nullable = false)
    private Integer month;

    /** 지출유형. 삭제 표시된 유형도 계속 가리킬 수 있다(FR-038). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "expend_group_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_target_monthly_expend_group")
    )
    private UserExpendGroup expendGroup;

    /** 그 달 목표 금액(원). 0원~1억. 0은 유효한 값이다. */
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;
}

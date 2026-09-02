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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 통계 지출유형별 요약 — {@code tbl_statistics_expend_group}.
 *
 * <p><b>{@code expendGroupIdx}에 FK를 걸지 않는다</b>(FR-078a). 이것이 이 Entity에서
 * 가장 조심할 점이다. 통계는 저장 시점의 사진이라 원본 유형이 그 뒤에 사라져도 남아야
 * 하는데, FK가 있으면 그 유형을 지우는 순간 삭제가 막히거나(RESTRICT) 과거 통계가
 * 함께 사라진다(CASCADE). 둘 다 "그때 이렇게 보였다"는 기록을 훼손한다.
 *
 * <p>그래서 이 컬럼은 {@code @ManyToOne}·{@code @JoinColumn}이 아니라 <b>평범한
 * {@code Long} 컬럼</b>이다. 연관으로 매핑하면 Hibernate가 FK를 만들어 버린다.
 * 화면 복원은 함께 저장한 {@code expendGroupName}이 맡고, 응답 {@code expendGroupId}는
 * 저장된 값을 그대로 내려준다 — 실재하지 않는 유형일 수 있다.
 *
 * <p>{@code targetAmount}는 <b>적용된</b> 목표금액이다({@code 월별 값 ?? 기본 값}).
 * 어느 쪽에서 왔는지는 남기지 않는다 — 통계에 필요한 것은 결과 하나다.
 *
 * <p>지출이 <b>0원인 유형은 저장하지 않는다</b>(FR-076). 수단별 요약과 반대다.
 * 쓰지 않은 유형까지 넣으면 화면의 유형 목록이 회원의 전체 유형 목록과 같아진다.
 *
 * <p>{@code usageRate}가 {@code NUMERIC(6,2)}인 이유는 사용률이 100%를 넘을 수
 * 있어서다({@code OVER}). 목표 대비 1000% 같은 값도 담긴다. 목표가 0이면 0으로 둔다 —
 * 나눗셈이 성립하지 않는다.
 *
 * <p>{@code status} 기준(90% 미만 / 90~110% / 110% 초과)이 나중에 바뀌어도 이미 저장된
 * 행은 다시 계산하지 않는다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §14</a>
 */
@Entity
@Table(
        name = "tbl_statistics_expend_group",
        comment = "통계 지출유형별 요약. 유형 참조에 FK 가 없다 — 원본이 사라져도 이 기록은 남아야 한다",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_stat_group",
                columnNames = {"statistics_idx", "expend_group_idx"}
        ),
        check = @CheckConstraint(name = "ck_stat_group_status",
                constraint = "status in ('UNDER', 'OK', 'OVER')")
)
@Getter
@Setter
@NoArgsConstructor
public class UserStatisticsExpendGroup extends BaseAuditEntity {

    /** {@code status} 허용 값 — 목표의 90% 미만. */
    public static final String STATUS_UNDER = "UNDER";
    /** {@code status} 허용 값 — 목표의 90~110%. */
    public static final String STATUS_OK = "OK";
    /** {@code status} 허용 값 — 목표의 110% 초과. */
    public static final String STATUS_OVER = "OVER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stat_group_user")
    )
    private User user;

    /** 소속 통계 스냅샷. 부모가 지워지면 이 행도 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "statistics_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_stat_group_statistics")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserStatistics statistics;

    /**
     * 지출유형 대리키 — <b>값으로만</b> 보관한다. FK가 없다(FR-078a).
     *
     * <p>연관으로 매핑하지 말 것. Hibernate가 FK를 만들면 원본 유형이 사라질 때
     * 과거 통계가 훼손된다.
     */
    @Column(name = "expend_group_idx", nullable = false)
    private Long expendGroupIdx;

    /** 저장 당시 유형 이름. 원본이 사라져도 화면은 이 이름으로 복원한다. */
    @Column(name = "expend_group_name", nullable = false, length = 30)
    private String expendGroupName;

    /** 그 유형의 지출 합계(원). 0원인 유형은 아예 저장하지 않는다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 적용된 목표금액(원) — 월별 값이 있으면 그것, 없으면 기본 값. */
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    /** 사용률 %. 100을 넘을 수 있다. 목표가 0이면 0. */
    @Column(name = "usage_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal usageRate;

    /** {@link #STATUS_UNDER}·{@link #STATUS_OK}·{@link #STATUS_OVER} 중 하나. */
    @Column(name = "status", nullable = false, length = 10)
    private String status;
}

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 통계 주별 지출 — {@code tbl_user_statistics_weekly}.
 *
 * <p>{@link UserStatistics} 한 건에 딸린 주 단위 합계다. 주 경계를 <b>저장해 둔다</b>
 * ({@code weekStart}·{@code weekEnd}) — 월요일 기준이되 1일이 월요일이 아니면 첫 주는
 * 1일부터 시작하므로, 규칙을 조회할 때마다 다시 적용하면 계산 코드가 바뀔 때 과거
 * 통계의 주 구분까지 소급해서 달라진다.
 *
 * <p>부모가 삭제되면 함께 사라진다({@code ON DELETE CASCADE}). 재저장이 상세를
 * 지웠다 다시 넣는 방식이라 이 삭제 경로가 자주 쓰인다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §13</a>
 */
@Entity
@Table(
        name = "tbl_user_statistics_weekly",
        comment = "통계 주별 지출. 주 경계를 저장해 두어 조회할 때 다시 계산하지 않는다",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_user_stat_weekly",
                columnNames = {"statistics_idx", "week_index"}
        ),
        check = @CheckConstraint(name = "ck_stat_weekly_index", constraint = "week_index >= 1")
)
@Getter
@Setter
@NoArgsConstructor
public class UserStatisticsWeekly extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_stat_weekly_user")
    )
    private User user;

    /** 소속 통계 스냅샷. 부모가 지워지면 이 행도 사라진다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "statistics_idx",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_stat_weekly_statistics")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserStatistics statistics;

    /** 주차(1부터). */
    @Column(name = "week_index", nullable = false)
    private Integer weekIndex;

    /** 주 시작일. 월요일 기준이되 그 달 첫 주는 1일부터다. */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** 주 종료일. */
    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    /** 그 주 지출 합계(원). */
    @Column(name = "amount", nullable = false)
    private Long amount;
}

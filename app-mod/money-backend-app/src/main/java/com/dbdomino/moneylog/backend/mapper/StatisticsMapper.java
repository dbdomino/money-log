package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.StatisticsExpendGroupResponse;
import com.dbdomino.moneylog.backend.dto.response.StatisticsPaymentMethodResponse;
import com.dbdomino.moneylog.backend.dto.response.StatisticsResponse;
import com.dbdomino.moneylog.backend.dto.response.StatisticsWeeklyResponse;
import com.dbdomino.moneylog.backend.service.statistics.StatisticsCalculator;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.entity.UserStatisticsExpendGroup;
import com.dbdomino.moneylog.data.entity.UserStatisticsPaymentMethod;
import com.dbdomino.moneylog.data.entity.UserStatisticsWeekly;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 통계 Entity·계산 결과 ↔ DTO.
 *
 * <h2>같은 응답을 만드는 입구가 둘이다</h2>
 *
 * <pre>{@code
 * 저장본  UserStatistics + 상세 3종 Entity   →  StatisticsResponse   (source=SAVED)
 * 즉석    StatisticsCalculator.Result        →  StatisticsResponse   (source=CALCULATED)
 * }</pre>
 *
 * <p>둘을 <b>한 매퍼에 두는 것</b>이 요점이다. 나누면 두 경로의 필드 채우기가 갈릴 수
 * 있는데, 사용자가 5.5 로 이번 달을 보고 5.6 으로 저장한 뒤 다시 조회하는 흐름이라
 * <b>저장 직후 숫자가 달라 보이면 신뢰를 잃는다.</b>
 *
 * <h2>이름은 저장 당시 스냅샷이다 — 목표금액과 정반대다</h2>
 *
 * <pre>{@code
 * // 006 목표금액 ExpendTargetMapper — 연관을 타고 원본에서 읽는다 (현재 이름)
 * //   group.name  →  response.expendGroupName
 *
 * // 006 통계 여기 — Entity 자신의 이름 컬럼에서 읽는다 (스냅샷)
 * //   statisticsGroup.expendGroupName  →  response.expendGroupName
 * }</pre>
 *
 * <p>한 기능 안에서 두 규칙이 공존하는 것이 006 의 특징이다. 대상이 달라서다 —
 * <b>목표금액은 지금 유효한 설정</b>이고 <b>통계는 과거 기록</b>이다.
 *
 * <p><b>스키마가 이 구분을 이미 표현한다.</b> {@code tbl_expend_target_*} 에는 이름 컬럼이
 * 없고 {@code tbl_statistics_*} 에는 NOT NULL 로 있다(덤프 확인). 그리고 통계 상세에는
 * 지출유형·수단 <b>FK 가 없어서</b>(FR-519) 원본이 사라져도 행이 남는다 — 그때 화면을
 * 복원하는 것이 이 이름이다. 여기서 연관을 타고 현재 이름을 읽도록 고치면 <b>원본이 사라진
 * 과거 통계를 그릴 수 없게 된다.</b>
 *
 * <p><b>비즈니스 판정을 담지 않는다</b>(헌장 원칙 II). 저장본을 쓸지 계산할지는
 * {@code StatisticsQueryService} 가 정하고 여기는 받은 것을 옮기기만 한다.
 */
@Mapper(componentModel = "spring")
public interface StatisticsMapper {

    /** 저장본 → 응답. {@code source} 는 항상 {@code SAVED} 이고 {@code savedAt} 이 있다. */
    default StatisticsResponse toSavedResponse(UserStatistics statistics,
                                               List<UserStatisticsWeekly> weeklyRows,
                                               List<UserStatisticsExpendGroup> groupRows,
                                               List<UserStatisticsPaymentMethod> methodRows) {
        return new StatisticsResponse(
                statistics.getYear(), statistics.getMonth(),
                StatisticsResponse.SOURCE_SAVED, statistics.getSavedAt(),
                statistics.getIncomeTotal(), statistics.getExpenseTotal(),
                new StatisticsResponse.FixedVsRegularRatio(
                        statistics.getFixedAmount(), statistics.getRegularAmount(),
                        statistics.getFixedPercent(), statistics.getRegularPercent()),
                toWeeklyResponses(weeklyRows),
                toGroupResponses(groupRows),
                toMethodResponses(methodRows));
    }

    /**
     * 계산 결과 → 응답. {@code source} 는 항상 {@code CALCULATED} 다.
     *
     * @param savedAt 저장본이 있으면 그 시각, 없으면 {@code null}. <b>{@code view=live} 에서
     *                저장본이 있을 때 값이 실린다</b>(FR-515) — 그래야 프론트가 "저장본 있음
     *                / 지금 최신"을 구분한다
     */
    default StatisticsResponse toCalculatedResponse(int year, int month,
                                                    StatisticsCalculator.Result result,
                                                    OffsetDateTime savedAt) {
        return new StatisticsResponse(
                year, month, StatisticsResponse.SOURCE_CALCULATED, savedAt,
                result.incomeTotal(), result.expenseTotal(),
                new StatisticsResponse.FixedVsRegularRatio(
                        result.fixedAmount(), result.regularAmount(),
                        result.fixedPercent(), result.regularPercent()),
                fromCalculatedWeeklyRows(result.weeklyRows()),
                fromCalculatedGroupRows(result.groupRows()),
                fromCalculatedMethodRows(result.methodRows()));
    }

    // ── 저장본 상세 3종 ──────────────────────────────────────────────────────

    List<StatisticsWeeklyResponse> toWeeklyResponses(List<UserStatisticsWeekly> rows);

    StatisticsWeeklyResponse toWeeklyResponse(UserStatisticsWeekly row);

    List<StatisticsExpendGroupResponse> toGroupResponses(List<UserStatisticsExpendGroup> rows);

    /**
     * {@code target} 은 Entity 의 {@code targetAmount} 다.
     *
     * <p>응답 필드 이름이 짧은 것은 5.5 설계 명세의 필드 표가 그렇게 정했기 때문이다.
     */
    @Mapping(target = "expendGroupId", source = "expendGroupIdx")
    @Mapping(target = "target", source = "targetAmount")
    StatisticsExpendGroupResponse toGroupResponse(UserStatisticsExpendGroup row);

    List<StatisticsPaymentMethodResponse> toMethodResponses(
            List<UserStatisticsPaymentMethod> rows);

    @Mapping(target = "paymentMethodId", source = "paymentMethodIdx")
    StatisticsPaymentMethodResponse toMethodResponse(UserStatisticsPaymentMethod row);

    // ── 계산 결과 상세 3종 ───────────────────────────────────────────────────

    List<StatisticsWeeklyResponse> fromCalculatedWeeklyRows(
            List<StatisticsCalculator.WeeklyRow> rows);

    StatisticsWeeklyResponse fromCalculatedWeeklyRow(StatisticsCalculator.WeeklyRow row);

    List<StatisticsExpendGroupResponse> fromCalculatedGroupRows(
            List<StatisticsCalculator.GroupRow> rows);

    @Mapping(target = "expendGroupId", source = "expendGroupIdx")
    @Mapping(target = "target", source = "targetAmount")
    StatisticsExpendGroupResponse fromCalculatedGroupRow(StatisticsCalculator.GroupRow row);

    List<StatisticsPaymentMethodResponse> fromCalculatedMethodRows(
            List<StatisticsCalculator.MethodRow> rows);

    @Mapping(target = "paymentMethodId", source = "paymentMethodIdx")
    StatisticsPaymentMethodResponse fromCalculatedMethodRow(StatisticsCalculator.MethodRow row);
}

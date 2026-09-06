package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.StatisticsViewQuery;
import com.dbdomino.moneylog.backend.dto.response.StatisticsResponse;
import com.dbdomino.moneylog.backend.mapper.StatisticsMapper;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.statistics.StatisticsCalculator;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.repository.UserStatisticsExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsWeeklyRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 5.5 월별 통계 조회 — <b>저장본이냐 즉석 계산이냐의 분기를 여기 둔다</b>(FR-513).
 *
 * <h2>계산기는 분기를 모른다</h2>
 *
 * <p>{@link StatisticsCalculator} 는 호출되면 <b>항상 계산한다.</b> "저장본이 있으면
 * 저장본"이라는 판단은 이 클래스 몫이다. 계산기에 분기를 넣으면 5.6(저장)이 그 분기를
 * 피해 가야 하고, 그 순간 <b>두 API 가 같은 달에 다른 값을 낼 수 있는 길</b>이 열린다.
 *
 * <h2>세 갈래</h2>
 *
 * <pre>{@code
 * view 생략 · saved
 *     저장본 있음  →  저장본 그대로.   source=SAVED,       savedAt=저장 시각
 *     저장본 없음  →  즉석 계산.       source=CALCULATED,  savedAt=null
 *
 * view=live
 *     저장본을 읽지도 쓰지도 않고 즉석 계산.
 *                                     source=CALCULATED,  savedAt=있으면 저장 시각
 * }</pre>
 *
 * <p><b>{@code view=live} 도 {@code savedAt} 을 위해 저장본 <i>행</i>은 조회한다.</b>
 * "읽지 않는다"는 <b>수치를 쓰지 않는다</b>는 뜻이지 존재 확인까지 막는다는 뜻이 아니다 —
 * FR-515 가 그 시각을 함께 실으라고 정했다.
 *
 * <h2>DB 를 바꾸지 않는다</h2>
 *
 * <p>{@code view=live} 가 저장본을 <b>"쓰지도" 않는다</b>(SC-507). "최신 값을 봤으니 저장해
 * 두자"는 편의는 넣지 않는다 — 저장은 5.6 의 명시적 행위여야 한다.
 *
 * <p>그리고 <b>005 의 lazy 생성도 일으키지 않는다.</b> 계산기가 월별 고정지출 내역을
 * 읽기만 하므로, 한 번도 열지 않은 달의 고정지출 합계는 0 이다 — 버그가 아니라 결정이다.
 *
 * @see <a href="../../../../../../../../specs/006-backend-target-statistics/contracts/statistics-snapshot.md">statistics-snapshot.md §8</a>
 */
@Service
public class StatisticsQueryService {

    private final UserStatisticsRepository statisticsRepository;
    private final UserStatisticsWeeklyRepository weeklyRepository;
    private final UserStatisticsExpendGroupRepository groupRepository;
    private final UserStatisticsPaymentMethodRepository methodRepository;
    private final StatisticsCalculator calculator;
    private final StatisticsMapper mapper;

    public StatisticsQueryService(UserStatisticsRepository statisticsRepository,
                                  UserStatisticsWeeklyRepository weeklyRepository,
                                  UserStatisticsExpendGroupRepository groupRepository,
                                  UserStatisticsPaymentMethodRepository methodRepository,
                                  StatisticsCalculator calculator,
                                  StatisticsMapper mapper) {
        this.statisticsRepository = statisticsRepository;
        this.weeklyRepository = weeklyRepository;
        this.groupRepository = groupRepository;
        this.methodRepository = methodRepository;
        this.calculator = calculator;
        this.mapper = mapper;
    }

    /**
     * 5.5 조회.
     *
     * <p>{@code readOnly = true} 다 — 이 경로는 <b>어떤 테이블도 쓰지 않는다.</b>
     */
    @Transactional(readOnly = true)
    public StatisticsResponse get(AuthPrincipal principal, YearMonthValue yearMonth,
                                  StatisticsViewQuery view) {
        Optional<UserStatistics> saved = statisticsRepository.findByUserIdKeyAndYearAndMonth(
                principal.idKey(), yearMonth.year(), yearMonth.month());

        if (view.prefersSaved() && saved.isPresent()) {
            return savedResponse(saved.get());
        }
        return calculatedResponse(principal, yearMonth,
                saved.map(UserStatistics::getSavedAt).orElse(null));
    }

    /**
     * 저장본을 그대로 돌려준다 — <b>다시 계산하지 않는다</b>(FR-518).
     *
     * <p>주 경계도 저장된 {@code week_start}·{@code week_end} 를 쓴다. 그래서 저장 이후에
     * 그 달 지출을 고치거나, 지출유형을 삭제 표시하거나, 목표금액을 바꾸거나, 판정 기준이
     * 바뀌어도 이 응답은 변하지 않는다. 최신화하는 유일한 방법은 5.6 을 다시 부르는 것이다.
     */
    private StatisticsResponse savedResponse(UserStatistics statistics) {
        Long idx = statistics.getIdx();
        return mapper.toSavedResponse(statistics,
                weeklyRepository.findByStatisticsIdxOrderByWeekIndexAsc(idx),
                groupRepository.findByStatisticsIdxOrderByIdxAsc(idx),
                methodRepository.findByStatisticsIdxOrderByIdxAsc(idx));
    }

    /**
     * 지금 계산한다.
     *
     * @param savedAt 저장본이 있으면 그 시각. 저장본 없는 달이면 {@code null} 이다
     */
    private StatisticsResponse calculatedResponse(AuthPrincipal principal,
                                                  YearMonthValue yearMonth,
                                                  OffsetDateTime savedAt) {
        return mapper.toCalculatedResponse(yearMonth.year(), yearMonth.month(),
                calculator.calculate(principal, yearMonth), savedAt);
    }
}

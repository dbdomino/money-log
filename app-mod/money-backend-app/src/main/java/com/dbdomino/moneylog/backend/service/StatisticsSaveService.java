package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.response.StatisticsResponse;
import com.dbdomino.moneylog.backend.dto.response.StatisticsSaveResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.statistics.StatisticsCalculator;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserStatistics;
import com.dbdomino.moneylog.data.entity.UserStatisticsExpendGroup;
import com.dbdomino.moneylog.data.entity.UserStatisticsPaymentMethod;
import com.dbdomino.moneylog.data.entity.UserStatisticsWeekly;
import com.dbdomino.moneylog.data.repository.UserRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsRepository;
import com.dbdomino.moneylog.data.repository.UserStatisticsWeeklyRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 5.6 통계 저장 — 그 달을 스냅샷으로 굳힌다.
 *
 * <h2>5.5 와 같은 계산기를 쓴다</h2>
 *
 * <p>{@link StatisticsCalculator} 하나를 즉석 계산(5.5)과 저장(5.6)이 공유한다. 사용자
 * 흐름이 "5.5 로 보고 마음에 들면 5.6 으로 저장"이라 <b>저장 직후 다시 조회했을 때 숫자가
 * 달라지면 신뢰를 잃는다.</b>
 *
 * <h2>판정 순서</h2>
 *
 * <pre>{@code
 * 1. year·month 범위                              오류 → 3603   (컨트롤러/DTO 에서 끝난다)
 * 2. 저장 대상이 현재 연월을 초과하는가                → 3604
 * 3. 계산 (5.5 의 즉석 계산과 같은 계산기)
 * 4. 통계 행 upsert + 상세 3종 삭제 후 재삽입
 * }</pre>
 *
 * <p><b>2번의 경계가 "초과"다.</b> 이번 달은 진행 중이지만 실제 데이터가 있어 저장할
 * 가치가 있고, 이후 5.6 을 다시 불러 덮어쓴다. {@code >=} 로 잡으면 이번 달 저장이
 * 막힌다.
 *
 * <h2>재저장 — 통계 행은 갱신, 상세는 지웠다 다시 넣는다 (FR-517)</h2>
 *
 * <p>상세를 갱신이 아니라 삭제 후 삽입하는 이유는 <b>행 집합 자체가 달라지기</b>
 * 때문이다. 지난번 저장 이후 새 유형에 지출이 생기면 행이 늘고, 어떤 유형의 지출이 전부
 * 삭제되면 행이 줄어든다. 갱신으로 맞추려면 "있는데 없어진 것"을 찾아 지우는 로직이 따로
 * 필요하고, 그걸 빠뜨리면 <b>없어진 유형의 행이 남는다</b>.
 *
 * <p>반대로 <b>통계 행 자체는 갱신한다.</b> 지우고 다시 만들면 {@code idx} 가 바뀌어
 * 상세의 {@code statistics_idx} 를 전부 다시 써야 하고,
 * {@code ux_statistics (id_key, year, month)} 때문에 삭제와 삽입 사이에 창이 생긴다.
 *
 * <h2>삭제와 삽입 사이에 flush 가 필요하다</h2>
 *
 * <p><b>Hibernate 의 {@code ActionQueue} 는 한 flush 안에서 INSERT 를 DELETE 보다 먼저
 * 실행한다.</b> 상세 3종에 유니크 제약이 있어({@code ux_stat_group}·{@code ux_stat_method}·
 * {@code ux_stat_weekly}) "지우고 새로 넣기"를 그대로 쓰면 같은 키의 INSERT 가 먼저 나가
 * <b>유니크 위반이 {@code 9000} 으로 새어 나간다</b>. 같은 달을 <b>두 번째로</b> 저장할
 * 때만 터지므로 첫 저장만 거는 시험으로는 드러나지 않는다.
 *
 * <p>여기서는 파생 삭제({@code deleteByStatisticsIdx}) 뒤 {@code flush()} 를 명시해 DELETE
 * 를 먼저 내보낸다. 005 의 4.9 가 같은 자리에서 다른 방식으로 터졌다 — 그쪽은 지운 Entity
 * 를 {@code save} 대상에 넣어 {@code ObjectDeletedException} 이 났다. 파생 삭제와 삽입이
 * 만나는 지점은 두 번 다 문제였다.
 *
 * <h2>빈 달도 저장한다 (FR-528)</h2>
 *
 * <p>지출·소득이 한 건도 없어도 합계·비율 6값을 0 으로 채운 행을 남긴다. 거절하면
 * <b>"이 달은 아무것도 쓰지 않았다"는 확정을 남길 방법이 없어지고</b>, 화면이 그 상태를
 * 저장본 없음({@code CALCULATED})과 구분하지 못한다.
 *
 * @see <a href="../../../../../../../../specs/006-backend-target-statistics/contracts/statistics-snapshot.md">statistics-snapshot.md §9~§12</a>
 */
@Service
public class StatisticsSaveService {

    private final UserRepository userRepository;
    private final UserStatisticsRepository statisticsRepository;
    private final UserStatisticsWeeklyRepository weeklyRepository;
    private final UserStatisticsExpendGroupRepository groupRepository;
    private final UserStatisticsPaymentMethodRepository methodRepository;
    private final StatisticsCalculator calculator;

    public StatisticsSaveService(UserRepository userRepository,
                                 UserStatisticsRepository statisticsRepository,
                                 UserStatisticsWeeklyRepository weeklyRepository,
                                 UserStatisticsExpendGroupRepository groupRepository,
                                 UserStatisticsPaymentMethodRepository methodRepository,
                                 StatisticsCalculator calculator) {
        this.userRepository = userRepository;
        this.statisticsRepository = statisticsRepository;
        this.weeklyRepository = weeklyRepository;
        this.groupRepository = groupRepository;
        this.methodRepository = methodRepository;
        this.calculator = calculator;
    }

    /**
     * 5.6 저장. <b>한 트랜잭션</b>이라 상세 삭제와 재삽입이 함께 커밋되거나 함께 되돌아간다.
     */
    @Transactional
    public StatisticsSaveResponse save(AuthPrincipal principal, YearMonthValue yearMonth) {
        requireNotFuture(yearMonth);

        User owner = userRepository.findById(principal.idKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        StatisticsCalculator.Result result = calculator.calculate(principal, yearMonth);

        UserStatistics statistics = upsertStatistics(owner, yearMonth, result);
        replaceDetails(owner, statistics, result);

        return new StatisticsSaveResponse(yearMonth.year(), yearMonth.month(),
                statistics.getSavedAt(), StatisticsResponse.SOURCE_SAVED,
                "%d년 %d월 통계가 저장되었습니다".formatted(yearMonth.year(), yearMonth.month()));
    }

    /**
     * 미래 월 거절 — <b>현재 연월을 초과</b>하면 {@code 3604}(FR-527).
     *
     * <p>미래를 막는 이유: 데이터가 아직 없어 합계 0 인 스냅샷이 굳는데 <b>저장본은
     * 불변</b>이라 나중에 그 달 지출이 쌓여도 조회는 계속 0 을 보여준다.
     *
     * <p><b>이번 달은 초과가 아니다.</b> {@code isAfter} 가 같은 달을 걸지 않으므로
     * 그대로 통과한다 — 진행 중이지만 실제 데이터가 있어 저장할 가치가 있고, 이후 다시
     * 불러 덮어쓸 수 있다.
     */
    private static void requireNotFuture(YearMonthValue yearMonth) {
        if (yearMonth.isAfter(YearMonthValue.now())) {
            throw new BusinessException(ErrorCode.STATISTICS_FUTURE_MONTH);
        }
    }

    /**
     * 통계 행 — <b>있으면 갱신</b>한다. 재저장해도 행이 늘지 않는다(FR-516).
     *
     * <p>{@code savedAt} 은 매번 새로 쓴다 — 화면의 "마지막 저장" 표시가 그 값이다.
     */
    private UserStatistics upsertStatistics(User owner, YearMonthValue yearMonth,
                                            StatisticsCalculator.Result result) {
        UserStatistics statistics = statisticsRepository
                .findByUserIdKeyAndYearAndMonth(owner.getIdKey(), yearMonth.year(),
                        yearMonth.month())
                .orElseGet(() -> {
                    UserStatistics created = new UserStatistics();
                    created.setUser(owner);
                    created.setYear(yearMonth.year());
                    created.setMonth(yearMonth.month());
                    return created;
                });

        statistics.setSavedAt(OffsetDateTime.now());
        statistics.setIncomeTotal(result.incomeTotal());
        statistics.setExpenseTotal(result.expenseTotal());
        statistics.setFixedAmount(result.fixedAmount());
        statistics.setRegularAmount(result.regularAmount());
        statistics.setFixedPercent(result.fixedPercent());
        statistics.setRegularPercent(result.regularPercent());
        return statisticsRepository.save(statistics);
    }

    /**
     * 상세 3종을 <b>전부 지우고 새로 넣는다</b>(FR-517).
     *
     * <p><b>삭제 뒤 {@code flush()} 가 반드시 필요하다.</b> 없으면 Hibernate 가 같은 flush
     * 안에서 INSERT 를 DELETE 보다 먼저 내보내 유니크 제약에 걸린다 — 재저장에서만 터지는
     * 실패다. 클래스 javadoc 에 자세히 적었다.
     */
    private void replaceDetails(User owner, UserStatistics statistics,
                                StatisticsCalculator.Result result) {
        Long statisticsIdx = statistics.getIdx();
        weeklyRepository.deleteByStatisticsIdx(statisticsIdx);
        groupRepository.deleteByStatisticsIdx(statisticsIdx);
        methodRepository.deleteByStatisticsIdx(statisticsIdx);

        // DELETE 를 먼저 내보낸다. 이 세 줄이 없으면 재저장이 9000 이 된다.
        weeklyRepository.flush();
        groupRepository.flush();
        methodRepository.flush();

        weeklyRepository.saveAll(weeklyEntities(owner, statistics, result.weeklyRows()));
        groupRepository.saveAll(groupEntities(owner, statistics, result.groupRows()));
        methodRepository.saveAll(methodEntities(owner, statistics, result.methodRows()));
    }

    private static List<UserStatisticsWeekly> weeklyEntities(
            User owner, UserStatistics statistics,
            List<StatisticsCalculator.WeeklyRow> rows) {
        List<UserStatisticsWeekly> entities = new ArrayList<>();
        for (StatisticsCalculator.WeeklyRow row : rows) {
            UserStatisticsWeekly entity = new UserStatisticsWeekly();
            entity.setUser(owner);
            entity.setStatistics(statistics);
            entity.setWeekIndex(row.weekIndex());
            entity.setWeekStart(row.weekStart());
            entity.setWeekEnd(row.weekEnd());
            entity.setAmount(row.amount());
            entities.add(entity);
        }
        return entities;
    }

    /**
     * 유형별 — <b>이름을 스냅샷으로 박는다</b>(FR-519).
     *
     * <p>FK 가 없으므로 원본이 삭제 표시돼도 이 행이 남고, 그때 화면을 복원하는 것이 이
     * 이름이다. 여기서 ID 만 저장하면 삭제된 유형의 통계를 그릴 수 없다.
     */
    private static List<UserStatisticsExpendGroup> groupEntities(
            User owner, UserStatistics statistics,
            List<StatisticsCalculator.GroupRow> rows) {
        List<UserStatisticsExpendGroup> entities = new ArrayList<>();
        for (StatisticsCalculator.GroupRow row : rows) {
            UserStatisticsExpendGroup entity = new UserStatisticsExpendGroup();
            entity.setUser(owner);
            entity.setStatistics(statistics);
            entity.setExpendGroupIdx(row.expendGroupIdx());
            entity.setExpendGroupName(row.expendGroupName());
            entity.setAmount(row.amount());
            entity.setTargetAmount(row.targetAmount());
            entity.setUsageRate(row.usageRate());
            entity.setStatus(row.status());
            entities.add(entity);
        }
        return entities;
    }

    /** 수단별 — 이름 스냅샷 규칙은 유형별과 같다. */
    private static List<UserStatisticsPaymentMethod> methodEntities(
            User owner, UserStatistics statistics,
            List<StatisticsCalculator.MethodRow> rows) {
        List<UserStatisticsPaymentMethod> entities = new ArrayList<>();
        for (StatisticsCalculator.MethodRow row : rows) {
            UserStatisticsPaymentMethod entity = new UserStatisticsPaymentMethod();
            entity.setUser(owner);
            entity.setStatistics(statistics);
            entity.setPaymentMethodIdx(row.paymentMethodIdx());
            entity.setPaymentMethodName(row.paymentMethodName());
            entity.setAmount(row.amount());
            entities.add(entity);
        }
        return entities;
    }
}

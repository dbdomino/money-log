package com.dbdomino.moneylog.backend.service.statistics;

import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.entity.UserExpendTargetDefault;
import com.dbdomino.moneylog.data.entity.UserExpendTargetMonthly;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.data.entity.UserIncome;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendTargetDefaultRepository;
import com.dbdomino.moneylog.data.repository.UserExpendTargetMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 그 달의 통계를 계산한다 — <b>5.5(즉석)와 5.6(저장)이 함께 쓰는 단 하나의 계산기</b>.
 *
 * <h2>이 기능에서 가장 중요한 분리다</h2>
 *
 * <p>각자 구현하면 <b>"지금 보이는 값"과 "저장된 값"이 달라진다.</b> 그게 바로 이 기능이
 * 방지하려는 상황이다 — 사용자가 `view=live` 로 최신을 보고 저장했는데 저장본이 다른
 * 숫자를 담으면 어느 쪽도 믿을 수 없게 된다.
 *
 * <h2>005 의 lazy 생성을 일으키지 않는다</h2>
 *
 * <p>고정지출 합계는 {@code tbl_fixed_expense_monthly} 를 <b>읽기만</b> 한다.
 * {@code FixedExpenseMonthlyService.ensureMonthlyRows} 를 부르면 안 된다 — 통계 조회가
 * 다른 테이블에 쓰는 부작용이 하나 더 생기고, 005·006 양쪽 Assumptions 이 "생성을
 * 일으키는 것은 4.5·4.8·4.9 셋뿐"으로 못박았다.
 *
 * <p>그 결과 <b>한 번도 열지 않은 달의 고정지출 합계는 0</b> 이다. 놀랄 수 있지만
 * 의도된 경계이며 quickstart #36 이 그것을 검증한다.
 *
 * <h2>유형별과 수단별이 정반대다</h2>
 *
 * <table border="1">
 *   <caption>0원 행을 어떻게 다루나</caption>
 *   <tr><th></th><th>0원 행</th><th>모집단</th></tr>
 *   <tr><td>유형별(FR-521)</td><td><b>뺀다</b></td><td>그 달 지출이 1건 이상인 유형</td></tr>
 *   <tr><td>수단별(FR-521a)</td><td><b>넣는다</b></td>
 *       <td>① 그 달 지출이 있는 수단(<b>상태 무관</b>) ∪ ② 사용 중인 {@code EXPENSE} 수단</td></tr>
 * </table>
 *
 * <p>②의 조건을 빠뜨리고 "회원 소유 수단 전부"로 읽으면 <b>버린 카드의 0원 행이 매달
 * 쌓인다.</b> 반대로 ①을 빠뜨리면 그 달에 실제로 쓴 카드가 삭제 표시됐다는 이유로
 * 합계에서 사라져 수단별 합이 지출 총액과 맞지 않는다.
 *
 * @see <a href="../../../../../../../../../specs/006-backend-target-statistics/contracts/statistics-snapshot.md">statistics-snapshot.md</a>
 */
@Component
public class StatisticsCalculator {

    /**
     * {@code usage_rate} 의 상한.
     *
     * <p>컬럼이 {@code numeric(6,2)} 라 {@code 9999.99} 가 최대다(덤프 확인). 목표
     * 1,000원에 지출 1,000만원이면 1,000,000% 가 나오는데 그대로 저장하면 <b>DB 오류가
     * {@code 9000} 으로 새어 나간다</b> — 사용자에게는 서버 오류로 보인다.
     */
    public static final BigDecimal MAX_USAGE_RATE = new BigDecimal("9999.99");

    /** 사용률 90% 미만. */
    public static final String STATUS_UNDER = "UNDER";

    /** 사용률 90 이상 110 이하. */
    public static final String STATUS_OK = "OK";

    /** 사용률 110% 초과. */
    public static final String STATUS_OVER = "OVER";

    private static final BigDecimal UNDER_THRESHOLD = new BigDecimal("90");
    private static final BigDecimal OVER_THRESHOLD = new BigDecimal("110");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final UserExpenseRepository expenseRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserFixedExpenseMonthlyRepository monthlyRepository;
    private final UserExpendTargetDefaultRepository defaultTargetRepository;
    private final UserExpendTargetMonthlyRepository monthlyTargetRepository;
    private final UserPaymentMethodRepository paymentMethodRepository;
    private final WeekBoundaryResolver weekBoundaryResolver;
    private final TargetResolver targetResolver;

    public StatisticsCalculator(UserExpenseRepository expenseRepository,
                                UserIncomeRepository incomeRepository,
                                UserFixedExpenseMonthlyRepository monthlyRepository,
                                UserExpendTargetDefaultRepository defaultTargetRepository,
                                UserExpendTargetMonthlyRepository monthlyTargetRepository,
                                UserPaymentMethodRepository paymentMethodRepository,
                                WeekBoundaryResolver weekBoundaryResolver,
                                TargetResolver targetResolver) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.monthlyRepository = monthlyRepository;
        this.defaultTargetRepository = defaultTargetRepository;
        this.monthlyTargetRepository = monthlyTargetRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.weekBoundaryResolver = weekBoundaryResolver;
        this.targetResolver = targetResolver;
    }

    /** 계산 결과 — 합계·비율 6값과 상세 3종. */
    public record Result(long incomeTotal, long expenseTotal, long fixedAmount,
                         long regularAmount, BigDecimal fixedPercent,
                         BigDecimal regularPercent, List<WeeklyRow> weeklyRows,
                         List<GroupRow> groupRows, List<MethodRow> methodRows) {
    }

    /** 주별 한 행. 경계를 함께 담아 저장본이 그대로 쓴다. */
    public record WeeklyRow(int weekIndex, LocalDate weekStart, LocalDate weekEnd, long amount) {
    }

    /** 유형별 한 행. 이름은 <b>계산 시점 스냅샷</b>이다(FR-519). */
    public record GroupRow(Long expendGroupIdx, String expendGroupName, long amount,
                           long targetAmount, BigDecimal usageRate, String status) {
    }

    /** 수단별 한 행. 이름은 <b>계산 시점 스냅샷</b>이다. */
    public record MethodRow(Long paymentMethodIdx, String paymentMethodName, long amount) {
    }

    /**
     * 그 달을 계산한다. <b>DB 를 바꾸지 않는다</b> — 읽기만 하는 순수 계산이다.
     *
     * <p>지출은 <b>일반 + 할부 + 고정</b> 셋을 합친다(005 의 4.8 과 같은 정의).
     * 그래야 가계부 화면의 합계와 통계의 합계가 맞는다.
     */
    public Result calculate(AuthPrincipal principal, YearMonthValue yearMonth) {
        Long idKey = principal.idKey();
        LocalDate from = yearMonth.firstDay();
        LocalDate to = yearMonth.lastDay();

        List<UserExpense> expenses =
                expenseRepository.findByUserIdKeyAndPaymentDateBetween(idKey, from, to);
        List<UserIncome> incomes =
                incomeRepository.findByUserIdKeyAndPaymentDateBetween(idKey, from, to);
        // 읽기만 한다 — ensureMonthlyRows 를 부르지 않는다(FR-513 · statistics-snapshot §2).
        List<UserFixedExpenseMonthly> fixedRows = monthlyRepository
                .findByUserIdKeyAndYearAndMonth(idKey, yearMonth.year(), yearMonth.month());

        long regularAmount = expenses.stream().mapToLong(UserExpense::getAmount).sum();
        long fixedAmount = fixedRows.stream()
                .mapToLong(UserFixedExpenseMonthly::getAmount).sum();
        long expenseTotal = regularAmount + fixedAmount;
        long incomeTotal = incomes.stream().mapToLong(UserIncome::getAmount).sum();

        return new Result(
                incomeTotal, expenseTotal, fixedAmount, regularAmount,
                percentOf(fixedAmount, expenseTotal),
                percentOf(regularAmount, expenseTotal),
                weeklyRows(yearMonth, expenses, fixedRows),
                groupRows(idKey, yearMonth, expenses, fixedRows),
                methodRows(idKey, expenses, fixedRows));
    }

    /**
     * 비율. <b>분모가 0 이면 0</b> 이다(FR-513).
     *
     * <p>컬럼이 {@code numeric(5,2)} NOT NULL 이라 어떤 경우에도 값을 채워야 하고,
     * 0 으로 나눌 수 없다. 비율은 정의상 100 을 넘지 않아 상한 걱정은 없다 —
     * {@code expenseTotal} 이 {@code fixedAmount + regularAmount} 이기 때문이다.
     */
    private static BigDecimal percentOf(long part, long total) {
        if (total == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** 주별 — 경계를 만들고 그 안에 떨어지는 지출을 더한다. 지출 0원인 주도 행을 남긴다. */
    private List<WeeklyRow> weeklyRows(YearMonthValue yearMonth, List<UserExpense> expenses,
                                       List<UserFixedExpenseMonthly> fixedRows) {
        List<WeeklyRow> rows = new ArrayList<>();
        for (WeekBoundaryResolver.Week week : weekBoundaryResolver.weeksOf(yearMonth)) {
            long amount = expenses.stream()
                    .filter(expense -> week.contains(expense.getPaymentDate()))
                    .mapToLong(UserExpense::getAmount).sum()
                    + fixedRows.stream()
                    .filter(row -> week.contains(row.getPaymentDate()))
                    .mapToLong(UserFixedExpenseMonthly::getAmount).sum();
            rows.add(new WeeklyRow(week.weekIndex(), week.start(), week.end(), amount));
        }
        return rows;
    }

    /**
     * 유형별 — <b>그 달 지출이 1건 이상인 유형만</b>(FR-521).
     *
     * <p>0원인 유형을 빼는 것이 수단별과 정반대다. 목표만 정해 두고 한 푼도 쓰지 않은
     * 유형까지 표에 넣으면 "쓴 것"을 보는 화면이 안 쓴 것으로 채워진다.
     */
    private List<GroupRow> groupRows(Long idKey, YearMonthValue yearMonth,
                                     List<UserExpense> expenses,
                                     List<UserFixedExpenseMonthly> fixedRows) {
        // 이름은 계산 시점 값으로 함께 모은다 — 저장되면 그것이 스냅샷이 된다.
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, Long> amounts = new LinkedHashMap<>();
        for (UserExpense expense : expenses) {
            Long groupIdx = expense.getExpendGroup().getIdx();
            names.putIfAbsent(groupIdx, expense.getExpendGroupName());
            amounts.merge(groupIdx, expense.getAmount(), Long::sum);
        }
        for (UserFixedExpenseMonthly row : fixedRows) {
            Long groupIdx = row.getExpendGroup().getIdx();
            // 고정지출은 이름을 저장하지 않으므로 연관의 현재 이름을 읽는다(005 FR-405).
            names.putIfAbsent(groupIdx, row.getExpendGroup().getName());
            amounts.merge(groupIdx, row.getAmount(), Long::sum);
        }

        Map<Long, Long> defaults = new LinkedHashMap<>();
        for (UserExpendTargetDefault target
                : defaultTargetRepository.findByUserIdKeyOrderByIdxAsc(idKey)) {
            defaults.put(target.getExpendGroup().getIdx(), target.getTargetAmount());
        }
        Map<Long, Long> monthlies = new LinkedHashMap<>();
        for (UserExpendTargetMonthly target : monthlyTargetRepository
                .findByUserIdKeyAndYearAndMonth(idKey, yearMonth.year(), yearMonth.month())) {
            monthlies.put(target.getExpendGroup().getIdx(), target.getTargetAmount());
        }

        List<GroupRow> rows = new ArrayList<>();
        amounts.forEach((groupIdx, amount) -> {
            long targetAmount = targetResolver.resolve(monthlies.get(groupIdx),
                    defaults.get(groupIdx));
            BigDecimal usageRate = usageRateOf(amount, targetAmount);
            rows.add(new GroupRow(groupIdx, names.get(groupIdx), amount, targetAmount,
                    usageRate, statusOf(usageRate)));
        });
        return rows;
    }

    /**
     * 수단별 — <b>두 집합의 합집합</b>(FR-521a).
     *
     * <pre>{@code
     * ① 그 달 지출이 1건 이상인 수단  → 상태와 무관하게 전부 (삭제 표시·사용 안 함 포함)
     * ② 지출 0원 행                  → 계산 시점 사용 중인 purpose=EXPENSE 수단만
     * }</pre>
     *
     * <p>②를 "회원 소유 전부"로 넓히면 <b>버린 카드의 0원 행이 매달 쌓인다.</b>
     * ①을 빠뜨리면 그 달에 실제로 쓴 카드가 삭제됐다는 이유로 사라져 수단별 합이
     * 지출 총액과 맞지 않는다.
     */
    private List<MethodRow> methodRows(Long idKey, List<UserExpense> expenses,
                                       List<UserFixedExpenseMonthly> fixedRows) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, Long> amounts = new LinkedHashMap<>();

        // ① 지출이 있는 수단. 상태를 묻지 않는다.
        for (UserExpense expense : expenses) {
            Long methodIdx = expense.getPaymentMethod().getIdx();
            names.putIfAbsent(methodIdx, expense.getPaymentMethodName());
            amounts.merge(methodIdx, expense.getAmount(), Long::sum);
        }
        for (UserFixedExpenseMonthly row : fixedRows) {
            Long methodIdx = row.getPaymentMethod().getIdx();
            names.putIfAbsent(methodIdx, row.getPaymentMethod().getName());
            amounts.merge(methodIdx, row.getAmount(), Long::sum);
        }

        // ② 0원 행 — 사용 중인 EXPENSE 수단만 채운다.
        for (UserPaymentMethod method : paymentMethodRepository
                .findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalseOrderByIdxAsc(
                        idKey, UserPaymentMethod.PURPOSE_EXPENSE)) {
            names.putIfAbsent(method.getIdx(), method.getName());
            amounts.putIfAbsent(method.getIdx(), 0L);
        }

        List<MethodRow> rows = new ArrayList<>();
        amounts.forEach((methodIdx, amount) ->
                rows.add(new MethodRow(methodIdx, names.get(methodIdx), amount)));
        return rows;
    }

    /**
     * 사용률 — <b>상한 {@link #MAX_USAGE_RATE} 에서 자른다</b>(FR-522).
     *
     * <p>100% 를 넘을 수 있고 1000% 도 표현해야 하지만 컬럼이 {@code numeric(6,2)} 다.
     * 자르지 않으면 저장에서 DB 오류가 나고 사용자에게는 {@code 9000} 으로 보인다.
     *
     * <p><b>목표가 0 이면 0</b> 이다 — 나눗셈이 성립하지 않는다.
     */
    private static BigDecimal usageRateOf(long amount, long targetAmount) {
        if (targetAmount == 0L) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = BigDecimal.valueOf(amount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(targetAmount), 2, RoundingMode.HALF_UP);
        return rate.compareTo(MAX_USAGE_RATE) > 0 ? MAX_USAGE_RATE : rate;
    }

    /**
     * 목표 대비 상태 — {@code UNDER} / {@code OK} / {@code OVER}(FR-523).
     *
     * <pre>{@code
     * 90 미만        → UNDER
     * 90 이상 110 이하 → OK
     * 110 초과        → OVER
     * }</pre>
     *
     * <p><b>경계가 양쪽 다 포함이다.</b> 정확히 90 과 정확히 110 은 {@code OK} 다 —
     * 한쪽만 잘못 잡으면 그 값에 걸린 유형이 반대로 분류되는데 응답은 성공이라 조용히 틀린다.
     *
     * <p>목표가 0 이면 사용률이 0 이므로 {@code UNDER} 다.
     *
     * <p><b>채우지 않으면 저장이 통째로 실패한다</b> — {@code status} 가
     * {@code varchar(10) NOT NULL} 이고 {@code ck_stat_group_status} 가 이 세 값만 받는다.
     */
    private static String statusOf(BigDecimal usageRate) {
        if (usageRate.compareTo(UNDER_THRESHOLD) < 0) {
            return STATUS_UNDER;
        }
        return usageRate.compareTo(OVER_THRESHOLD) > 0 ? STATUS_OVER : STATUS_OK;
    }
}

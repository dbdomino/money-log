package com.dbdomino.moneylog.backend.service.ledger;

import com.dbdomino.moneylog.backend.dto.request.LedgerMonthlyListQuery;
import com.dbdomino.moneylog.backend.dto.response.LedgerItemResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 네 출처를 읽어 <b>한 목록으로 합치고</b> 필터·정렬을 적용한다.
 *
 * <h2>SQL {@code UNION} 을 쓰지 않는다</h2>
 *
 * <p>네 출처의 컬럼 구성이 다르다.
 *
 * <table border="1">
 *   <caption>출처별 차이</caption>
 *   <tr><th>출처</th><th>지출유형</th><th>장소</th><th>할부</th><th>이름의 성격</th></tr>
 *   <tr><td>{@code tbl_expense}(일반)</td><td>있다</td><td>있다</td><td>NULL</td><td>스냅샷</td></tr>
 *   <tr><td>{@code tbl_expense}(할부)</td><td>있다</td><td>있다</td><td>채워짐</td><td>스냅샷</td></tr>
 *   <tr><td>{@code tbl_income}</td><td><b>없다</b></td><td><b>없다</b></td><td>없다</td><td>스냅샷</td></tr>
 *   <tr><td>{@code tbl_fixed_expense_monthly}</td><td>있다</td><td><b>없다</b></td><td>없다</td><td><b>현재 이름</b></td></tr>
 * </table>
 *
 * <p>{@code UNION} 으로 맞추려면 없는 컬럼을 {@code NULL} 로 채워야 하는데, 그러면
 * <b>"값이 없다"와 "컬럼이 아예 없다"가 SQL 에서 구분되지 않는다.</b> 게다가 고정지출
 * 행의 이름은 조인해서 현재 값을 읽어야 해 다른 셋과 조회 형태가 다르다.
 *
 * <h2>필터도 SQL 로 밀지 않는다</h2>
 *
 * <p>같은 이유다. {@code expendGroupId} 필터는 소득 행을 <b>전부</b> 떨어뜨려야 하고
 * ({@code tbl_income} 에 그 컬럼이 없다), {@code keyword} 는 소득에 장소가 없어 내용만
 * 봐야 한다. 조립 후 적용하는 편이 이 차이를 다루기 쉽다.
 *
 * <p><b>페이징이 없어 메모리 정렬이 성립한다</b>(FR-422). 한 회원의 한 달 거래 건수가
 * 규모의 상한이다.
 *
 * @see <a href="../../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/ledger-list.md">ledger-list.md §1·§2·§5</a>
 */
@Component
public class LedgerAssembler {

    private final UserExpenseRepository expenseRepository;
    private final UserIncomeRepository incomeRepository;
    private final UserFixedExpenseMonthlyRepository monthlyRepository;
    private final LedgerItemFactory itemFactory;

    public LedgerAssembler(UserExpenseRepository expenseRepository,
                           UserIncomeRepository incomeRepository,
                           UserFixedExpenseMonthlyRepository monthlyRepository,
                           LedgerItemFactory itemFactory) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.monthlyRepository = monthlyRepository;
        this.itemFactory = itemFactory;
    }

    /**
     * 그 달의 네 출처를 <b>필터 없이</b> 전부 읽어 공통 형태로 바꾼다.
     *
     * <p>필터를 여기서 걸지 않는 것은 <b>합계가 필터와 무관해야</b> 하기 때문이다 —
     * 호출자가 이 결과로 {@code expenseTotal}·{@code incomeTotal} 을 먼저 내고, 그다음
     * {@link #narrow} 로 목록만 좁힌다.
     *
     * <p>지출·소득은 <b>날짜 범위</b>로 읽는다. 저장 구조가 연·월을 따로 갖지 않고
     * {@code payment_date} 하나만 갖기 때문이다 — 그 달 1일~말일을 만들어 넘긴다.
     * 고정지출은 연·월 컬럼이 있어 그대로 조회한다.
     */
    public List<LedgerItemResponse> assemble(AuthPrincipal principal, YearMonthValue yearMonth) {
        LocalDate from = yearMonth.firstDay();
        LocalDate to = yearMonth.lastDay();

        List<LedgerItemResponse> items = new ArrayList<>();
        expenseRepository.findByUserIdKeyAndPaymentDateBetween(principal.idKey(), from, to)
                .forEach(expense -> items.add(itemFactory.fromExpense(expense)));
        incomeRepository.findByUserIdKeyAndPaymentDateBetween(principal.idKey(), from, to)
                .forEach(income -> items.add(itemFactory.fromIncome(income)));
        monthlyRepository.findByUserIdKeyAndYearAndMonth(
                        principal.idKey(), yearMonth.year(), yearMonth.month())
                .forEach(monthly -> items.add(itemFactory.fromFixedExpense(monthly)));
        return items;
    }

    /**
     * 필터를 적용하고 정렬한다.
     *
     * <p>정렬 기본은 {@code paymentDate} {@code desc} 다 — 명시하지 않으면 최근 것이 위다.
     * <b>2차 정렬로 {@code ledgerItemId} 를 건다.</b> 같은 날짜·같은 금액인 행들의 순서가
     * 매 호출마다 흔들리면 화면이 이유 없이 다르게 보이기 때문이다.
     */
    public List<LedgerItemResponse> narrow(List<LedgerItemResponse> items,
                                           LedgerMonthlyListQuery query) {
        Comparator<LedgerItemResponse> comparator = LedgerMonthlyListQuery.SORT_AMOUNT
                .equals(query.sort())
                ? Comparator.comparing(LedgerItemResponse::amount)
                : Comparator.comparing(LedgerItemResponse::paymentDate);
        if (!query.ascending()) {
            comparator = comparator.reversed();
        }
        // 동점을 가르는 축. 방향을 뒤집지 않아 asc·desc 어느 쪽이든 안정적이다.
        comparator = comparator.thenComparing(LedgerItemResponse::ledgerItemId);

        return items.stream()
                .filter(item -> matches(item, query))
                .sorted(comparator)
                .toList();
    }

    /** 다섯 필터를 모두 통과하는가. */
    private static boolean matches(LedgerItemResponse item, LedgerMonthlyListQuery query) {
        if (!query.includesType(item.type())) {
            return false;
        }
        if (query.paymentMethodId() != null
                && !query.paymentMethodId().equals(item.paymentMethodId())) {
            return false;
        }
        if (query.expendGroupId() != null) {
            // 소득 행은 expendGroupId 가 null 이라 여기서 전부 떨어진다 — 의도한 동작이다.
            // 소득에는 지출유형이 없으므로 "그 유형의 거래"에 소득이 낄 자리가 없다.
            if (!query.expendGroupId().equals(item.expendGroupId())) {
                return false;
            }
        }
        if (query.dateFrom() != null && item.paymentDate().isBefore(query.dateFrom())) {
            return false;
        }
        if (query.dateTo() != null && item.paymentDate().isAfter(query.dateTo())) {
            return false;
        }
        return matchesKeyword(item, query.keyword());
    }

    /**
     * 장소·내용 부분 일치. <b>대소문자를 무시한다</b>(ledger-list.md § 정한 것).
     *
     * <p>손으로 적은 자유 문자열이라 대소문자가 일정하지 않다 — 구분하면 자기가 적은 것을
     * 자기가 못 찾는다.
     *
     * <p><b>소득·고정지출에는 장소가 없어 내용만 본다.</b> {@code null} 을 빈 문자열로
     * 다루면 "검색어가 빈 문자열일 때 전부 걸리는" 문제가 생기므로 각각 따로 본다.
     */
    private static boolean matchesKeyword(LedgerItemResponse item, String keyword) {
        if (keyword == null) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        return contains(item.place(), needle) || contains(item.content(), needle);
    }

    private static boolean contains(String value, String lowerCaseNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
    }
}

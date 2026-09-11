package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.dto.request.LedgerMonthlyListQuery;
import com.dbdomino.moneylog.backend.dto.response.LedgerItemResponse;
import com.dbdomino.moneylog.backend.dto.response.LedgerMonthlyListResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ledger.LedgerAssembler;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 4.8 월별 가계부 통합 목록.
 *
 * <h2>순서가 규칙이다</h2>
 *
 * <pre>{@code
 * 1. lazy 생성 — 그 달의 고정지출 내역을 만든다 (FR-418)
 * 2. 조립     — 네 출처를 읽어 한 목록으로 합친다
 * 3. 합계     — 필터를 적용하기 전에 낸다
 * 4. 필터·정렬 — 목록만 좁힌다
 * }</pre>
 *
 * <p><b>1번을 빠뜨리면 그 달을 처음 여는 사용자에게 고정지출이 보이지 않는다.</b>
 * 4.5 와 <b>같은 규칙으로</b> 만들어야 하므로 {@link FixedExpenseMonthlyService} 의
 * 메서드를 그대로 부른다 — 각자 구현하면 같은 달이 어느 API 로 처음 열렸는지에 따라
 * 값이 갈린다.
 *
 * <p><b>3번이 4번보다 앞이다.</b> 합계는 필터와 무관한 그 달 전체 기준이기 때문이다
 * (ledger-list.md § 정한 것). 순서를 뒤집으면 {@code type=INCOME} 으로 조회했을 때
 * {@code expenseTotal} 이 0 이 되어 화면 상단 요약이 필터마다 흔들린다.
 *
 * <h2>lazy 생성의 경계</h2>
 *
 * <p>생성은 <b>그 달을 처음 열 때</b>만 일어난다. 이미 연 달에 고정지출을 새로 등록해도
 * 그 달 목록은 자동으로 늘지 않는다 — 반영하려면 4.9(재작성)를 부른다. 그것이 자동과
 * 수동의 경계다.
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/ledger-list.md">ledger-list.md §2·§6</a>
 */
@Service
public class LedgerService {

    private final FixedExpenseMonthlyService monthlyService;
    private final LedgerAssembler assembler;

    public LedgerService(FixedExpenseMonthlyService monthlyService, LedgerAssembler assembler) {
        this.monthlyService = monthlyService;
        this.assembler = assembler;
    }

    /**
     * 4.8 조회. <b>이 GET 은 상태를 바꾼다</b>(FR-418).
     *
     * <p>필터가 걸려 있어도 <b>그 달 전체</b>를 생성한다. 4.5 가 필터로 생성 대상을 좁히지
     * 않는 것과 같은 이유이며(FR-406), 여기서 좁히면 두 API 가 만드는 결과가 갈린다.
     */
    @Transactional
    public LedgerMonthlyListResponse monthly(AuthPrincipal principal,
                                             LedgerMonthlyListQuery query) {
        YearMonthValue yearMonth = query.yearMonth();

        // 1. 4.5 와 같은 규칙으로 그 달의 고정지출 내역을 만든다.
        monthlyService.ensureMonthlyRows(principal, yearMonth);

        // 2. 네 출처를 필터 없이 전부 읽어 합친다.
        List<LedgerItemResponse> all = assembler.assemble(principal, yearMonth);

        // 3. 합계는 필터 이전 값으로 낸다.
        long expenseTotal = sumOf(all, LedgerItemResponse.TYPE_EXPENSE,
                LedgerItemResponse.TYPE_INSTALLMENT, LedgerItemResponse.TYPE_FIXED);
        long incomeTotal = sumOf(all, LedgerItemResponse.TYPE_INCOME);

        // 4. 목록만 좁히고 정렬한다.
        List<LedgerItemResponse> items = assembler.narrow(all, query);

        return new LedgerMonthlyListResponse(
                items, yearMonth.year(), yearMonth.month(), expenseTotal, incomeTotal);
    }

    /**
     * 그 종류들의 금액 합.
     *
     * <p>{@code expenseTotal} 이 <b>일반 + 할부 + 고정</b> 셋을 합친 값인 것이 이 API 의
     * 특징이다. 할부를 빠뜨리면 이번 달 카드값이 빠지고, 고정지출을 빠뜨리면 월세가 빠진다.
     */
    private static long sumOf(List<LedgerItemResponse> items, String... types) {
        List<String> wanted = List.of(types);
        return items.stream()
                .filter(item -> wanted.contains(item.type()))
                .mapToLong(LedgerItemResponse::amount)
                .sum();
    }
}

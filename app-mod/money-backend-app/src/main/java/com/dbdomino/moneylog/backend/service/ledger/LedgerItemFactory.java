package com.dbdomino.moneylog.backend.service.ledger;

import com.dbdomino.moneylog.backend.dto.response.LedgerItemResponse;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.data.entity.UserIncome;
import org.springframework.stereotype.Component;

/**
 * 네 출처의 행을 <b>공통 형태</b>({@link LedgerItemResponse})로 바꾼다.
 *
 * <h2>이름 규칙이 갈리는 단 하나의 자리다</h2>
 *
 * <p>같은 목록 안에서 {@code paymentMethodName} 이 어디서 오는지가 행 종류마다 다르다
 * (FR-419·FR-425).
 *
 * <pre>{@code
 * EXPENSE · INSTALLMENT · INCOME  →  Entity 자신의 이름 컬럼   (등록 당시 스냅샷)
 * FIXED                           →  연관을 타고 원본에서 읽음  (조회 시점 현재 이름)
 * }</pre>
 *
 * <p><b>이 판정이 두 곳에 생기면 반드시 갈린다.</b> 그래서 네 변환을 한 클래스에 모았다 —
 * 나란히 두면 "왜 여기만 다른가"가 눈에 보이고, 흩어 두면 누군가 통일하려 든다.
 *
 * <p>수단 이름을 바꾼 뒤 같은 달을 재조회하면 {@code FIXED} 행만 새 이름이 된다.
 * 그것이 맞다 — 고정지출은 "지금 유효한 설정"이고 이미 쓴 지출은 "과거 기록"이다.
 *
 * <h2>{@code ledgerItemId} 는 종류별 접두사를 붙인 문자열이다</h2>
 *
 * <pre>{@code
 * EXPENSE · INSTALLMENT  →  expense:{expenseId}
 * INCOME                 →  income:{incomeId}
 * FIXED                  →  fixed:{fixedExpenseId}:{year}:{month}
 * }</pre>
 *
 * <p>네 출처의 PK 가 서로 다른 테이블에서 나오므로 숫자만으로는 {@code expense.idx = 5} 와
 * {@code income.idx = 5} 가 구분되지 않는다.
 *
 * <p><b>{@code FIXED} 만 세 조각인 이유</b>는 {@code fixedExpenseId} 만으로 월별 내역 1행이
 * 특정되지 않기 때문이다 — 같은 고정지출이 여러 달에 걸쳐 행을 갖는다. 유니크 제약
 * {@code ux_fixed_expense_monthly (fixed_expense_idx, year, month)} 와 같은 조합이다.
 *
 * <p>월별 내역의 PK({@code idx})를 쓰지 않는 것은 <b>프론트가 그 값으로 4.6 을 부를 수
 * 없기 때문</b>이다 — 4.6 의 Path 가 {@code /monthly/{year}/{month}/{fixedExpenseId}} 다.
 *
 * @see <a href="../../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/ledger-list.md">ledger-list.md §3·§4</a>
 */
@Component
public class LedgerItemFactory {

    /**
     * 일반 지출 또는 할부 — <b>{@code installment_group_id} 의 유무로 가른다</b>.
     *
     * <p>둘은 같은 테이블의 같은 형태 행이고 차이는 할부 3컬럼뿐이다. 004 가 그 셋을
     * "전부 비거나 전부 채워진다"로 유지하므로 하나만 봐도 판정이 선다.
     *
     * <p><b>이름은 Entity 자신의 컬럼에서 읽는다</b>(스냅샷). 004 가 등록 시점 이름을
     * 저장해 두었고, 원본 이름이 바뀌어도 따라가지 않는 것이 지출의 규칙이다.
     */
    public LedgerItemResponse fromExpense(UserExpense expense) {
        boolean installment = expense.getInstallmentGroupId() != null;
        return new LedgerItemResponse(
                "expense:" + expense.getIdx(),
                installment ? LedgerItemResponse.TYPE_INSTALLMENT
                        : LedgerItemResponse.TYPE_EXPENSE,
                expense.getIdx(),
                expense.getPaymentDate(),
                expense.getAmount(),
                expense.getPaymentMethod().getIdx(),
                expense.getPaymentMethodName(),
                expense.getExpendGroup().getIdx(),
                expense.getExpendGroupName(),
                expense.getPlace(),
                expense.getContent(),
                null,
                expense.getInstallmentGroupId(),
                expense.getInstallmentIndex(),
                expense.getInstallmentTotal());
    }

    /**
     * 소득.
     *
     * <p><b>지출유형과 장소가 {@code null} 이다.</b> 값이 없어서가 아니라
     * {@code tbl_income} 에 그 컬럼이 <b>아예 없기</b> 때문이다 — 소득에는 그 개념이 없다.
     * 그래서 {@code expendGroupId} 필터가 걸리면 소득 행은 전부 빠진다.
     */
    public LedgerItemResponse fromIncome(UserIncome income) {
        return new LedgerItemResponse(
                "income:" + income.getIdx(),
                LedgerItemResponse.TYPE_INCOME,
                income.getIdx(),
                income.getPaymentDate(),
                income.getAmount(),
                income.getPaymentMethod().getIdx(),
                income.getPaymentMethodName(),
                null,
                null,
                null,
                income.getContent(),
                null,
                null,
                null,
                null);
    }

    /**
     * 고정지출의 그 달 내역.
     *
     * <p><b>여기만 이름을 연관에서 읽는다</b> — {@code tbl_fixed_expense_monthly} 에는 이름
     * 컬럼이 하나도 없어 읽을 자리가 없고, 그것이 001 이 스키마로 못박은 결정이다.
     *
     * <p>{@code sourceId} 는 <b>고정지출 설정의 PK</b>이지 이 행의 {@code idx} 가 아니다.
     * 프론트가 4.6 을 부를 때 필요한 값이 그쪽이다.
     *
     * <p>{@code place} 가 {@code null} 인 것도 컬럼이 없어서다 — 고정지출에 장소 개념이 없다.
     */
    public LedgerItemResponse fromFixedExpense(UserFixedExpenseMonthly monthly) {
        Long fixedExpenseId = monthly.getFixedExpense().getIdx();
        return new LedgerItemResponse(
                "fixed:%d:%d:%d".formatted(fixedExpenseId, monthly.getYear(), monthly.getMonth()),
                LedgerItemResponse.TYPE_FIXED,
                fixedExpenseId,
                monthly.getPaymentDate(),
                monthly.getAmount(),
                monthly.getPaymentMethod().getIdx(),
                // 연관에서 읽는다. 위 두 메서드와 정반대이며 그것이 이 기능의 규칙이다.
                monthly.getPaymentMethod().getName(),
                monthly.getExpendGroup().getIdx(),
                monthly.getExpendGroup().getName(),
                null,
                monthly.getContent(),
                monthly.getFixedExpense().getName(),
                null,
                null,
                null);
    }
}

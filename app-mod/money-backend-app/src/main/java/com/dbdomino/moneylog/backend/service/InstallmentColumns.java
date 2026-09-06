package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserExpense;

/**
 * 할부 3개 컬럼의 불변식 — <b>셋 다 비거나 셋 다 채워진다</b>.
 *
 * <pre>{@code
 * 일시불:  installment_group_id = NULL
 *         installment_index    = NULL
 *         installment_total    = NULL
 *
 * 할부:    installment_group_id = 시퀀스 값 (그룹 내 N개 행이 공유)
 *         installment_index    = 1 .. N
 *         installment_total    = N  (N >= 2)
 * }</pre>
 *
 * <h2>DB 가 막아주지 않는다</h2>
 *
 * <p>CHECK 은 {@code ck_expense_installment_index}(NULL 또는 1 이상)와
 * {@code ck_expense_installment_total}(NULL 또는 2 이상) 둘뿐이고, 둘 다 <b>각 컬럼의
 * 범위만</b> 본다. 세 컬럼의 동시성을 보는 제약이 없어 <b>{@code installment_index} 만
 * 채운 행을 DB 가 받아들인다</b>.
 *
 * <p>그런 행이 생기면 "일시불인가 할부인가"를 판정할 수 없고, 조회·집계·중도상환이 전부
 * 그 판정에 의존하므로 한 번 오염되면 <b>조용히 틀린 답</b>이 나온다. 예외도 오류 응답도
 * 나지 않는다.
 *
 * <p><b>DB 에 CHECK 을 추가하지 않는다.</b> 스키마 변경이 되는데 이 기능은 무변경이
 * 전제이고(헌장 원칙 VI), 애플리케이션에서 막을 수 있는 것을 위해 001 의 결정을 번복하지
 * 않는다. 필요해지면 별도 작업으로 다룬다.
 *
 * <h2>왜 독립 클래스인가</h2>
 *
 * <p>저장 진입점이 <b>셋</b>인데 서로 다른 클래스다 — {@code ExpenseService}(3.1 일시불) ·
 * {@code InstallmentService}(3.5 할부) · {@code ExcelImportService}(3.12 엑셀). 한 서비스
 * 안의 헬퍼로 두면 나머지 둘이 그 서비스를 부르거나(레이어가 꼬인다) 규칙을 복제하게 된다.
 *
 * <p>Entity 의 {@code @PrePersist} 에 두지 않는 것은 실패가 {@code PersistenceException}
 * 으로 올라와 {@code 3201}·{@code 3204} 같은 업무 코드로 바꾸기 어렵기 때문이다
 * (research.md §5). 서비스에서 미리 판정해야 응답 코드를 통제한다.
 *
 * @see <a href="../../../../../../../../specs/004-backend-expense-income/contracts/api-contract.md">api-contract.md §8</a>
 */
public final class InstallmentColumns {

    /** 할부의 최소 개월 수. 1개월 할부는 일시불이므로 허용하지 않는다(FR-311). */
    public static final int MIN_MONTHS = 2;

    private InstallmentColumns() {
    }

    /**
     * 일시불로 만든다 — 세 컬럼을 <b>전부</b> 비운다.
     *
     * <p>3.1(지출 등록)과 3.12(엑셀 업로드)가 쓴다. 엑셀 양식에는 할부 컬럼이 없어
     * 업로드가 만드는 지출은 예외 없이 일시불이다(excel-contract.md §4).
     */
    public static void markAsLumpSum(UserExpense expense) {
        expense.setInstallmentGroupId(null);
        expense.setInstallmentIndex(null);
        expense.setInstallmentTotal(null);
    }

    /**
     * 할부 회차로 만든다 — 세 컬럼을 <b>전부</b> 채운다.
     *
     * <p>3.5(할부 등록)만 쓴다. {@code groupId} 는 그룹 전체가 공유하는 시퀀스 값이므로
     * <b>호출자가 그룹당 한 번 채번해</b> N번의 호출에 같은 값을 넘긴다(FR-312).
     *
     * @param index 회차. 1부터 시작한다
     * @param total 총 개월 수. {@link #MIN_MONTHS} 이상이어야 한다
     * @throws BusinessException {@code 3204} — 회차·개월 수가 불변식을 벗어났다
     */
    public static void markAsInstallment(UserExpense expense, Long groupId, int index, int total) {
        requireValidInstallment(groupId, index, total);
        expense.setInstallmentGroupId(groupId);
        expense.setInstallmentIndex(index);
        expense.setInstallmentTotal(total);
    }

    /**
     * 할부 값이 성립하는가. 저장 전에 부른다.
     *
     * <p>{@code index > total} 을 막는 것이 CHECK 과의 차이다 — DB 는 둘을 따로 볼 뿐
     * "12개월 할부의 13회차" 같은 조합을 알지 못한다.
     */
    private static void requireValidInstallment(Long groupId, int index, int total) {
        if (groupId == null || total < MIN_MONTHS || index < 1 || index > total) {
            throw new BusinessException(ErrorCode.INSTALLMENT_VALIDATION_FAILED);
        }
    }

    /**
     * 이 행이 할부 회차인가. 세 컬럼이 <b>모두</b> 채워졌을 때만 참이다.
     *
     * <p>{@code installmentGroupId != null} 하나만 보고 판정하지 않는다 — 부분 채움 행이
     * 어쩌다 생겼을 때 그것을 할부로 오인하면 중도상환이 엉뚱한 행을 지운다.
     */
    public static boolean isInstallment(UserExpense expense) {
        return expense.getInstallmentGroupId() != null
                && expense.getInstallmentIndex() != null
                && expense.getInstallmentTotal() != null;
    }

    /**
     * 세 컬럼이 불변식을 지키는가 — <b>셋 다 비었거나 셋 다 채워졌는가</b>.
     *
     * <p>저장 직전 마지막 방어선으로 쓴다. {@link #markAsLumpSum}·
     * {@link #markAsInstallment} 만 거치면 위반이 생길 수 없지만, 나중에 다른 경로가
     * 컬럼을 직접 건드리면 여기서 걸린다.
     */
    public static boolean isConsistent(UserExpense expense) {
        boolean allNull = expense.getInstallmentGroupId() == null
                && expense.getInstallmentIndex() == null
                && expense.getInstallmentTotal() == null;
        return allNull || isInstallment(expense);
    }
}

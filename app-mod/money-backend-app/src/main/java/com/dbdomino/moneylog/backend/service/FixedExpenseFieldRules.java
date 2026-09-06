package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 고정지출 설정의 값 검증 — 전부 {@code 3401} 이다(FR-403·FR-404).
 *
 * <h2>왜 독립 클래스인가</h2>
 *
 * <p>검증이 필요한 경로가 <b>둘인데 입력 형태가 다르다</b>.
 *
 * <table border="1">
 *   <caption>같은 규칙, 다른 입력</caption>
 *   <tr><th>경로</th><th>입력</th><th>특징</th></tr>
 *   <tr><td>4.1 등록</td><td>{@code FixedExpenseCreateRequest}</td><td>전 필드가 필수</td></tr>
 *   <tr><td>4.4 수정</td><td>{@code Map<String, Object>}</td><td>보낸 필드만. omit = 유지</td></tr>
 * </table>
 *
 * <p>각자 검증하면 <b>등록은 막고 수정은 통과하는 구멍</b>이 생긴다. 실제로 위험한 것은
 * 기간 검증이다 — 수정은 시작만 보내고 종료를 omit 할 수 있어서, 보낸 값만 보면
 * "시작 2027-05, 종료(기존) 2027-02" 라는 뒤집힌 상태가 통과한다. 그러면 DB CHECK
 * {@code ck_fixed_expense_period} 가 걸려 {@code 3401} 이 아니라 <b>{@code 9000}</b> 이
 * 나간다 — 사용자에게는 서버 오류로 보인다.
 *
 * <p>그래서 {@link #requirePeriod} 는 <b>병합 후 최종 값</b>을 받는다. 호출자가 기존 값과
 * 요청 값을 합친 뒤 넘긴다.
 *
 * <h2>DB CHECK 과 같은 규칙을 애플리케이션에도 두는 이유</h2>
 *
 * <p>덤프에 {@code ck_fixed_expense_amount}(0 초과) · {@code ck_fixed_expense_day}(1~31) ·
 * {@code ck_fixed_expense_start_month}/{@code end_month}(1~12) ·
 * {@code ck_fixed_expense_period} 가 이미 있다. DB 가 막아 주는데도 여기서 다시 보는 것은
 * <b>실패 코드 때문</b>이다. CHECK 위반은 {@code DataIntegrityViolationException} 으로
 * 올라와 {@code 9000} 이 되는데, 사용자가 받아야 할 답은 "서버 오류"가 아니라 "값이
 * 잘못됐다({@code 3401})"이다.
 *
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/contracts/api-contract.md">api-contract.md §6</a>
 */
public final class FixedExpenseFieldRules {

    /** 매달 결제일의 하한. */
    public static final int MIN_PAYMENT_DAY = 1;

    /**
     * 매달 결제일의 상한.
     *
     * <p><b>31 을 허용한다.</b> 2월에 그런 날이 없다는 것은 여기서 막을 문제가 아니다 —
     * 말일 보정이 월별 내역을 만들 때 일어나며(FR-409), 여기서 막으면 매월 말일에 나가는
     * 고정지출을 표현할 수 없다.
     */
    public static final int MAX_PAYMENT_DAY = 31;

    /** 이름 길이 상한. {@code tbl_fixed_expense.name} 이 {@code varchar(50)} 이다. */
    public static final int NAME_MAX = 50;

    /** 내용 길이 상한. {@code content} 가 {@code varchar(255)} 다. */
    public static final int CONTENT_MAX = 255;

    private FixedExpenseFieldRules() {
    }

    /** 금액은 0보다 커야 한다. {@code null} 도 거절한다. */
    public static long requireAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "금액은 0보다 커야 합니다.");
        }
        return amount;
    }

    /** 결제일은 1~31 이어야 한다. */
    public static int requirePaymentDay(Integer dayOfMonth) {
        if (dayOfMonth == null || dayOfMonth < MIN_PAYMENT_DAY || dayOfMonth > MAX_PAYMENT_DAY) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "결제일은 %d 에서 %d 사이여야 합니다.".formatted(MIN_PAYMENT_DAY, MAX_PAYMENT_DAY));
        }
        return dayOfMonth;
    }

    /**
     * 적용 기간 — <b>병합 후 최종 값</b>을 받는다.
     *
     * <p>월 1~12 를 먼저 보고, 그다음 {@code 연 × 12 + 월} 합성값으로 종료가 시작보다
     * 앞서지 않는지 본다. <b>연과 월을 따로 비교하면 안 된다</b> — "2026-12 시작,
     * 2027-01 종료"가 월만 보면 12 &gt; 1 이라 거절되고, "2027-01 시작, 2026-12 종료"는
     * 1 &lt; 12 라 통과한다. 둘 다 반대로 판정된다.
     *
     * <p>양 끝을 포함한다 — 시작과 종료가 같은 <b>한 달짜리 설정이 유효하다</b>.
     *
     * @return 검증을 통과한 시작·종료
     */
    public static Period requirePeriod(Integer startYear, Integer startMonth,
                                       Integer endYear, Integer endMonth) {
        if (startYear == null || startMonth == null || endYear == null || endMonth == null) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "적용 기간의 연·월은 필수입니다.");
        }
        YearMonthValue start = YearMonthValue.require(
                startYear, startMonth, ErrorCode.FIXED_EXPENSE_FIELD_INVALID);
        YearMonthValue end = YearMonthValue.require(
                endYear, endMonth, ErrorCode.FIXED_EXPENSE_FIELD_INVALID);
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "종료 연월은 시작 연월보다 앞설 수 없습니다.");
        }
        return new Period(start, end);
    }

    /** 이름은 비어 있지 않고 50자 이하여야 한다. */
    public static String requireName(String name) {
        return requireText(name, NAME_MAX, "이름");
    }

    /** 내용은 비어 있지 않고 255자 이하여야 한다. */
    public static String requireContent(String content) {
        return requireText(content, CONTENT_MAX, "내용");
    }

    private static String requireText(String value, int max, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    label + "은(는) 필수입니다.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID,
                    "%s은(는) %d자 이하여야 합니다.".formatted(label, max));
        }
        return trimmed;
    }

    /**
     * 검증을 통과한 적용 기간.
     *
     * <p>{@link YearMonthValue} 두 개로 들고 다니다가 저장 직전에 연·월 네 컬럼으로
     * 흩어진다. 그 사이의 판정(FR-408 생성 대상 · FR-412 자동 반영 범위)은 전부 이 형태로
     * 한다 — 흩어진 정수 넷을 들고 다니면 어느 짝이 시작이고 끝인지 매번 다시 읽어야 한다.
     */
    public record Period(YearMonthValue start, YearMonthValue end) {
    }
}

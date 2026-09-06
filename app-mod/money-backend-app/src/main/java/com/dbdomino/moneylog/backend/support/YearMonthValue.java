package com.dbdomino.moneylog.backend.support;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 연·월 한 쌍. <b>{@code 연 × 12 + 월} 합성 비교를 가두는 자리</b>다.
 *
 * <h2>왜 값 객체인가</h2>
 *
 * <p>이 비교가 005 에서 <b>네 곳</b>에 나온다.
 *
 * <table border="1">
 *   <caption>합성 비교가 필요한 지점</caption>
 *   <tr><th>FR</th><th>무엇을 판단하나</th></tr>
 *   <tr><td>FR-404</td><td>등록 검증 — 종료 연월이 시작보다 앞서지 않는가</td></tr>
 *   <tr><td>FR-408</td><td>생성 대상 판정 — 그 달이 적용 기간 안인가</td></tr>
 *   <tr><td>FR-412</td><td>자동 반영 범위 — 그 달이 미래인가</td></tr>
 *   <tr><td>FR-413</td><td>재작성 대상 — 지정한 연·월</td></tr>
 * </table>
 *
 * <p>각자 계산하면 <b>한 곳만 틀려도 해를 넘기는 구간에서 조용히 어긋난다.</b> 연과 월을
 * 따로 비교하는 실수가 대표적이다 — "2026-11 ~ 2027-02" 기간에서 2027-01 은 시작 월(11)보다
 * 월이 작아 빠져 버린다. 예외도 오류 응답도 나지 않고 그 달의 고정지출만 사라진다.
 *
 * <h2>합성식은 여기서 만들지 않는다</h2>
 *
 * <p>{@link #value()} 는 {@link UserFixedExpense#yearMonthValue(int, int)} 에 위임한다.
 * DB CHECK {@code ck_fixed_expense_period} 와 {@code UserFixedExpenseRepository} 의 기간
 * 조회 JPQL 이 <b>같은 식</b>을 쓰고 있어, 여기서 곱셈을 다시 적으면 식이 둘이 된다.
 * 001 이 스키마에 못박은 규칙을 애플리케이션이 되풀이하는 것이 아니라 <b>참조</b>한다.
 *
 * <h2>{@code java.time.YearMonth} 를 쓰지 않는 이유</h2>
 *
 * <p>저장 형태가 {@code year}·{@code month} <b>INT 두 개</b>다(헌장 DB 저장 구조 규칙).
 * {@code YearMonth} 로 통일하면 매핑 계층마다 변환이 생기고 DB CHECK 과의 대응이 흐려진다.
 * 달력 계산이 필요한 곳({@link #paymentDateOn}·{@link #firstDay}·{@link #lastDay})에서만
 * 내부적으로 빌려 쓴다.
 *
 * @param year  연
 * @param month 월(1~12)
 * @see <a href="../../../../../../../../specs/005-backend-ledger-fixed-expense/research.md">research.md §4</a>
 */
public record YearMonthValue(int year, int month) implements Comparable<YearMonthValue> {

    /**
     * 받아들이는 연의 하한·상한.
     *
     * <p><b>DB 에는 연 CHECK 이 없다.</b> {@code ck_fixed_expense_month} 계열은 월만 1~12 로
     * 막고 연은 그대로 받는다(덤프 확인). 그래서 이 검사가 유일한 방어선이다 — 없으면
     * {@code year=20026} 같은 오타가 {@code 3403} 이 아니라 <b>빈 목록</b>으로 돌아와
     * 사용자는 "그 달에 아무것도 없다"로 읽는다.
     */
    public static final int MIN_YEAR = 1900;

    /** @see #MIN_YEAR */
    public static final int MAX_YEAR = 9999;

    /** 검증 없이 만든다. 이미 검증된 값(설정 행의 컬럼 등)에 쓴다. */
    public static YearMonthValue of(int year, int month) {
        return new YearMonthValue(year, month);
    }

    /**
     * 요청이 보낸 값을 검증해 만든다.
     *
     * <p><b>실패 코드를 호출자가 정한다.</b> 같은 "연·월 범위 오류"인데 4.5·4.6·4.9 는
     * {@code 3403}, 4.8 은 {@code 3501} 이다. 자원별 코드 블록 배정(고정지출 {@code 34xx} /
     * 가계부 목록 {@code 35xx})의 결과이며 <b>의도된 차이</b>다 — 실수처럼 보여 통일하고
     * 싶어지지만 통일하면 규칙이 깨진다(api-contract.md §5).
     *
     * @param errorCode 이 자원이 쓰는 연·월 오류 코드
     * @throws BusinessException 누락이거나 범위 밖
     */
    public static YearMonthValue require(Integer year, Integer month, ErrorCode errorCode) {
        if (year == null || month == null) {
            throw new BusinessException(errorCode, "year 와 month 는 필수입니다.");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException(errorCode, "month 는 1 에서 12 사이여야 합니다.");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BusinessException(errorCode,
                    "year 는 %d 에서 %d 사이여야 합니다.".formatted(MIN_YEAR, MAX_YEAR));
        }
        return new YearMonthValue(year, month);
    }

    /**
     * 서버 기준 현재 연월.
     *
     * <p>FR-412 의 "미래 달" 판정 기준점이다. <b>이번 달은 미래가 아니다</b> —
     * {@link #isAfter} 로 비교하므로 같은 달은 걸리지 않는다.
     */
    public static YearMonthValue now() {
        YearMonth current = YearMonth.now();
        return new YearMonthValue(current.getYear(), current.getMonthValue());
    }

    /** 합성값. 식은 {@link UserFixedExpense#yearMonthValue(int, int)} 하나뿐이다. */
    public int value() {
        return UserFixedExpense.yearMonthValue(year, month);
    }

    /** 이 달이 {@code other} 보다 뒤인가. 같은 달이면 거짓이다. */
    public boolean isAfter(YearMonthValue other) {
        return value() > other.value();
    }

    /** 이 달이 {@code other} 보다 앞인가. 같은 달이면 거짓이다. */
    public boolean isBefore(YearMonthValue other) {
        return value() < other.value();
    }

    /** 이 달이 {@code start}~{@code end} 안인가(FR-408). <b>양 끝을 포함한다</b>. */
    public boolean isWithin(YearMonthValue start, YearMonthValue end) {
        int target = value();
        return start.value() <= target && target <= end.value();
    }

    /**
     * 이 달의 결제일 — <b>말일 보정을 여기서 한다</b>(FR-409).
     *
     * <pre>{@code
     * (2026, 2).paymentDateOn(31)  ->  2026-02-28
     * (2028, 2).paymentDateOn(31)  ->  2028-02-29   (윤년)
     * (2026, 2).paymentDateOn(25)  ->  2026-02-25
     * }</pre>
     *
     * <p><b>결과를 저장한다.</b> 조회 때마다 다시 계산하면 4.5 와 4.8 이 각자 계산하다
     * 한쪽만 윤년을 빠뜨렸을 때 같은 달의 결제일이 두 화면에서 다르게 보인다.
     *
     * @param dayOfMonth 설정의 {@code payment_day_of_month}(1~31)
     * @throws IllegalArgumentException 범위 밖 — 설정 행은 CHECK {@code ck_fixed_expense_day}
     *                                  로 1~31 이 보장되므로 여기 걸리면 프로그래밍 오류다
     */
    public LocalDate paymentDateOn(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("결제일은 1~31 이어야 합니다: " + dayOfMonth);
        }
        YearMonth target = YearMonth.of(year, month);
        return LocalDate.of(year, month, Math.min(dayOfMonth, target.lengthOfMonth()));
    }

    /** 이 달 1일. 4.8 이 그 달 지출·소득을 결제일 범위로 긁을 때 쓴다. */
    public LocalDate firstDay() {
        return LocalDate.of(year, month, 1);
    }

    /** 이 달 말일. 윤년을 포함해 실제 일수를 따른다. */
    public LocalDate lastDay() {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    /** 그 날짜가 이 달에 속하는가. 4.6 의 "{@code paymentDate} 가 Path 와 다른 달" 판정이 쓴다. */
    public boolean contains(LocalDate date) {
        return date != null && date.getYear() == year && date.getMonthValue() == month;
    }

    @Override
    public int compareTo(YearMonthValue other) {
        return Integer.compare(value(), other.value());
    }

    /** 로그와 {@code YYYY-MM} 형식 응답에 쓴다. */
    @Override
    public String toString() {
        return "%04d-%02d".formatted(year, month);
    }
}

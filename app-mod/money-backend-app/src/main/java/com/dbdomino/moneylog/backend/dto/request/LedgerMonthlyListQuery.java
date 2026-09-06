package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.backend.dto.response.LedgerItemResponse;
import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 4.8 월별 가계부 목록의 조회 조건.
 *
 * <h2>연·월 오류가 {@code 3501} 이다 — 4.5·4.6·4.9 와 다르다</h2>
 *
 * <p>같은 "연·월 범위 오류"인데 고정지출 계열은 {@code 3403}, 가계부 목록은
 * {@code 3501}, 006 의 통계는 {@code 3603} 이다. 자원별 코드 블록 배정
 * (고정지출 {@code 34xx} / 가계부 {@code 35xx})의 결과이며 <b>의도된 차이</b>다.
 * 실수처럼 보여 통일하고 싶어지지만 통일하면 규칙이 깨진다(api-contract.md §5).
 *
 * <h2>{@code type} 은 콤마로 복수 지정한다</h2>
 *
 * <p>{@code type=EXPENSE,INSTALLMENT} 가 유효하다. 생략하면 전부다. 없는 값이 섞이면
 * <b>{@code 9001} 로 거절한다</b> — 조용히 무시하면 오타({@code type=EXPENES})가 "전부"로
 * 읽혀 사용자가 필터를 걸었다고 믿는 채 전체를 보게 된다.
 *
 * @param yearMonth       조회 연·월
 * @param types           볼 행 종류. <b>비어 있으면 전부</b>다
 * @param paymentMethodId 수단 필터
 * @param expendGroupId   지출유형 필터. 걸리면 <b>{@code INCOME} 행이 전부 빠진다</b> —
 *                        소득에는 지출유형이 없기 때문이다(ledger-list.md §5)
 * @param dateFrom        결제일 하한(포함). 그 달 <b>안에서</b> 더 좁히는 용도이며 달을
 *                        넘나들지 않는다
 * @param dateTo          결제일 상한(포함)
 * @param keyword         장소·내용 부분 일치. <b>대소문자를 무시한다</b> — 손으로 적은
 *                        자유 문자열이라 구분하면 자기가 적은 것을 자기가 못 찾는다
 * @param sort            {@code paymentDate} 또는 {@code amount}. 기본 {@code paymentDate}
 * @param ascending       {@code order=asc} 인가. 기본은 <b>{@code desc}</b>(최근 것이 위)
 */
public record LedgerMonthlyListQuery(YearMonthValue yearMonth,
                                     Set<String> types,
                                     Long paymentMethodId,
                                     Long expendGroupId,
                                     LocalDate dateFrom,
                                     LocalDate dateTo,
                                     String keyword,
                                     String sort,
                                     boolean ascending) {

    /** 정렬 기준 — 결제일. 생략 시 기본값이다. */
    public static final String SORT_PAYMENT_DATE = "paymentDate";

    /** 정렬 기준 — 금액. */
    public static final String SORT_AMOUNT = "amount";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            LedgerItemResponse.TYPE_EXPENSE, LedgerItemResponse.TYPE_INSTALLMENT,
            LedgerItemResponse.TYPE_INCOME, LedgerItemResponse.TYPE_FIXED);

    /**
     * 값을 검증해 조회 조건을 만든다.
     *
     * <p><b>연·월 오류만 {@code 3501} 이고 나머지 형식 오류는 {@code 9001} 이다.</b>
     * {@code 3501} 은 "그 달을 특정할 수 없다"는 뜻이라 자원 코드가 붙지만,
     * {@code sort=nope} 같은 것은 어느 자원에나 있는 평범한 요청 형식 오류다.
     */
    public static LedgerMonthlyListQuery of(Integer year, Integer month, String type,
                                            Long paymentMethodId, Long expendGroupId,
                                            String dateFrom, String dateTo, String keyword,
                                            String sort, String order) {
        YearMonthValue yearMonth =
                YearMonthValue.require(year, month, ErrorCode.LEDGER_MONTH_INVALID);
        return new LedgerMonthlyListQuery(
                yearMonth,
                parseTypes(type),
                paymentMethodId,
                expendGroupId,
                parseDate(dateFrom, "dateFrom"),
                parseDate(dateTo, "dateTo"),
                blankToNull(keyword),
                parseSort(sort),
                parseAscending(order));
    }

    /**
     * 콤마로 나눠 행 종류를 읽는다. 빈 값이면 빈 집합(= 전부)이다.
     *
     * <p>공백을 다듬고 대문자로 맞춘다 — {@code "EXPENSE, income"} 도 받는다. 다만
     * <b>모르는 값은 거절한다</b>.
     */
    private static Set<String> parseTypes(String type) {
        if (type == null || type.isBlank()) {
            return Set.of();
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String token : Arrays.stream(type.split(",")).map(String::trim).toList()) {
            if (token.isEmpty()) {
                continue;
            }
            String upper = token.toUpperCase(Locale.ROOT);
            if (!ALLOWED_TYPES.contains(upper)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "type 은 EXPENSE·INCOME·FIXED·INSTALLMENT 중에서 고릅니다: " + token);
            }
            parsed.add(upper);
        }
        return parsed;
    }

    private static LocalDate parseDate(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    name + " 은(는) YYYY-MM-DD 형식이어야 합니다.");
        }
    }

    private static String parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_PAYMENT_DATE;
        }
        String trimmed = sort.trim();
        if (SORT_PAYMENT_DATE.equals(trimmed) || SORT_AMOUNT.equals(trimmed)) {
            return trimmed;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST,
                "sort 는 paymentDate 또는 amount 여야 합니다.");
    }

    /** 기본은 {@code desc} 다 — 명시하지 않으면 최근 것이 위다. */
    private static boolean parseAscending(String order) {
        if (order == null || order.isBlank()) {
            return false;
        }
        String lower = order.trim().toLowerCase(Locale.ROOT);
        if ("asc".equals(lower)) {
            return true;
        }
        if ("desc".equals(lower)) {
            return false;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "order 는 asc 또는 desc 여야 합니다.");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 그 종류를 보여줘야 하는가. {@code types} 가 비어 있으면 전부 통과다. */
    public boolean includesType(String type) {
        return types.isEmpty() || types.contains(type);
    }
}

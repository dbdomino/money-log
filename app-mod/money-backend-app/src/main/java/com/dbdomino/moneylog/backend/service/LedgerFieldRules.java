package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 지출·소득이 공유하는 값 규칙 — 금액·날짜·길이.
 *
 * <p>FR-305 가 <b>"이 검증은 단건 등록·수정과 엑셀 업로드에 동일하게 적용한다"</b>고
 * 못박았다. 세 경로가 각자 검사하면 한 곳만 느슨해져도 그 경로로만 잘못된 값이 들어온다.
 *
 * <h2>실패 코드는 호출자가 준다</h2>
 *
 * <p>규칙은 같지만 코드가 다르다 — <b>지출은 {@code 3201}, 소득은 {@code 3301}</b>
 * (api-contract.md §7). 그래서 각 메서드가 {@link ErrorCode} 를 받는다. 한쪽 코드를 여기
 * 박아 두면 나머지 API 가 남의 대역 코드를 내보내게 된다.
 *
 * <h2>왜 DB 에 맡기지 않는가</h2>
 *
 * <p>길이 초과는 DB 도 거절하지만 그 실패는 {@code 9000}(서버 오류) + HTTP 500 으로
 * 나간다 — <b>사용자 입력 문제인데 서버 장애처럼 보인다</b>. 금액도 마찬가지로
 * {@code ck_expense_amount}(> 0) 가 있지만 그 위반을 업무 코드로 바꿀 방법이 없다.
 * 애플리케이션이 먼저 잡아야 응답 코드를 통제한다(data-model.md §1).
 */
final class LedgerFieldRules {

    /** {@code tbl_expense.place varchar(100)}. */
    static final int PLACE_MAX = 100;

    /** {@code tbl_expense.content varchar(255)} · {@code tbl_income.content varchar(255)}. */
    static final int CONTENT_MAX = 255;

    private LedgerFieldRules() {
    }

    /**
     * 금액. <b>0보다 커야 한다.</b>
     *
     * <p>{@code BIGINT} 범위 초과와 소수점·문자는 요청을 읽는 단계에서 이미 걸린다 —
     * 여기 닿는 값은 정수뿐이므로 부호만 본다.
     */
    static long requireAmount(Long amount, ErrorCode errorCode) {
        if (amount == null || amount <= 0L) {
            throw new BusinessException(errorCode, "금액은 0보다 큰 정수여야 합니다.");
        }
        return amount;
    }

    /**
     * 결제일·입금일. 형식은 {@code YYYY-MM-DD} 다.
     *
     * <p><b>{@code LocalDate.parse} 의 기본 동작을 그대로 쓴다</b> — {@code 2026-13-01} 이나
     * {@code 2026-02-30} 같이 달력에 없는 날짜도 여기서 걸린다. 형식만 맞으면 통과시키면
     * DB 가 거절하고 그 실패가 {@code 9000} 으로 새어 나간다.
     */
    static LocalDate requireDate(String text, ErrorCode errorCode) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(errorCode, "날짜는 YYYY-MM-DD 형식이어야 합니다.");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(errorCode, "날짜는 YYYY-MM-DD 형식이어야 합니다.");
        }
    }

    /**
     * 장소. 지출에만 있고 <b>필수</b>다(FR-307). 100자를 넘을 수 없다.
     *
     * <p>길이를 {@code String.length()} 로 센다 — 컬럼이 {@code varchar(100)} 이고
     * PostgreSQL 의 {@code varchar(n)} 은 바이트가 아니라 <b>문자 수</b>를 센다. 한글을
     * 바이트로 세면 실제로 들어가는 값보다 훨씬 일찍 거절하게 된다.
     */
    static String requirePlace(String place, ErrorCode errorCode) {
        return requireText(place, PLACE_MAX, "장소", errorCode);
    }

    /** 지출의 내용. <b>필수</b>이며 255자를 넘을 수 없다. 소득의 내용은 규칙이 다르다. */
    static String requireContent(String content, ErrorCode errorCode) {
        return requireText(content, CONTENT_MAX, "내용", errorCode);
    }

    /**
     * 소득의 내용. <b>비어 있을 수 있다</b>(FR-307) — 지출과 다른 유일한 값 규칙이다.
     *
     * <p>빈 문자열은 {@code null} 로 바꾼다. 컬럼이 NULL 을 허용하므로 "안 적었다"를
     * 빈 문자열과 {@code null} 두 가지로 저장하면 조회하는 쪽이 둘 다 다뤄야 한다.
     */
    static String normalizeOptionalContent(String content, ErrorCode errorCode) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return requireText(content, CONTENT_MAX, "내용", errorCode);
    }

    private static String requireText(String value, int max, String label, ErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorCode, label + "은(는) 비울 수 없습니다.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(errorCode, label + "은(는) " + max + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }
}

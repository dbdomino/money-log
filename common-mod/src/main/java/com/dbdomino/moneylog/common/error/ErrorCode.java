package com.dbdomino.moneylog.common.error;

/**
 * API 실패 코드. 성공은 {@code 200}, 실패는 <b>정수 4자리</b>다(헌장 원칙 III).
 *
 * <p>대역은 자원별로 나뉜다.
 *
 * <ul>
 *   <li>{@code 10xx} — 인증·권한
 *   <li>{@code 20xx} — 회원
 *   <li>{@code 30xx} — 결제수단, {@code 31xx} 지출유형, {@code 32xx} 지출·할부,
 *       {@code 33xx} 소득, {@code 34xx} 고정지출, {@code 35xx} 가계부·엑셀,
 *       {@code 36xx} 목표금액·통계
 *   <li>{@code 90xx} — 공통·서버
 * </ul>
 *
 * <p><b>002 가 쓰는 것만 담지 않는다.</b> 003~006 의 코드도 각 스펙의 에러코드 표에서
 * 함께 옮겨 왔다. 후속 기능이 이 Enum 을 매번 손대게 만들 이유가 없고, 코드가 한곳에
 * 모여 있어야 중복 배정을 눈으로 잡을 수 있다.
 *
 * <p><b>결번</b>: {@code 3404}는 폐기된 구 {@code FixedExpenseMonthlyOverrideUpsert}의
 * 코드다. 되살려 쓰지 않는다.
 *
 * <p>HTTP 상태는 여기서 정하지 않는다. {@link GlobalExceptionHandler}가 비즈니스·검증
 * 실패를 <b>HTTP 200</b>으로, {@link #INTERNAL_SERVER_ERROR}만 500 으로 내보낸다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/spec.md">002 spec.md § 이 기능이 쓰는 에러코드</a>
 */
public enum ErrorCode {

    /** 성공. 실패 코드와 같은 자리에서 다루기 위해 넣어 둔다. */
    OK(200, "성공"),

    // ── 10xx 인증·권한 ────────────────────────────────────────────────
    /** Access Token 없음·형식 오류·JWT 만료·{@code access_expires_at} 지남. */
    UNAUTHORIZED(1001, "로그인이 필요합니다."),
    /** 관리자 권한 없음({@code role != 1}). */
    FORBIDDEN_ADMIN_ONLY(1002, "관리자만 사용할 수 있습니다."),
    /** 아이디·비밀번호 불일치. 어느 쪽이 틀렸는지 구분해 알리지 않는다. */
    LOGIN_FAILED(1003, "아이디 또는 비밀번호가 올바르지 않습니다."),
    /** 비활성 계정({@code active=false}). */
    ACCOUNT_INACTIVE(1004, "비활성화된 계정입니다."),
    /** Refresh Token 없음·만료·해시 불일치. 로그아웃 후 갱신도 여기다. */
    REFRESH_TOKEN_INVALID(1005, "다시 로그인해 주세요."),
    /** 세션 무효 — 로그아웃·중복 로그인·해시 불일치. 토큰은 멀쩡한데 세션이 갈린 경우다. */
    SESSION_INVALID(1006, "세션이 만료되었습니다. 다시 로그인해 주세요."),

    // ── 20xx 회원 ────────────────────────────────────────────────────
    /** 회원 없음. 아이디·닉네임·이메일 불일치를 포함한다. */
    MEMBER_NOT_FOUND(2001, "회원 정보를 찾을 수 없습니다."),
    /** 아이디 중복. */
    MEMBER_ID_DUPLICATED(2002, "이미 사용 중인 아이디입니다."),
    /** 이메일 중복. 이메일은 값이 있을 때만 유일하다. */
    EMAIL_DUPLICATED(2003, "이미 사용 중인 이메일입니다."),
    /** 비밀번호 규칙 위반 — 8자 이상, 영문 대소문자·숫자·특수문자 중 3종류 이상. */
    PASSWORD_RULE_VIOLATION(2004, "비밀번호는 8자 이상이며 영문 대문자·소문자·숫자·특수문자 중 3종류 이상을 포함해야 합니다."),
    /** 비밀번호 확인 불일치. */
    PASSWORD_CONFIRM_MISMATCH(2005, "비밀번호가 서로 일치하지 않습니다."),

    // ── 30xx 결제수단 (003) ──────────────────────────────────────────
    PAYMENT_METHOD_TYPE_INVALID(3001, "결제수단의 종류 또는 용도가 올바르지 않습니다."),
    PAYMENT_METHOD_EXPIRY_INVALID(3002, "카드 유효기간은 YYYY-MM 형식이어야 합니다."),
    PAYMENT_METHOD_NOT_FOUND(3003, "결제수단을 찾을 수 없습니다."),
    PAYMENT_METHOD_ALREADY_DELETED(3004, "이미 삭제된 결제수단입니다."),
    PAYMENT_METHOD_PURPOSE_LOCKED(3005, "사용 내역이 있어 용도를 바꿀 수 없습니다."),

    // ── 31xx 지출유형 (003) ──────────────────────────────────────────
    EXPEND_GROUP_NAME_DUPLICATED(3101, "이미 사용 중인 지출유형 이름입니다."),
    EXPEND_GROUP_ICON_INVALID(3102, "아이콘 파일의 형식 또는 크기가 올바르지 않습니다."),
    EXPEND_GROUP_NOT_FOUND(3103, "지출유형을 찾을 수 없습니다."),
    EXPEND_GROUP_ICON_NOT_FOUND(3104, "아이콘 파일을 찾을 수 없습니다."),
    EXPEND_GROUP_DEFAULT_NAME_LOCKED(3105, "기본 지출유형의 이름은 바꿀 수 없습니다."),
    EXPEND_GROUP_IN_USE(3106, "이 유형을 사용한 지출이 있어 삭제할 수 없습니다."),
    EXPEND_GROUP_DEFAULT_UNDELETABLE(3107, "기본 지출유형은 삭제할 수 없습니다."),
    EXPEND_GROUP_ALREADY_DELETED(3108, "이미 삭제된 지출유형입니다."),

    // ── 32xx 지출·할부 (004) ─────────────────────────────────────────
    EXPENSE_FIELD_INVALID(3201, "지출의 금액 또는 날짜 형식이 올바르지 않습니다."),
    EXPENSE_NOT_FOUND(3202, "지출 내역을 찾을 수 없습니다."),
    EXPENSE_INSTALLMENT_IMMUTABLE(3203, "할부 개월 수와 시작 연월은 바꿀 수 없습니다."),
    INSTALLMENT_VALIDATION_FAILED(3204, "할부 개월 수 또는 금액이 올바르지 않습니다."),
    INSTALLMENT_CREATE_FAILED(3205, "할부 생성에 실패했습니다."),
    INSTALLMENT_GROUP_NOT_FOUND(3206, "할부 그룹을 찾을 수 없습니다."),
    INSTALLMENT_NOTHING_TO_SETTLE(3207, "중도상환할 남은 할부 건이 없습니다."),

    // ── 33xx 소득 (004) ──────────────────────────────────────────────
    INCOME_FIELD_INVALID(3301, "소득의 금액 또는 날짜 형식이 올바르지 않습니다."),
    INCOME_NOT_FOUND(3302, "소득 내역을 찾을 수 없습니다."),

    // ── 34xx 고정지출 (005) ──────────────────────────────────────────
    FIXED_EXPENSE_FIELD_INVALID(3401, "고정지출의 기간·결제일·금액·수단이 올바르지 않습니다."),
    FIXED_EXPENSE_NOT_FOUND(3402, "고정지출을 찾을 수 없습니다."),
    FIXED_EXPENSE_MONTH_INVALID(3403, "연·월 값이 올바르지 않습니다."),
    // 3404 는 결번이다(폐기된 구 FixedExpenseMonthlyOverrideUpsert). 되살려 쓰지 않는다.
    FIXED_EXPENSE_MONTHLY_NOT_CREATED(3405, "해당 연·월의 고정지출 내역이 아직 만들어지지 않았습니다."),

    // ── 35xx 가계부·엑셀 (004·005) ───────────────────────────────────
    LEDGER_MONTH_INVALID(3501, "가계부 조회의 연·월 값이 올바르지 않습니다."),
    EXCEL_ROW_VALIDATION_FAILED(3502, "업로드 파일에 잘못된 행이 있습니다."),
    EXCEL_FORMAT_NOT_XLSX(3503, "xlsx 파일만 업로드할 수 있습니다."),
    EXCEL_ROW_LIMIT_EXCEEDED(3504, "업로드 파일의 행 수가 300행을 넘습니다."),
    EXCEL_FILE_EMPTY(3505, "업로드 파일이 비어 있습니다."),

    // ── 36xx 목표금액·통계 (006) ─────────────────────────────────────
    TARGET_GROUP_NOT_IN_USE(3601, "사용하지 않는 지출유형입니다."),
    TARGET_AMOUNT_OUT_OF_RANGE(3602, "목표금액은 0원 이상 1억원 이하여야 합니다."),
    STATISTICS_PARAM_INVALID(3603, "연·월 또는 조회 구분 값이 올바르지 않습니다."),
    STATISTICS_FUTURE_MONTH(3604, "미래 월의 통계는 저장할 수 없습니다."),

    // ── 90xx 공통·서버 ───────────────────────────────────────────────
    /** 내부 서버 오류. <b>이 코드만 HTTP 500</b>이다. */
    INTERNAL_SERVER_ERROR(9000, "서버 오류가 발생했습니다."),
    /** 필수 필드 누락·형식 오류·페이징 값 오류 등 잘못된 요청. */
    BAD_REQUEST(9001, "요청 값이 올바르지 않습니다.");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /** 응답 {@code resCode}에 실리는 값. */
    public int code() {
        return code;
    }

    /** 응답 {@code data.message}의 기본값. 호출부가 더 구체적인 문구로 덮을 수 있다. */
    public String message() {
        return message;
    }
}

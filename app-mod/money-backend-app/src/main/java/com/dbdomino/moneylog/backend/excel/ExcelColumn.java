package com.dbdomino.moneylog.backend.excel;

/**
 * 엑셀 양식의 열 정의 — <b>양식 생성(3.11)과 업로드 파싱(3.12)이 공유한다</b>.
 *
 * <p>FR-318 이 "양식 다운로드와 업로드의 컬럼 정의가 일치해야 한다"고 요구한다. 두 곳에
 * 각자 적으면 반드시 갈린다 — 열을 하나 추가하면서 한쪽만 고치는 일이 생긴다. 하나를
 * 공유하면 <b>일치가 구조로 보장된다</b>: 순서·헤더 문자열·필수 여부가 한 곳에서 나온다.
 *
 * <h2>필수 여부가 A열 값에 따라 달라진다</h2>
 *
 * <table border="1">
 *   <caption>excel-contract.md §1</caption>
 *   <tr><th>열</th><th>{@code EXPENSE} 행</th><th>{@code INCOME} 행</th></tr>
 *   <tr><td>E 지출유형</td><td>필수</td><td><b>비어 있어야 한다</b></td></tr>
 *   <tr><td>F 장소</td><td>필수</td><td><b>비어 있어야 한다</b></td></tr>
 *   <tr><td>G 내용</td><td>필수</td><td>선택</td></tr>
 * </table>
 *
 * <p>소득 행에 E·F 가 채워져 있으면 오류다 — {@code tbl_income} 에 그 컬럼이 <b>없어</b>
 * 저장할 자리가 없는데 사용자는 저장됐다고 믿게 된다(FR-306).
 *
 * <p><b>할부 열이 없다.</b> 업로드는 일시불 지출과 소득만 만들며, 그것이 만드는 지출 행의
 * 할부 3컬럼은 전부 NULL 이다(api-contract.md §8).
 */
public enum ExcelColumn {

    /** A — 이 행을 지출로 넣을지 소득으로 넣을지 정한다. 나머지 열의 필수 여부도 이 값이 가른다. */
    KIND(0, "구분"),

    /** B — 결제일·입금일 {@code YYYY-MM-DD}. */
    PAYMENT_DATE(1, "결제일"),

    /** C — 금액(원 단위 정수, 0 초과). */
    AMOUNT(2, "금액"),

    /** D — 수단 이름. 양식에서는 본인 사용 중 수단이 드롭다운으로 들어간다. */
    PAYMENT_METHOD(3, "수단"),

    /** E — 지출유형 이름. <b>지출 행에만</b> 쓰며 드롭다운으로 들어간다. */
    EXPEND_GROUP(4, "지출유형"),

    /** F — 장소. <b>지출 행에만</b> 쓴다. */
    PLACE(5, "장소"),

    /** G — 내용. 지출은 필수, 소득은 선택이다. */
    CONTENT(6, "내용");

    /** A열 값 — 지출. */
    public static final String KIND_EXPENSE = "EXPENSE";

    /** A열 값 — 소득. */
    public static final String KIND_INCOME = "INCOME";

    /**
     * 시트 이름. 업로드는 <b>첫 시트</b>를 읽으므로 파싱이 이 이름에 의존하지는 않는다 —
     * 사용자가 시트 이름을 바꿔도 읽힌다.
     */
    public static final String SHEET_NAME = "지출소득";

    /**
     * 헤더가 차지하는 행 수. <b>한 줄뿐이다</b>(excel-contract.md § 정한 것).
     *
     * <p>안내 행을 두지 않은 이유가 여기 있다 — 데이터 행 수 판정(FR-319 의 300행·0행)이
     * "몇 행부터가 데이터인가"에 매달리는데, 헤더만 두면 그 규칙이 <b>2행부터</b> 하나로 끝난다.
     */
    public static final int HEADER_ROWS = 1;

    private final int index;
    private final String header;

    ExcelColumn(int index, String header) {
        this.index = index;
        this.header = header;
    }

    /** 0부터 시작하는 열 번호. POI 의 셀 인덱스와 같다. */
    public int index() {
        return index;
    }

    /** 헤더 행에 적히는 문자열. */
    public String header() {
        return header;
    }

    /** 사람이 읽는 열 문자({@code A}~{@code G}). {@code errors[].column} 에 싣는다. */
    public String letter() {
        return String.valueOf((char) ('A' + index));
    }

    /** 이 열이 {@code kind} 행에서 값을 가져야 하는가. */
    public boolean isRequiredFor(String kind) {
        return switch (this) {
            case KIND, PAYMENT_DATE, AMOUNT, PAYMENT_METHOD -> true;
            case EXPEND_GROUP, PLACE, CONTENT -> KIND_EXPENSE.equals(kind);
        };
    }

    /**
     * 이 열이 {@code kind} 행에서 <b>비어 있어야</b> 하는가.
     *
     * <p>{@link #isRequiredFor} 의 반대가 아니다 — 소득 행의 {@code CONTENT} 는 필수도
     * 아니고 금지도 아닌 <b>선택</b>이다(FR-307).
     */
    public boolean mustBeBlankFor(String kind) {
        return KIND_INCOME.equals(kind) && (this == EXPEND_GROUP || this == PLACE);
    }
}

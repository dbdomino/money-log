package com.dbdomino.moneylog.backend.excel;

import java.util.Map;

/**
 * 업로드 파일에서 읽어 낸 <b>한 행</b>. POI 타입이 아니라 평범한 문자열 묶음이다.
 *
 * <p>{@code Workbook}·{@code Row}·{@code Cell} 을 Service 로 넘기지 않으려고 둔 경계다
 * (헌장 원칙 II). Service 는 이 타입만 보므로 엑셀 라이브러리를 바꿔도 손댈 곳이
 * {@code excel} 패키지 안에 머문다.
 *
 * <p><b>값을 해석하지 않는다.</b> 금액도 날짜도 문자열 그대로 들고 있으며, 숫자로 바꾸거나
 * 형식을 보는 일은 검증 단계가 한다 — 읽기와 검증을 나눠야 파일 단위 판정
 * ({@code 3503}·{@code 3504}·{@code 3505})을 행 검증보다 <b>먼저</b> 끝낼 수 있다.
 *
 * @param rowNumber 사람이 보는 <b>1-based</b> 행 번호. 헤더가 1행이므로 데이터는 2부터다 —
 *                  {@code errors[].row} 에 그대로 실려 사용자가 엑셀에서 찾아갈 수 있어야 한다
 * @param values    열별 값. 빈 셀은 <b>빈 문자열</b>이며 {@code null} 이 아니다
 */
public record ExcelRow(int rowNumber, Map<ExcelColumn, String> values) {

    /** 그 열의 값. 빈 셀은 빈 문자열이다. */
    public String get(ExcelColumn column) {
        return values.getOrDefault(column, "");
    }

    /** 그 열이 비어 있는가. */
    public boolean isBlank(ExcelColumn column) {
        return get(column).isBlank();
    }

    /** A열이 지시하는 구분. 대소문자를 가리지 않고 앞뒤 공백을 턴다. */
    public String kind() {
        return get(ExcelColumn.KIND).trim().toUpperCase(java.util.Locale.ROOT);
    }

    /** 이 행이 통째로 비어 있는가 — 사용자가 아래쪽 빈 줄을 남겨 두는 경우가 흔하다. */
    public boolean isEmptyRow() {
        return values.values().stream().allMatch(String::isBlank);
    }
}

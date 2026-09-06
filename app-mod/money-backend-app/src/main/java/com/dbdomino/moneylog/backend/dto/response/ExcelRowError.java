package com.dbdomino.moneylog.backend.dto.response;

/**
 * 엑셀 업로드에서 걸린 <b>한 칸</b>의 오류. {@code 3502} 응답의 {@code errors[]} 항목이다.
 *
 * <p>세 값이 함께 있어야 사용자가 <b>파일에서 그 자리를 찾아갈 수 있다</b>. 메시지만
 * 주면 "몇 번째를 고치라는 건지" 알 수 없고, 위치만 주면 무엇이 잘못됐는지 알 수 없다.
 *
 * @param row     엑셀 행 번호. <b>1-based</b> 이며 헤더가 1행이므로 데이터는 2부터다 —
 *                사용자가 엑셀에서 보는 번호와 같아야 한다
 * @param column  열 문자({@code A}~{@code G}) 또는 열 이름. 사용자가 화면에서 짚을 수
 *                있는 표기를 쓴다
 * @param message 무엇이 잘못됐는지. 사용자가 고칠 수 있는 말로 적는다
 */
public record ExcelRowError(int row, String column, String message) {
}

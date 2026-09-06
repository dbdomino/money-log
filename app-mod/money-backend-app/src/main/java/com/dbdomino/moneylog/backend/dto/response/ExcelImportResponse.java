package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 3.12 엑셀 업로드의 응답. <b>성공과 행별 오류가 같은 타입을 쓴다.</b>
 *
 * <p>3.12 는 응답 규격의 예외가 <b>아니다</b> — 파일을 돌려주지 않고 성공·실패 모두
 * {@code { resCode, data }} 를 쓴다(FR-322). 양식 다운로드(3.11)만 예외다.
 *
 * <table border="1">
 *   <caption>어느 필드가 차는가</caption>
 *   <tr><th>상황</th><th>resCode</th><th>채워지는 것</th></tr>
 *   <tr><td>성공</td><td>200</td><td>건수 3개 + {@code message}. {@code errors} 는 비어 있다</td></tr>
 *   <tr><td>행별 검증 오류</td><td>3502</td><td>{@code message} + {@code errors}. 건수는 0이다</td></tr>
 * </table>
 *
 * <p>파일 단위 거절({@code 3503} xlsx 아님 · {@code 3504} 300행 초과 · {@code 3505} 빈 파일)은
 * 이 타입을 쓰지 않는다 — {@code { message }} 한 칸이면 충분하고, 프론트가 "파일을 다시
 * 고르세요"와 "표의 이 행을 고치세요"를 <b>다르게 안내해야</b> 해서 코드를 나눠 두었다.
 *
 * <p>{@code errors} 를 {@code null} 이 아니라 <b>빈 목록</b>으로 둔다. 성공 응답에서도
 * 필드가 사라지지 않아 프론트가 길이만 보면 된다.
 *
 * @param importedCount 등록된 전체 건수(지출 + 소득)
 * @param expenseCount  등록된 지출 건수
 * @param incomeCount   등록된 소득 건수
 * @param message       화면에 보여 줄 요약 문구
 * @param errors        행별 오류. 성공이면 빈 목록이다
 */
public record ExcelImportResponse(int importedCount, int expenseCount, int incomeCount,
                                  String message, List<ExcelRowError> errors) {

    /** 성공 응답. 오류 목록은 비어 있다. */
    public static ExcelImportResponse succeeded(int expenseCount, int incomeCount) {
        int total = expenseCount + incomeCount;
        return new ExcelImportResponse(total, expenseCount, incomeCount,
                total + "건이 등록되었습니다", List.of());
    }

    /**
     * 행별 검증 실패. <b>건수가 전부 0이다</b> — 전체 롤백이라 한 건도 저장되지 않았다(FR-320).
     *
     * <p>"몇 건은 들어갔다"로 읽힐 여지를 남기지 않으려고 0을 명시한다.
     */
    public static ExcelImportResponse failed(List<ExcelRowError> errors) {
        return new ExcelImportResponse(0, 0, 0,
                errors.size() + "개 항목이 잘못되어 한 건도 등록하지 않았습니다", List.copyOf(errors));
    }
}

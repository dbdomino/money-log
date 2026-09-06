package com.dbdomino.moneylog.backend.excel;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 업로드된 {@code .xlsx} 를 읽어 <b>평범한 행 목록</b>으로 바꾼다.
 *
 * <p>여기서 <b>업무 검증을 하지 않는다.</b> 읽기와 검증을 나눠야 파일 단위 판정
 * ({@code 3503} 형식 · {@code 3504} 300행 초과 · {@code 3505} 빈 파일)을 행 검증보다
 * <b>먼저</b> 끝낼 수 있다(FR-319). 프론트가 "파일을 다시 고르세요"와 "표의 N행을
 * 고치세요"를 다르게 안내해야 해서 코드를 나눠 두었다.
 *
 * <p>POI 타입을 밖으로 내보내지 않는다(헌장 원칙 II) — 결과는 {@link ExcelRow} 목록이다.
 *
 * <h2>셀 값을 문자열로 통일한다</h2>
 *
 * <p>같은 "2026-03-15" 라도 사용자가 날짜 서식으로 넣으면 POI 는 숫자 셀로 준다. 금액도
 * 숫자 셀이고, 그것을 그대로 {@code toString} 하면 {@code 12000.0} 이 된다. 그래서 셀
 * 종류별로 <b>사람이 입력한 대로</b> 되돌린 문자열을 만든다 — 검증 단계가 문자열 하나만
 * 보게 하려는 것이다.
 */
@Component
public class ExcelRowReader {

    /** 날짜 셀을 되돌릴 형식. 양식이 요구하는 {@code YYYY-MM-DD} 와 같다. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** {@code .xlsx} 가 아니면 던진다. 호출자가 {@code 3503} 으로 바꾼다. */
    public static class NotXlsxException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        NotXlsxException(Throwable cause) {
            super("xlsx 파일이 아닙니다.", cause);
        }
    }

    /**
     * 첫 시트의 데이터 행을 읽는다. 헤더(1행)는 건너뛴다.
     *
     * <p><b>통째로 빈 행은 버린다.</b> 사용자가 표 아래에 빈 줄을 남겨 두는 일이 흔한데,
     * 그것을 데이터로 세면 정상 파일이 {@code 3502} 로 거절된다. 300행 판정도 이 결과를
     * 기준으로 하므로 빈 줄이 상한을 잡아먹지 않는다.
     *
     * <p><b>시트 이름을 보지 않는다.</b> 첫 시트를 읽으므로 사용자가 이름을 바꿔도 읽힌다 —
     * 양식이 정한 이름은 만들 때 쓰는 것이지 파싱의 조건이 아니다.
     *
     * @throws NotXlsxException {@code .xlsx} 로 열 수 없는 파일
     */
    public List<ExcelRow> read(InputStream input) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }
            Sheet sheet = workbook.getSheetAt(0);
            List<ExcelRow> rows = new ArrayList<>();
            for (int rowIndex = ExcelColumn.HEADER_ROWS; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                ExcelRow read = toExcelRow(row, rowIndex + 1);
                if (!read.isEmptyRow()) {
                    rows.add(read);
                }
            }
            return rows;
        } catch (IOException | RuntimeException e) {
            if (e instanceof NotXlsxException notXlsx) {
                throw notXlsx;
            }
            // POI 는 형식이 아니면 여러 종류의 예외를 던진다. 호출자에게는 "xlsx 가 아니다"
            // 하나로만 보이면 된다 — 어느 예외였는지는 사용자가 할 조치를 바꾸지 않는다.
            throw new NotXlsxException(e);
        }
    }

    private static ExcelRow toExcelRow(Row row, int rowNumber) {
        Map<ExcelColumn, String> values = new EnumMap<>(ExcelColumn.class);
        for (ExcelColumn column : ExcelColumn.values()) {
            values.put(column, cellText(row.getCell(column.index())));
        }
        return new ExcelRow(rowNumber, values);
    }

    /**
     * 셀을 <b>사람이 입력한 대로</b>의 문자열로 되돌린다.
     *
     * <p>숫자 셀에서 {@code 12000.0} 이 나오지 않게 소수부가 없으면 정수로 적는다 —
     * 그대로 두면 금액 검증이 "정수가 아니다"로 거절한다.
     */
    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case NUMERIC -> numericText(cell);
            case FORMULA -> formulaText(cell);
            default -> "";
        };
    }

    /** 숫자 셀. 날짜 서식이면 {@code YYYY-MM-DD} 로, 아니면 불필요한 소수부를 턴다. */
    private static String numericText(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMAT);
        }
        return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
    }

    /** 수식 셀은 계산된 값을 쓴다. 사용자가 다른 시트에서 값을 끌어오는 경우가 있다. */
    private static String formulaText(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> numericText(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}

package com.dbdomino.moneylog.backend.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 3.11 양식({@code .xlsx})을 POI 로 만든다.
 *
 * <p><b>POI 타입을 이 패키지 밖으로 내보내지 않는다</b>(헌장 원칙 II). Service 는
 * 이름 목록을 넘기고 {@code byte[]} 를 받는다 — {@code Workbook}·{@code Row}·{@code Cell}
 * 이 Service 로 새면 Service 가 라이브러리에 묶이고, 형식이 바뀔 때 손댈 곳이 흩어진다.
 *
 * <h2>양식은 회원마다 내용이 다르다</h2>
 *
 * <p>FR-317 이 본인의 <b>사용 중</b> 수단·지출유형을 데이터 유효성 목록(드롭다운)으로
 * 넣으라고 요구한다. 그래서 정적 파일로 미리 만들어 둘 수 없고 요청마다 생성한다 —
 * plan.md 가 "양식을 리소스의 고정 파일로 두는" 대안을 기각한 이유가 이것이다.
 *
 * <h2>헤더는 한 줄이다</h2>
 *
 * <p>안내 행을 넣지 않는다. 데이터 행 수 판정(FR-319)이 "몇 행부터가 데이터인가"에
 * 매달리는데, 헤더만 두면 그 규칙이 <b>2행부터</b> 하나로 끝난다
 * (excel-contract.md § 정한 것).
 */
@Component
public class ExcelTemplateWriter {

    /**
     * 드롭다운을 적용할 데이터 행의 끝.
     *
     * <p>업로드 상한이 300행(FR-319)이라 그만큼만 걸면 되지만, 사용자가 그 아래에 붙여넣기
     * 하는 것을 막지 않으려고 넉넉히 잡는다 — 유효성 목록은 <b>입력을 돕는 장치</b>이지
     * 검증이 아니다. 실제 검증은 업로드가 다시 한다.
     */
    private static final int VALIDATION_LAST_ROW = 1000;

    /**
     * 양식을 만들어 바이트로 돌려준다.
     *
     * @param paymentMethodNames D열 드롭다운에 넣을 <b>사용 중 수단</b> 이름. 용도 구분 없이
     *                           전부다 — 양식의 A열이 지출·소득을 모두 담기 때문이다
     * @param expendGroupNames   E열 드롭다운에 넣을 <b>사용 중 지출유형</b> 이름
     */
    public byte[] write(List<String> paymentMethodNames, List<String> expendGroupNames) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet(ExcelColumn.SHEET_NAME);
            writeHeader(sheet);
            applyDropdown(sheet, ExcelColumn.PAYMENT_METHOD, paymentMethodNames);
            applyDropdown(sheet, ExcelColumn.EXPEND_GROUP, expendGroupNames);
            applyDropdown(sheet, ExcelColumn.KIND,
                    List.of(ExcelColumn.KIND_EXPENSE, ExcelColumn.KIND_INCOME));

            for (ExcelColumn column : ExcelColumn.values()) {
                sheet.autoSizeColumn(column.index());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // 호출자가 9000 으로 바꾼다 — 양식 생성 실패는 사용자 입력 문제가 아니다.
            throw new UncheckedIOException(e);
        }
    }

    /** 1행에 헤더를 적는다. 문자열은 {@link ExcelColumn} 이 소유한다. */
    private static void writeHeader(XSSFSheet sheet) {
        var header = sheet.createRow(0);
        for (ExcelColumn column : ExcelColumn.values()) {
            header.createCell(column.index()).setCellValue(column.header());
        }
    }

    /**
     * 한 열에 데이터 유효성 목록을 건다.
     *
     * <p><b>목록이 비어 있으면 걸지 않는다.</b> 빈 목록으로 만들면 그 열에 아무 값도 넣을
     * 수 없는 양식이 나오는데, 수단이 하나도 없는 회원은 지출을 적을 수 없다는 뜻이 아니라
     * 003 에서 수단을 먼저 만들면 되는 상태다.
     */
    private static void applyDropdown(XSSFSheet sheet, ExcelColumn column, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint =
                helper.createExplicitListConstraint(values.toArray(String[]::new));
        CellRangeAddressList range = new CellRangeAddressList(
                ExcelColumn.HEADER_ROWS, VALIDATION_LAST_ROW, column.index(), column.index());

        DataValidation validation = helper.createValidation(constraint, range);
        // 목록 밖 값을 막되 경고로 알린다. 업로드가 다시 검증하므로 여기서 막지 못해도
        // 데이터가 잘못 저장되지는 않는다.
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }
}

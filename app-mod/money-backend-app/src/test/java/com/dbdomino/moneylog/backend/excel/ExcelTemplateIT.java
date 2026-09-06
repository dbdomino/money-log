package com.dbdomino.moneylog.backend.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;

/**
 * 3.11 양식 다운로드 — quickstart #34·#35·#36·#37.
 *
 * <p><b>#35·#36 이 한 쌍이다.</b> 성공은 파일이고 <b>인증 실패는 래퍼</b>다 —
 * 003 의 아이콘(2.10)은 인증 실패에도 래퍼를 쓰지 않으므로 헷갈리기 쉽다. 사용 흐름이
 * 달라서인데, 2.10 은 {@code <img>}/fetch 로 받는 이미지지만 3.11 은 사용자가 <b>다운로드
 * 버튼을 누르는</b> 흐름이라 실패 사유를 화면에 띄워야 한다(FR-322).
 */
class ExcelTemplateIT extends AbstractExcelIT {

    /** 그 열에 걸린 데이터 유효성 목록의 값들. 없으면 빈 목록이다. */
    private List<String> dropdownValues(XSSFSheet sheet, ExcelColumn column) {
        return sheet.getDataValidations().stream()
                .filter(XSSFDataValidation.class::isInstance)
                .map(XSSFDataValidation.class::cast)
                .filter(validation -> Arrays.stream(validation.getRegions().getCellRangeAddresses())
                        .anyMatch(range -> range.getFirstColumn() == column.index()))
                .flatMap(validation -> Arrays.stream(
                        validation.getValidationConstraint().getExplicitListValues()))
                .toList();
    }

    @Test
    @DisplayName("#35 성공은 { resCode, data } 가 아니라 파일이고 attachment 로 내려온다")
    void successIsAFileNotAWrapper() throws Exception {
        String token = signupAndLogin().token();

        MockHttpServletResponse response = downloadTemplate(token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(XLSX_MIME);
        assertThat(response.getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("expense_income_template.xlsx");
        assertThat(response.getContentAsByteArray()).isNotEmpty();
        // 래퍼가 섞여 나오면 본문이 JSON 이 된다.
        assertThat(response.getContentAsString(StandardCharsets.ISO_8859_1))
                .doesNotContain("resCode");
    }

    @Test
    @DisplayName("#36 Bearer 없이 부르면 래퍼를 쓴다 — 003 의 2.10 과 다르다")
    void withoutTokenUsesTheWrapper() throws Exception {
        MockHttpServletResponse response = downloadTemplate(null);

        // 2.10 은 여기서 본문 없는 401 이지만 3.11 은 래퍼 + HTTP 200 이다.
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = asWrapper(response);
        assertThat(resCode(body)).isEqualTo(1001);
        assertThat(body.get("data").get("message").asString()).isNotBlank();
    }

    @Test
    @DisplayName("#34 양식의 헤더가 A~G 일곱 칸이고 데이터는 2행부터다")
    void headerIsOneRowOfSevenColumns() throws Exception {
        String token = signupAndLogin().token();

        try (XSSFWorkbook workbook = openWorkbook(downloadTemplate(token).getContentAsByteArray())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            for (ExcelColumn column : ExcelColumn.values()) {
                assertThat(header.getCell(column.index()).getStringCellValue())
                        .as("%s 열", column.letter())
                        .isEqualTo(column.header());
            }
            // 안내 행을 두지 않기로 했다 — 헤더 다음이 바로 데이터다.
            assertThat(sheet.getLastRowNum()).isZero();
        }
    }

    @Test
    @DisplayName("#34 본인 사용 중 수단·지출유형이 드롭다운으로 들어 있다")
    void dropdownsCarryTheMembersOwnNames() throws Exception {
        Fixture fixture = prepare();

        try (XSSFWorkbook workbook =
                     openWorkbook(downloadTemplate(fixture.token()).getContentAsByteArray())) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            // 수단은 용도 구분 없이 전부다 — 양식의 A열이 지출·소득을 모두 담는다.
            assertThat(dropdownValues(sheet, ExcelColumn.PAYMENT_METHOD))
                    .contains("국민카드", "월급통장");
            // 지출유형은 가입이 만들어 준 기본 10종이다.
            assertThat(dropdownValues(sheet, ExcelColumn.EXPEND_GROUP))
                    .contains("식비", "교통", "기타");
            assertThat(dropdownValues(sheet, ExcelColumn.KIND))
                    .containsExactlyInAnyOrder("EXPENSE", "INCOME");
        }
    }

    @Test
    @DisplayName("#37 사용 안 함·삭제 표시된 수단은 드롭다운에 없다")
    void unusableNamesAreExcluded() throws Exception {
        Fixture fixture = prepare();
        long deadId = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + deadId, fixture.token())))
                .isEqualTo(200);
        long disabledId = createExpensePaymentMethod(fixture.token(), "잠깐 안 씀");
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + disabledId, fixture.token(), """
                {"inUse":false}
                """))).isEqualTo(200);

        try (XSSFWorkbook workbook =
                     openWorkbook(downloadTemplate(fixture.token()).getContentAsByteArray())) {
            List<String> names = dropdownValues(workbook.getSheetAt(0), ExcelColumn.PAYMENT_METHOD);
            assertThat(names).contains("국민카드").doesNotContain("옛 카드", "잠깐 안 씀");
        }
    }

    @Test
    @DisplayName("#37 남의 수단은 드롭다운에 섞이지 않는다")
    void othersNamesAreNotIncluded() throws Exception {
        Fixture other = prepare();
        createExpensePaymentMethod(other.token(), "남의카드");
        Fixture fixture = prepare();

        try (XSSFWorkbook workbook =
                     openWorkbook(downloadTemplate(fixture.token()).getContentAsByteArray())) {
            assertThat(dropdownValues(workbook.getSheetAt(0), ExcelColumn.PAYMENT_METHOD))
                    .doesNotContain("남의카드");
        }
    }

    @Test
    @DisplayName("수단이 하나도 없어도 양식은 만들어진다")
    void aMemberWithNoPaymentMethodStillGetsATemplate() throws Exception {
        String token = signupAndLogin().token();

        MockHttpServletResponse response = downloadTemplate(token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(XLSX_MIME);
    }
}

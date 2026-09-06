package com.dbdomino.moneylog.backend.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.12 업로드의 정상 경로 — quickstart #38·#39·#45·#47·#48.
 *
 * <p>업로드가 만드는 행의 규칙은 <b>3.1·3.7 과 완전히 같다</b>(excel-contract.md §4).
 * 엑셀이라고 예외를 두지 않는다 — 이름 스냅샷도, 사용 중 참조만 허용하는 것도, 소유자를
 * 토큰이 정하는 것도 같다.
 */
class ExcelUploadIT extends AbstractExcelIT {

    /** 그 회원의 지출 행 하나를 읽는다. */
    private Map<String, Object> expenseRowOf(Fixture fixture) {
        return jdbc.queryForMap("""
                select e.payment_method_idx, e.payment_method_name, e.expend_group_idx,
                       e.expend_group_name, e.amount, e.payment_date, e.place, e.content,
                       e.installment_group_id, e.installment_index, e.installment_total
                  from moneylog.tbl_expense e
                  join moneylog.tbl_user u on u.id_key = e.id_key
                 where u.user_id = ?
                """, fixture.member().memberId());
    }

    @Test
    @DisplayName("#38 각 행이 지출·소득으로 저장되고 별도 '가계부' 행은 없다")
    void rowsBecomeExpensesAndIncomes() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"),
                expenseRow("2026-03-16", "30000", "국민카드", "교통", "지하철", "정기권"),
                incomeRow("2026-03-25", "3000000", "월급통장", "급여")));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(countExpenses(fixture.member())).isEqualTo(2);
        assertThat(countIncomes(fixture.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("#39 성공 응답은 래퍼이고 건수 3개와 message 를 담는다 — 파일을 돌려주지 않는다")
    void successUsesTheWrapper() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"),
                incomeRow("2026-03-25", "3000000", "월급통장", "급여")));

        JsonNode data = upload(fixture, file).get("data");

        assertThat(data.get("importedCount").asInt()).isEqualTo(2);
        assertThat(data.get("expenseCount").asInt()).isEqualTo(1);
        assertThat(data.get("incomeCount").asInt()).isEqualTo(1);
        assertThat(data.get("message").asString()).isNotBlank();
        // 성공에도 errors 필드가 있고 비어 있다 — 프론트가 길이만 보면 된다.
        assertThat(data.get("errors").isArray()).isTrue();
        assertThat(data.get("errors")).isEmpty();
    }

    @Test
    @DisplayName("#47 업로드가 만든 행의 이름 스냅샷이 3.1·3.7 과 같은 규칙으로 채워진다")
    void snapshotsFollowTheSameRule() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심")));
        assertThat(resCode(upload(fixture, file))).isEqualTo(200);

        Map<String, Object> row = expenseRowOf(fixture);
        assertThat(row.get("payment_method_name")).isEqualTo("국민카드");
        assertThat(row.get("expend_group_name")).isEqualTo("식비");
        // 참조도 함께 걸린다 — 이름만 저장하면 수정 화면이 원본을 찾지 못한다.
        assertThat(row.get("payment_method_idx")).isNotNull();
        assertThat(row.get("expend_group_idx")).isNotNull();
    }

    @Test
    @DisplayName("#47 업로드 후 원본 이름을 바꿔도 스냅샷은 그대로다")
    void snapshotsDoNotFollowLaterRenames() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(upload(fixture, workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심")))))).isEqualTo(200);
        long methodId = ((Number) expenseRowOf(fixture).get("payment_method_idx")).longValue();

        assertThat(resCode(patchJson("/api/v1/payment-methods/" + methodId, fixture.token(), """
                {"name":"국민카드(메인)"}
                """))).isEqualTo(200);

        assertThat(expenseRowOf(fixture).get("payment_method_name")).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#48 업로드가 만든 지출 행의 할부 3컬럼은 전부 NULL 이다")
    void uploadedExpensesAreLumpSum() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(upload(fixture, workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심")))))).isEqualTo(200);

        Map<String, Object> row = expenseRowOf(fixture);
        // 양식에 할부 열이 없으므로 업로드는 일시불만 만든다.
        assertThat(row.get("installment_group_id")).isNull();
        assertThat(row.get("installment_index")).isNull();
        assertThat(row.get("installment_total")).isNull();
    }

    @Test
    @DisplayName("#45 같은 내용의 행이 두 번 있으면 둘 다 저장된다 — 중복 허용")
    void duplicateRowsAreBothSaved() throws Exception {
        Fixture fixture = prepare();
        String[] same = expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심");
        byte[] file = workbook(List.<String[]>of(same, same));

        JsonNode response = upload(fixture, file);

        assertThat(resCode(response)).isEqualTo(200);
        // 실제로 같은 날 같은 금액을 두 번 쓸 수 있다(FR-309).
        assertThat(countExpenses(fixture.member())).isEqualTo(2);
    }

    @Test
    @DisplayName("소득 행의 content 는 비워도 된다")
    void incomeContentIsOptional() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(incomeRow("2026-03-25", "3000000", "월급통장", null)));

        assertThat(resCode(upload(fixture, file))).isEqualTo(200);
        assertThat(countIncomes(fixture.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("표 아래의 빈 줄은 데이터로 세지 않는다")
    void trailingBlankRowsAreIgnored() throws Exception {
        Fixture fixture = prepare();
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"),
                new String[] {null, null, null, null, null, null, null},
                new String[] {"", "", "", "", "", "", ""}));

        JsonNode response = upload(fixture, file);

        // 빈 줄을 데이터로 세면 정상 파일이 3502 로 거절된다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("importedCount").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("금액이 숫자 셀이어도 12000.0 이 아니라 12000 으로 읽힌다")
    void numericCellsAreReadAsIntegers() throws Exception {
        Fixture fixture = prepare();
        byte[] file;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var out = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(ExcelColumn.SHEET_NAME);
            var header = sheet.createRow(0);
            for (ExcelColumn column : ExcelColumn.values()) {
                header.createCell(column.index()).setCellValue(column.header());
            }
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(ExcelColumn.KIND_EXPENSE);
            row.createCell(1).setCellValue("2026-03-15");
            row.createCell(2).setCellValue(12000);   // 숫자 셀
            row.createCell(3).setCellValue("국민카드");
            row.createCell(4).setCellValue("식비");
            row.createCell(5).setCellValue("편의점");
            row.createCell(6).setCellValue("점심");
            workbook.write(out);
            file = out.toByteArray();
        }

        assertThat(resCode(upload(fixture, file))).isEqualTo(200);
        assertThat(expenseRowOf(fixture).get("amount")).isEqualTo(12000L);
    }

    @Test
    @DisplayName("토큰 없이 업로드하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        byte[] file = workbook(List.<String[]>of(
                expenseRow("2026-03-15", "12000", "국민카드", "식비", "편의점", "점심")));

        assertThat(resCode(upload(null, file))).isEqualTo(1001);
    }
}

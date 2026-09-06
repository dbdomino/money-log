package com.dbdomino.moneylog.backend.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 엑셀(3.11·3.12) 통합 테스트의 공통 바탕.
 *
 * <p><b>검증용 {@code .xlsx} 를 테스트가 POI 로 직접 만든다.</b> 고정 파일을 리소스에 두면
 * 컬럼 정의가 바뀔 때 같이 안 바뀌어 시험이 옛 양식을 계속 통과시킨다(plan.md).
 *
 * <p>행을 문자열 배열로 받아 그대로 셀에 넣는다 — 값을 해석하지 않으므로 잘못된 값
 * (소수점 금액·엉뚱한 날짜)도 그대로 만들 수 있어야 검증 시험이 성립한다.
 */
abstract class AbstractExcelIT extends AbstractApiIT {

    protected static final String TEMPLATE_URL = "/api/v1/expense-incomes/excel/template";
    protected static final String UPLOAD_URL = "/api/v1/expense-incomes/excel/upload";

    /** {@code .xlsx} 의 MIME 타입. */
    protected static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** 엑셀을 쓸 준비가 끝난 회원 — 가입이 만든 기본 유형과 지출·소득용 수단을 갖는다. */
    protected record Fixture(Member member, String expenseMethodName, String incomeMethodName,
                             String expendGroupName) {

        String token() {
            return member.token();
        }
    }

    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        createExpensePaymentMethod(member.token(), "국민카드");
        createIncomePaymentMethod(member.token(), "월급통장");
        return new Fixture(member, "국민카드", "월급통장", "식비");
    }

    /**
     * 헤더 한 줄과 주어진 데이터 행으로 {@code .xlsx} 를 만든다.
     *
     * <p>헤더 문자열과 열 순서는 {@link ExcelColumn} 에서 가져온다 — 시험이 정의를 따로
     * 적으면 FR-318("양식과 업로드의 컬럼 정의가 일치")을 시험 자신이 어기게 된다.
     */
    protected byte[] workbook(List<String[]> dataRows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(ExcelColumn.SHEET_NAME);
            var header = sheet.createRow(0);
            for (ExcelColumn column : ExcelColumn.values()) {
                header.createCell(column.index()).setCellValue(column.header());
            }
            for (int i = 0; i < dataRows.size(); i++) {
                var row = sheet.createRow(i + ExcelColumn.HEADER_ROWS);
                String[] values = dataRows.get(i);
                for (int c = 0; c < values.length; c++) {
                    if (values[c] != null) {
                        row.createCell(c).setCellValue(values[c]);
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** 지출 행 하나. 열 순서는 A~G 다. */
    protected String[] expenseRow(String date, String amount, String method, String group,
                                  String place, String content) {
        return new String[] {ExcelColumn.KIND_EXPENSE, date, amount, method, group, place, content};
    }

    /** 소득 행 하나. E·F(지출유형·장소)는 비워야 한다. */
    protected String[] incomeRow(String date, String amount, String method, String content) {
        return new String[] {ExcelColumn.KIND_INCOME, date, amount, method, null, null, content};
    }

    /** 3.12 업로드. 파트 이름은 {@code file} 이다. */
    protected JsonNode upload(Fixture fixture, byte[] content, String filename) throws Exception {
        var file = new MockMultipartFile("file", filename, XLSX_MIME, content);
        var request = MockMvcRequestBuilders.multipart(UPLOAD_URL).file(file);
        if (fixture != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token());
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** 3.12 업로드 — 기본 파일명. */
    protected JsonNode upload(Fixture fixture, byte[] content) throws Exception {
        return upload(fixture, content, "ledger.xlsx");
    }

    /** 3.11 양식 다운로드. 성공은 파일이라 원시 응답을 그대로 본다. */
    protected MockHttpServletResponse downloadTemplate(String token) throws Exception {
        var request = MockMvcRequestBuilders.get(TEMPLATE_URL);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    /** 내려받은 양식을 다시 열어 시트를 본다. */
    protected XSSFWorkbook openWorkbook(byte[] content) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(content));
    }

    /** 그 회원의 지출·소득 건수. */
    protected int countExpenses(Member member) {
        return count("tbl_expense", member);
    }

    protected int countIncomes(Member member) {
        return count("tbl_income", member);
    }

    private int count(String table, Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.%s t
                  join moneylog.tbl_user u on u.id_key = t.id_key
                 where u.user_id = ?
                """.formatted(table), Integer.class, member.memberId());
        return count == null ? 0 : count;
    }

    /** 응답이 JSON 래퍼인지 확인하고 파싱한다. */
    protected JsonNode asWrapper(MockHttpServletResponse response) throws Exception {
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    }
}

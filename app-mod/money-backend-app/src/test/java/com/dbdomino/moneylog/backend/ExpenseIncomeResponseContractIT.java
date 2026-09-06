package com.dbdomino.moneylog.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 004 가 만든 API 12건의 응답 규격 — quickstart #50·#51 (SC-301).
 *
 * <p><b>예외를 인정하는 시험과 나머지를 규격으로 묶는 시험이 한 자리에 있어야</b> 그 예외가
 * 하나뿐임이 드러난다. 따로 두면 두 번째 예외가 생겨도 아무도 알아채지 못한다 —
 * 003 의 {@code ExpendGroupResponseContractIT} 와 같은 형태다.
 *
 * <p><b>004 에는 목록 API 가 없다.</b> 3.1~3.10 은 전부 단건이고 월별 목록은 005 의
 * 가계부 목록(4.8)이 담당하므로, {@code data.list} 규칙이 적용되는 API 가 없다
 * (api-contract.md §2). 목록처럼 보이는 3.5·3.12 의 응답은 <b>결과 요약</b>이지
 * 목록이 아니다.
 */
class ExpenseIncomeResponseContractIT extends AbstractApiIT {

    private static final String EXPENSE_URL = "/api/v1/expenses";
    private static final String INCOME_URL = "/api/v1/incomes";
    private static final String TEMPLATE_URL = "/api/v1/expense-incomes/excel/template";
    private static final String UPLOAD_URL = "/api/v1/expense-incomes/excel/upload";

    /** 그 응답이 {@code { resCode, data }} 규격이고 성공인가. */
    private void assertWrapped(JsonNode response, String api) {
        assertThat(response.has("resCode")).as("%s 에 resCode 가 없다", api).isTrue();
        assertThat(response.has("data")).as("%s 에 data 가 없다", api).isTrue();
        assertThat(response.size()).as("%s 의 최상위 필드는 둘뿐이어야 한다", api).isEqualTo(2);
        assertThat(response.get("resCode").asInt()).as("%s", api).isEqualTo(200);
    }

    @Test
    @DisplayName("#50 004 의 11건은 { resCode, data } 규격이다")
    void elevenApisUseTheWrapper() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        long expenseMethodId = createExpensePaymentMethod(token, "국민카드");
        long incomeMethodId = createIncomePaymentMethod(token, "월급통장");
        long groupId = defaultGroupId(member, "식비");

        // 3.1 지출 등록
        JsonNode expense = postJson(EXPENSE_URL, token, """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """.formatted(expenseMethodId, groupId));
        assertWrapped(expense, "3.1 ExpenseCreate");
        long expenseId = expense.get("data").get("expenseId").asLong();

        assertWrapped(getJson(EXPENSE_URL + "/" + expenseId, token), "3.2 ExpenseGet");
        assertWrapped(patchJson(EXPENSE_URL + "/" + expenseId, token, """
                {"amount":15000}
                """), "3.3 ExpenseUpdate");

        // 3.5 할부 등록 — 목록이 아니라 결과 요약이다.
        JsonNode installment = postJson(EXPENSE_URL + "/installments", token, """
                {"paymentMethodId":%d,"expendGroupId":%d,"monthlyAmount":100000,
                 "installmentMonths":3,"startYearMonth":"2020-01",
                 "place":"백화점","content":"노트북 할부"}
                """.formatted(expenseMethodId, groupId));
        assertWrapped(installment, "3.5 ExpenseCreateInstallment");
        long groupIdOfInstallment = installment.get("data").get("installmentGroupId").asLong();

        // 3.6 중도상환 — 미래 회차가 있어야 하므로 새 그룹을 하나 더 만든다.
        JsonNode future = postJson(EXPENSE_URL + "/installments", token, """
                {"paymentMethodId":%d,"expendGroupId":%d,"monthlyAmount":100000,
                 "installmentMonths":3,"startYearMonth":"%s",
                 "place":"백화점","content":"미래 할부"}
                """.formatted(expenseMethodId, groupId,
                java.time.YearMonth.now().plusMonths(1)));
        assertWrapped(future, "3.5 ExpenseCreateInstallment (미래)");
        assertWrapped(patchJson(EXPENSE_URL + "/installments/"
                        + future.get("data").get("installmentGroupId").asLong() + "/remainder",
                token, "{}"), "3.6 ExpenseSettleInstallmentRemainder");

        assertWrapped(deleteJson(EXPENSE_URL + "/" + expenseId, token), "3.4 ExpenseDelete");

        // 3.7 소득 등록
        JsonNode income = postJson(INCOME_URL, token, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"급여"}
                """.formatted(incomeMethodId));
        assertWrapped(income, "3.7 IncomeCreate");
        long incomeId = income.get("data").get("incomeId").asLong();

        assertWrapped(getJson(INCOME_URL + "/" + incomeId, token), "3.8 IncomeGet");
        assertWrapped(patchJson(INCOME_URL + "/" + incomeId, token, """
                {"amount":3500000}
                """), "3.9 IncomeUpdate");
        assertWrapped(deleteJson(INCOME_URL + "/" + incomeId, token), "3.10 IncomeDelete");

        // 3.12 엑셀 업로드 — 파일을 돌려주지 않으므로 예외가 아니다.
        assertWrapped(uploadEmptyValidFile(token, expenseMethodId), "3.12 ExcelUpload");

        // 남은 그룹이 실제로 만들어졌는지 확인해 3.5 가 빈 응답이 아님을 못박는다.
        assertThat(groupIdOfInstallment).isPositive();
    }

    @Test
    @DisplayName("#50 3.11 만 규격의 예외다 — 성공 본문이 .xlsx 바이너리다")
    void onlyTheTemplateDownloadIsExempt() throws Exception {
        String token = signupAndLogin().token();

        MockHttpServletResponse response = mockMvc.perform(
                        MockMvcRequestBuilders.get(TEMPLATE_URL)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment");
        // 이 하나만 래퍼가 아니다. 두 번째 예외가 생기면 위 시험이 먼저 깨진다.
        assertThat(response.getContentAsString(StandardCharsets.ISO_8859_1))
                .doesNotContain("resCode");
    }

    @Test
    @DisplayName("#50 3.11 도 실패에는 래퍼를 쓴다 — 003 의 2.10 과 다르다")
    void theExemptApiStillWrapsFailures() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(
                MockMvcRequestBuilders.get(TEMPLATE_URL)).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode body = objectMapper.readTree(
                response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(resCode(body)).isEqualTo(1001);
    }

    @Test
    @DisplayName("#51 비즈니스 실패는 HTTP 200 + 4자리 resCode 다")
    void businessFailuresAreHttp200() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        // 지출 3202 · 소득 3302 · 할부 3206 — 세 대역이 모두 같은 형태로 나온다.
        for (String url : new String[] {
                EXPENSE_URL + "/999999999", INCOME_URL + "/999999999"}) {
            MockHttpServletResponse raw = mockMvc.perform(MockMvcRequestBuilders.get(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andReturn().getResponse();

            assertThat(raw.getStatus()).as(url).isEqualTo(200);
            JsonNode body = objectMapper.readTree(
                    raw.getContentAsString(StandardCharsets.UTF_8));
            assertThat(body.size()).isEqualTo(2);
            assertThat(String.valueOf(body.get("resCode").asInt()))
                    .as("%s 의 resCode 는 4자리여야 한다", url)
                    .hasSize(4);
            assertThat(body.get("data").get("message").asString()).isNotBlank();
        }
    }

    @Test
    @DisplayName("#51 3502 만 data 에 errors[] 를 더 싣는다 — 나머지는 message 한 칸이다")
    void onlyRowValidationCarriesErrors() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        long methodId = createExpensePaymentMethod(token, "국민카드");

        // 값이 잘못된 행 하나를 올려 3502 를 받는다.
        JsonNode failed = upload(token, workbookWith(
                new String[] {"EXPENSE", "2026-03-15", "0", "국민카드", "식비", "편의점", "점심"}));
        assertThat(resCode(failed)).isEqualTo(3502);
        assertThat(failed.get("data").get("errors").isArray()).isTrue();
        assertThat(failed.size()).as("최상위는 resCode·data 둘뿐이다").isEqualTo(2);

        // 파일 단위 거절은 message 한 칸이다.
        JsonNode fileLevel = upload(token, "엑셀이 아니다".getBytes(StandardCharsets.UTF_8));
        assertThat(resCode(fileLevel)).isEqualTo(3503);
        assertThat(fileLevel.get("data").size()).isEqualTo(1);
        assertThat(methodId).isPositive();
    }

    /** 헤더와 데이터 행 하나를 담은 {@code .xlsx}. */
    private byte[] workbookWith(String[] row) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("지출소득");
            String[] headers = {"구분", "결제일", "금액", "수단", "지출유형", "장소", "내용"};
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            var data = sheet.createRow(1);
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    data.createCell(i).setCellValue(row[i]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** 정상 행 하나를 담은 파일을 올린다 — 3.12 의 성공 응답 형태를 보려는 것이다. */
    private JsonNode uploadEmptyValidFile(String token, long expenseMethodId) throws Exception {
        return upload(token, workbookWith(
                new String[] {"EXPENSE", "2026-03-15", "12000", "국민카드", "식비", "편의점", "점심"}));
    }

    private JsonNode upload(String token, byte[] content) throws Exception {
        var file = new MockMultipartFile("file", "ledger.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        var request = MockMvcRequestBuilders.multipart(UPLOAD_URL).file(file);
        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return objectMapper.readTree(mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

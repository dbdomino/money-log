package com.dbdomino.moneylog.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 005 가 만든 API 9건의 응답 규격 — quickstart #55 (SC-401).
 *
 * <h2>005 에는 래퍼 예외가 하나도 없다</h2>
 *
 * <p>003 과 004 에는 {@code { resCode, data }} 를 쓰지 않는 API 가 하나씩 있었다.
 *
 * <table border="1">
 *   <caption>앞선 기능의 예외</caption>
 *   <tr><th>API</th><th>어떻게 달랐나</th></tr>
 *   <tr><td>003 아이콘 조회(2.10)</td><td><b>성공도 실패도</b> 래퍼가 아니다 —
 *       {@code <img>} 로 받는 이미지라 실패는 본문 없는 HTTP 상태코드다</td></tr>
 *   <tr><td>004 엑셀 양식(3.11)</td><td><b>성공만</b> 예외 — 본문이 {@code .xlsx}
 *       바이너리다. 실패는 래퍼 + HTTP 200</td></tr>
 * </table>
 *
 * <p><b>005 에는 그런 API 가 없다.</b> 파일을 돌려주는 것도, 본문 없는 응답도 없다.
 * 그래서 이 시험은 "예외를 인정하는 시험"을 두지 않고 <b>9건 전부가 규격을 지킨다</b>만
 * 단언한다 — 나중에 예외가 하나라도 생기면 여기서 바로 깨진다.
 *
 * <p>003 의 {@code ExpendGroupResponseContractIT}·004 의
 * {@code ExpenseIncomeResponseContractIT} 와 같은 자리이며, 세 시험을 나란히 두면
 * <b>어느 기능에 예외가 몇 개인지</b>가 한눈에 드러난다.
 */
class LedgerFixedExpenseResponseContractIT extends AbstractApiIT {

    private static final String FIXED_URL = "/api/v1/fixed-expenses";
    private static final String MONTHLY_URL = "/api/v1/fixed-expenses/monthly";
    private static final String SYNC_URL = "/api/v1/fixed-expenses/monthly/sync";
    private static final String LEDGER_URL = "/api/v1/ledger/monthly";

    /** 그 응답이 {@code { resCode, data }} 규격이고 성공인가. */
    private void assertWrapped(JsonNode response, String api) {
        assertThat(response.has("resCode")).as("%s 에 resCode 가 없다", api).isTrue();
        assertThat(response.has("data")).as("%s 에 data 가 없다", api).isTrue();
        assertThat(response.size()).as("%s 의 최상위 필드는 둘뿐이어야 한다", api).isEqualTo(2);
        assertThat(response.get("resCode").asInt()).as("%s", api).isEqualTo(200);
    }

    /** 그 회원에게 지출용 수단·고정지출 설정을 세우고 설정 PK 를 돌려준다. */
    private long setUpFixedExpense(Member member, YearMonth start, YearMonth end)
            throws Exception {
        long paymentMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        return createFixedExpense(member.token(), "월세", paymentMethodId,
                defaultGroupId(member, "주거"), 500000L, 25, start.toString(), end.toString());
    }

    @Test
    @DisplayName("#55 005 의 9건이 전부 { resCode, data } 규격이다")
    void everyOneOfTheNineUsesTheWrapper() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        YearMonth target = YearMonth.now();

        // 4.1 등록
        JsonNode created = postJson(FIXED_URL, token, """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"매달 월세",
                 "startYear":%d,"startMonth":%d,"endYear":%d,"endMonth":%d}
                """.formatted(createExpensePaymentMethod(token, "국민카드"),
                defaultGroupId(member, "주거"),
                target.getYear(), target.getMonthValue(),
                target.plusMonths(6).getYear(), target.plusMonths(6).getMonthValue()));
        assertWrapped(created, "4.1 FixedExpenseCreate");
        long fixedExpenseId = created.get("data").get("fixedExpenseId").asLong();

        assertWrapped(getJson(FIXED_URL + "?offset=0&limit=10", token), "4.2 FixedExpenseList");
        assertWrapped(getJson(FIXED_URL + "/" + fixedExpenseId, token), "4.3 FixedExpenseGet");
        assertWrapped(patchJson(FIXED_URL + "/" + fixedExpenseId, token, """
                {"amount":550000}
                """), "4.4 FixedExpenseUpdate");

        // 4.5 — 이 호출이 그 달의 월별 내역을 만든다.
        assertWrapped(getJson(MONTHLY_URL + "?year=" + target.getYear()
                + "&month=" + target.getMonthValue(), token), "4.5 FixedExpenseMonthlyList");

        assertWrapped(patchJson(MONTHLY_URL + "/" + target.getYear() + "/"
                + target.getMonthValue() + "/" + fixedExpenseId, token, """
                {"amount":600000}
                """), "4.6 FixedExpenseMonthlyUpdate");

        assertWrapped(getJson(LEDGER_URL + "?year=" + target.getYear()
                + "&month=" + target.getMonthValue(), token), "4.8 LedgerMonthlyList");

        assertWrapped(postJson(SYNC_URL, token, """
                {"year":%d,"month":%d}
                """.formatted(target.getYear(), target.getMonthValue())),
                "4.9 FixedExpenseMonthlySync");

        // 4.7 삭제는 마지막이다 — 지우면 위 경로들이 3402 가 된다.
        assertWrapped(deleteJson(FIXED_URL + "/" + fixedExpenseId, token),
                "4.7 FixedExpenseDelete");
    }

    @Test
    @DisplayName("#55 9건 모두 성공 본문이 JSON 이다 — 바이너리를 돌려주는 API 가 없다")
    void noEndpointReturnsBinary() throws Exception {
        Member member = signupAndLogin();
        YearMonth target = YearMonth.now();
        long fixedExpenseId = setUpFixedExpense(member, target, target.plusMonths(6));

        List<String> getUrls = List.of(
                FIXED_URL + "?offset=0&limit=10",
                FIXED_URL + "/" + fixedExpenseId,
                MONTHLY_URL + "?year=" + target.getYear() + "&month=" + target.getMonthValue(),
                LEDGER_URL + "?year=" + target.getYear() + "&month=" + target.getMonthValue());

        for (String url : getUrls) {
            MockHttpServletResponse response = mockMvc.perform(
                            MockMvcRequestBuilders.get(url)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + member.token()))
                    .andReturn().getResponse();

            // 004 의 3.11 은 여기서 .xlsx MIME 이 나왔다. 005 에는 그런 API 가 없다.
            assertThat(response.getContentType()).as(url).startsWith("application/json");
            assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                    .as("%s 의 본문에 resCode 가 있어야 한다", url)
                    .contains("resCode");
        }
    }

    @Test
    @DisplayName("#55 비즈니스 실패도 HTTP 200 + 4자리 resCode 다")
    void businessFailuresAreHttp200() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        // 3402(없는 설정) · 3403(4.5 연·월) · 3501(4.8 연·월) 세 대역을 건다.
        List<String> failingUrls = List.of(
                FIXED_URL + "/999999999",
                MONTHLY_URL + "?year=2026&month=13",
                LEDGER_URL + "?year=2026&month=13");

        for (String url : failingUrls) {
            MockHttpServletResponse raw = mockMvc.perform(MockMvcRequestBuilders.get(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andReturn().getResponse();

            assertThat(raw.getStatus()).as(url).isEqualTo(200);
            JsonNode body = objectMapper.readTree(
                    raw.getContentAsString(StandardCharsets.UTF_8));
            assertThat(body.size()).as("%s 의 최상위는 둘뿐이다", url).isEqualTo(2);
            assertThat(String.valueOf(body.get("resCode").asInt()))
                    .as("%s 의 resCode 는 4자리여야 한다", url)
                    .hasSize(4);
            assertThat(body.get("data").get("message").asString()).isNotBlank();
        }
    }

    @Test
    @DisplayName("#55 실패 응답의 data 는 message 한 칸이다 — 005 에는 errors[] 가 없다")
    void failureDataCarriesOnlyAMessage() throws Exception {
        String token = signupAndLogin().token();

        JsonNode body = getJson(FIXED_URL + "/999999999", token);

        // 004 의 3502(엑셀 행 검증)만 errors[] 를 더 싣는다. 005 에는 그런 코드가 없다.
        assertThat(body.get("data").size()).isEqualTo(1);
        assertThat(body.get("data").has("errors")).isFalse();
    }

    @Test
    @DisplayName("#55 토큰 없이 부르면 9건 모두 1001 이고 그것도 래퍼다")
    void everyEndpointRequiresLoginAndWrapsThatFailure() throws Exception {
        YearMonth target = YearMonth.now();
        String ym = "year=" + target.getYear() + "&month=" + target.getMonthValue();

        // GET 넷
        for (String url : List.of(FIXED_URL + "?offset=0&limit=10", FIXED_URL + "/1",
                MONTHLY_URL + "?" + ym, LEDGER_URL + "?" + ym)) {
            assertThat(resCode(getJson(url, null))).as("GET %s", url).isEqualTo(1001);
        }
        // POST 둘
        assertThat(resCode(postJson(FIXED_URL, null, "{}"))).isEqualTo(1001);
        assertThat(resCode(postJson(SYNC_URL, null, "{\"year\":2026,\"month\":7}")))
                .isEqualTo(1001);
        // PATCH 둘
        assertThat(resCode(patchJson(FIXED_URL + "/1", null, "{\"amount\":1}"))).isEqualTo(1001);
        assertThat(resCode(patchJson(MONTHLY_URL + "/" + target.getYear() + "/"
                + target.getMonthValue() + "/1", null, "{\"amount\":1}"))).isEqualTo(1001);
        // DELETE 하나
        assertThat(resCode(deleteJson(FIXED_URL + "/1", null))).isEqualTo(1001);
    }

    @Test
    @DisplayName("#55 미인증 응답도 최상위가 resCode·data 둘뿐이다")
    void unauthenticatedResponseIsAlsoTheWrapper() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(
                MockMvcRequestBuilders.get(FIXED_URL + "?offset=0&limit=10"))
                .andReturn().getResponse();

        // 003 의 2.10(아이콘)은 여기서 본문 없는 401 이다. 005 에는 그런 API 가 없다.
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(
                response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(2);
        assertThat(resCode(body)).isEqualTo(1001);
        assertThat(body.get("data").get("message").asString()).isNotBlank();
    }

    @Test
    @DisplayName("#55 목록 셋이 전부 data.list 를 쓴다 — 배열을 data 에 직접 두지 않는다")
    void everyListUsesDataList() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        YearMonth target = YearMonth.now();
        setUpFixedExpense(member, target, target.plusMonths(6));
        String ym = "year=" + target.getYear() + "&month=" + target.getMonthValue();

        for (String url : List.of(FIXED_URL + "?offset=0&limit=10",
                MONTHLY_URL + "?" + ym, LEDGER_URL + "?" + ym)) {
            JsonNode data = getJson(url, token).get("data");
            assertThat(data.isObject()).as("%s 의 data 는 object 다", url).isTrue();
            assertThat(data.get("list").isArray()).as("%s 에 data.list 가 있다", url).isTrue();
        }

        // 4.9 도 목록을 싣는다.
        JsonNode sync = postJson(SYNC_URL, token, """
                {"year":%d,"month":%d}
                """.formatted(target.getYear(), target.getMonthValue()));
        assertThat(sync.get("data").get("list").isArray()).isTrue();
    }

    @Test
    @DisplayName("#55 페이징 필드는 4.2 에만 있다")
    void onlyTheSettingListIsPaged() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        YearMonth target = YearMonth.now();
        setUpFixedExpense(member, target, target.plusMonths(6));
        String ym = "year=" + target.getYear() + "&month=" + target.getMonthValue();

        JsonNode paged = getJson(FIXED_URL + "?offset=0&limit=10", token).get("data");
        assertThat(paged.has("offset")).isTrue();
        assertThat(paged.has("limit")).isTrue();
        assertThat(paged.has("totalCount")).isTrue();

        // 나머지 셋은 한 달치를 전부 돌려주고 합계·건수를 대신 싣는다.
        for (String url : List.of(MONTHLY_URL + "?" + ym, LEDGER_URL + "?" + ym)) {
            JsonNode data = getJson(url, token).get("data");
            assertThat(data.has("offset")).as("%s", url).isFalse();
            assertThat(data.has("limit")).as("%s", url).isFalse();
            assertThat(data.has("totalCount")).as("%s", url).isFalse();
        }
    }
}

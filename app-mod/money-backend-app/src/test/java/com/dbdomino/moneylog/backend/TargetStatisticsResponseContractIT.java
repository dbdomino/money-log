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
 * 006 이 만든 API 6건의 응답 규격 — quickstart #53 (SC-501).
 *
 * <h2>006 에도 래퍼 예외가 하나도 없다</h2>
 *
 * <table border="1">
 *   <caption>기능별 래퍼 예외 수</caption>
 *   <tr><th>기능</th><th>예외</th></tr>
 *   <tr><td>003</td><td>아이콘 조회(2.10) — <b>성공도 실패도</b> 래퍼가 아니다</td></tr>
 *   <tr><td>004</td><td>엑셀 양식(3.11) — <b>성공만</b> 예외({@code .xlsx} 바이너리)</td></tr>
 *   <tr><td>005</td><td>없음</td></tr>
 *   <tr><td><b>006</b></td><td><b>없음</b></td></tr>
 * </table>
 *
 * <p>006 에는 파일을 돌려주는 API 도, 본문 없는 응답도 없다. 그래서 이 시험은 "예외를
 * 인정하는 시험"을 두지 않고 <b>6건 전부가 규격을 지킨다</b>만 단언한다 — 나중에 예외가
 * 하나라도 생기면 여기서 바로 깨진다.
 *
 * <p>003·004·005 의 같은 시험과 나란히 두면 <b>어느 기능에 예외가 몇 개인지</b>가 한눈에
 * 드러난다.
 */
class TargetStatisticsResponseContractIT extends AbstractApiIT {

    private static final String TARGET_URL = "/api/v1/expend-targets";
    private static final String STATISTICS_URL = "/api/v1/statistics/monthly";
    private static final String SAVE_URL = "/api/v1/statistics/monthly/save";

    /** 조회·저장에 쓰는 연·월. 저장이 걸리므로 <b>미래가 아닌</b> 달이어야 한다. */
    private static final YearMonth TARGET_MONTH = YearMonth.now();

    /** 그 응답이 {@code { resCode, data }} 규격이고 성공인가. */
    private void assertWrapped(JsonNode response, String api) {
        assertThat(response.has("resCode")).as("%s 에 resCode 가 없다", api).isTrue();
        assertThat(response.has("data")).as("%s 에 data 가 없다", api).isTrue();
        assertThat(response.size()).as("%s 의 최상위 필드는 둘뿐이어야 한다", api).isEqualTo(2);
        assertThat(response.get("resCode").asInt()).as("%s", api).isEqualTo(200);
    }

    private static String ym() {
        return "year=" + TARGET_MONTH.getYear() + "&month=" + TARGET_MONTH.getMonthValue();
    }

    private static String path() {
        return TARGET_MONTH.getYear() + "/" + TARGET_MONTH.getMonthValue();
    }

    @Test
    @DisplayName("#53 006 의 6건이 전부 { resCode, data } 규격이다")
    void everyOneOfTheSixUsesTheWrapper() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        long groupId = defaultGroupId(member, "식비");

        // 5.3 기본 목표 저장
        assertWrapped(putDefaultTarget(token, groupId, 400_000L), "5.3");
        // 5.4 월별 목표 저장
        assertWrapped(putMonthlyTarget(token, TARGET_MONTH.getYear(),
                TARGET_MONTH.getMonthValue(), groupId, 500_000L), "5.4");
        // 5.1 목록
        assertWrapped(getJson(TARGET_URL + "?" + ym() + "&offset=0&limit=10", token), "5.1");
        // 5.2 단건
        assertWrapped(getJson(TARGET_URL + "/" + path() + "/" + groupId, token), "5.2");
        // 5.5 통계 조회
        assertWrapped(getJson(STATISTICS_URL + "/" + path(), token), "5.5");
        // 5.6 통계 저장
        assertWrapped(postJson(SAVE_URL, token, """
                {"year":%d,"month":%d}
                """.formatted(TARGET_MONTH.getYear(), TARGET_MONTH.getMonthValue())), "5.6");
    }

    @Test
    @DisplayName("#53 6건 모두 성공 본문이 JSON 이다 — 바이너리를 돌려주는 API 가 없다")
    void noEndpointReturnsBinary() throws Exception {
        Member member = signupAndLogin();
        long groupId = defaultGroupId(member, "식비");

        List<String> getUrls = List.of(
                TARGET_URL + "?" + ym() + "&offset=0&limit=10",
                TARGET_URL + "/" + path() + "/" + groupId,
                STATISTICS_URL + "/" + path(),
                STATISTICS_URL + "/" + path() + "?view=live");

        for (String url : getUrls) {
            MockHttpServletResponse response = mockMvc.perform(
                            MockMvcRequestBuilders.get(url)
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + member.token()))
                    .andReturn().getResponse();

            // 004 의 3.11 은 여기서 .xlsx MIME 이 나왔다. 006 에는 그런 API 가 없다.
            assertThat(response.getContentType()).as(url).startsWith("application/json");
            assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                    .as("%s 의 본문에 resCode 가 있어야 한다", url)
                    .contains("resCode");
        }
    }

    /**
     * 비즈니스 실패도 <b>HTTP 200</b> 이다(헌장 원칙 III).
     *
     * <p>006 의 세 대역을 건다 — {@code 3601}(미사용 유형) · {@code 3602}(금액) ·
     * {@code 3603}(연·월) · {@code 3604}(미래 월).
     */
    @Test
    @DisplayName("#53 비즈니스 실패도 HTTP 200 + 4자리 resCode 다")
    void businessFailuresAreHttp200() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        List<String> failingUrls = List.of(
                TARGET_URL + "?year=1999&month=7&offset=0&limit=10",
                TARGET_URL + "/1999/7/999999999",
                STATISTICS_URL + "/2026/13",
                STATISTICS_URL + "/" + path() + "?view=lives");

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

    /** 저장 실패({@code 3604})도 같은 규격이다 — POST 경로를 따로 건다. */
    @Test
    @DisplayName("#53 미래 월 저장 실패도 HTTP 200 + 3604 다")
    void saveFailureIsWrapped() throws Exception {
        String token = signupAndLogin().token();
        YearMonth next = YearMonth.now().plusMonths(1);

        MockHttpServletResponse raw = mockMvc.perform(MockMvcRequestBuilders.post(SAVE_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"year\":%d,\"month\":%d}"
                                .formatted(next.getYear(), next.getMonthValue())))
                .andReturn().getResponse();

        assertThat(raw.getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(raw.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get("resCode").asInt()).isEqualTo(3604);
    }

    @Test
    @DisplayName("#53 실패 응답의 data 는 message 한 칸이다 — 006 에는 errors[] 가 없다")
    void failureDataCarriesOnlyAMessage() throws Exception {
        String token = signupAndLogin().token();

        JsonNode body = getJson(STATISTICS_URL + "/2026/13", token);

        // 004 의 3502(엑셀 행 검증)만 errors[] 를 더 싣는다. 006 에는 그런 코드가 없다.
        assertThat(body.get("data").size()).isEqualTo(1);
        assertThat(body.get("data").has("errors")).isFalse();
    }

    @Test
    @DisplayName("#53 토큰 없이 부르면 6건 모두 1001 이고 그것도 래퍼다")
    void everyEndpointRequiresLoginAndWrapsThatFailure() throws Exception {
        // GET 넷 (5.1 · 5.2 · 5.5 두 갈래)
        for (String url : List.of(
                TARGET_URL + "?" + ym() + "&offset=0&limit=10",
                TARGET_URL + "/" + path() + "/1",
                STATISTICS_URL + "/" + path(),
                STATISTICS_URL + "/" + path() + "?view=live")) {
            assertThat(resCode(getJson(url, null))).as("GET %s", url).isEqualTo(1001);
        }
        // PATCH 둘 (5.3 · 5.4)
        assertThat(resCode(patchJson(TARGET_URL + "/default/1", null,
                "{\"defaultTargetAmount\":1}"))).isEqualTo(1001);
        assertThat(resCode(patchJson(TARGET_URL + "/monthly/" + path() + "/1", null,
                "{\"monthlyTargetAmount\":1}"))).isEqualTo(1001);
        // POST 하나 (5.6)
        assertThat(resCode(postJson(SAVE_URL, null, "{\"year\":2026,\"month\":6}")))
                .isEqualTo(1001);
    }

    @Test
    @DisplayName("#53 미인증 응답도 최상위가 resCode·data 둘뿐이다")
    void unauthenticatedResponseIsAlsoWrapped() throws Exception {
        JsonNode body = getJson(STATISTICS_URL + "/" + path(), null);

        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get("resCode").asInt()).isEqualTo(1001);
        assertThat(body.get("data").has("message")).isTrue();
    }

    /**
     * <b>{@code PUT} 을 쓰는 API 가 없다</b>(헌장 원칙 III).
     *
     * <p>저장 둘이 upsert 라 {@code PUT} 이 자연스러워 보이는 자리인데 {@code PATCH} 로
     * 갔다. 여기서 {@code PUT} 이 통하면 그 결정이 뒤집힌 것이다.
     */
    @Test
    @DisplayName("#53 목표금액 저장을 PUT 으로는 부를 수 없다")
    void noPutEndpoint() throws Exception {
        Member member = signupAndLogin();
        long groupId = defaultGroupId(member, "식비");

        MockHttpServletResponse raw = mockMvc.perform(
                        MockMvcRequestBuilders.put(TARGET_URL + "/default/" + groupId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + member.token())
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"defaultTargetAmount\":400000}"))
                .andReturn().getResponse();

        // 실패도 HTTP 200 이므로 resCode 로 본다.
        JsonNode body = objectMapper.readTree(raw.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("resCode").asInt()).isNotEqualTo(200);
    }
}

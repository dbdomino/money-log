package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 5.6 은 연·월을 Body 로만 받는다 — quickstart #46-1 (FR-524).
 *
 * <h2>Query 를 조용히 읽어 주지 않는다</h2>
 *
 * <p>읽어 주면 <b>입력 경로가 둘</b>이 되고, 둘이 다른 값을 담았을 때 어느 쪽을 따르는지가
 * 구현에 숨는다. 프론트가 Query 로 보내고도 성공하면 그 습관이 굳는데, 나중에 Body 도
 * 함께 보내기 시작하면 그때부터 저장 대상이 조용히 갈린다.
 *
 * <h2>{@code resCode} 로 단언한다 — HTTP 상태가 아니다</h2>
 *
 * <p>이 앱은 <b>실패도 HTTP 200</b> 이다(헌장 원칙 III). 메서드 불일치를
 * {@code status().isMethodNotAllowed()} 로 단언하면 {@code GlobalExceptionHandler} 가
 * 이미 200 + {@code 9001} 로 바꿔 놓아 <b>시험이 통과하는지 여부와 무관하게 아무것도
 * 검증하지 못한다</b> — 005 에서 시험 3건이 그렇게 헛돌았다.
 */
class StatisticsSaveMethodIT extends AbstractStatisticsIT {

    /** {@code Content-Type} 만 붙이고 몸통 없이 POST 한다. */
    private JsonNode postWithoutBody(Fixture fixture, String url) throws Exception {
        var request = MockMvcRequestBuilders.post(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                .contentType(MediaType.APPLICATION_JSON);
        return objectMapper.readTree(mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 임의 메서드로 저장 URL 을 부른다. */
    private JsonNode callWithMethod(Fixture fixture, String method) throws Exception {
        var request = MockMvcRequestBuilders.request(
                        org.springframework.http.HttpMethod.valueOf(method), SAVE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fixture.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\":2026,\"month\":6}");
        return objectMapper.readTree(mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("#46-1 연·월을 Query 로만 보내면 Body 가 비어 3603 이다")
    void rejectsYearMonthInQuery() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = postJson(SAVE_URL + "?year=2026&month=6", fixture.token(), "{}");

        assertThat(resCode(response)).isEqualTo(3603);
        assertThat(countStatistics(fixture.member(), 2026, 6)).isZero();
    }

    @Test
    @DisplayName("#46-1 연·월을 Path 로 붙이면 그런 경로가 없다 — 저장되지 않는다")
    void rejectsYearMonthInPath() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = postJson(SAVE_URL + "/2026/6", fixture.token(), "{}");

        assertThat(resCode(response)).isNotEqualTo(200);
        assertThat(countStatistics(fixture.member(), 2026, 6)).isZero();
    }

    @Test
    @DisplayName("#46-1 몸통을 아예 보내지 않아도 3603 이다 — {} 와 같은 결과다")
    void missingBodyIsSameAsEmptyBody() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postWithoutBody(fixture, SAVE_URL))).isEqualTo(3603);
        assertThat(resCode(postJson(SAVE_URL, fixture.token(), "{}"))).isEqualTo(3603);
    }

    @Test
    @DisplayName("#46-1 month 만 빠져도 3603 이다")
    void rejectsPartialBody() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(SAVE_URL, fixture.token(), "{\"year\":2026}")))
                .isEqualTo(3603);
        assertThat(resCode(postJson(SAVE_URL, fixture.token(), "{\"month\":6}")))
                .isEqualTo(3603);
    }

    /**
     * Body 와 Query 가 <b>다른 값</b>을 담았을 때 Body 를 따른다.
     *
     * <p>Query 를 읽는 구현이면 2026-06 이 저장되어 여기서 걸린다.
     */
    @Test
    @DisplayName("#46-1 Body 와 Query 를 함께 보내면 Body 를 따른다")
    void bodyWinsOverQuery() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();

        JsonNode response = postJson(SAVE_URL + "?year=2026&month=6", fixture.token(),
                "{\"year\":%d,\"month\":%d}".formatted(year, month));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("year").asInt()).isEqualTo(year);
        assertThat(response.get("data").get("month").asInt()).isEqualTo(month);
        assertThat(countStatistics(fixture.member(), year, month)).isEqualTo(1);
        assertThat(countStatistics(fixture.member(), 2026, 6)).isZero();
    }

    /**
     * GET·PATCH·PUT 으로는 부를 수 없다.
     *
     * <p><b>{@code resCode} 로 단언한다</b> — HTTP 상태를 보면 이 앱이 실패도 200 으로
     * 내리므로 시험이 헛돈다.
     */
    @Test
    @DisplayName("#46-1 GET·PATCH·PUT 으로는 저장할 수 없다")
    void rejectsOtherMethods() throws Exception {
        Fixture fixture = prepare();

        for (String method : new String[] {"GET", "PATCH", "PUT", "DELETE"}) {
            JsonNode response = callWithMethod(fixture, method);
            assertThat(resCode(response)).as("%s 응답: %s", method, response).isNotEqualTo(200);
        }
        assertThat(countStatisticsAll(fixture)).isZero();
    }

    /** 그 회원의 통계 행 전체 수. 어떤 메서드로도 행이 생기지 않았음을 본다. */
    private int countStatisticsAll(Fixture fixture) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_statistics s
                  join moneylog.tbl_user u on u.id_key = s.id_key
                 where u.user_id = ?
                """, Integer.class, fixture.member().memberId());
        return count == null ? 0 : count;
    }
}

package com.dbdomino.moneylog.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 4.9 의 요청 형태 — quickstart #38 (FR-421).
 *
 * <p><b>연·월을 Body 로만 받는다.</b> Path 나 Query 를 쓰지 않는다 — 이 저장소의 POST 는
 * 필요한 값을 전부 Body 로 받으며, 4.8 이 GET 이라 Query 를 쓰는 것과 대비된다.
 *
 * <p>Query 로 보내면 <b>Body 가 비어 연·월 누락이 되어 {@code 3403}</b> 이다. 그것이
 * "받지 않는다"의 실제 모습이다 — 조용히 Query 를 읽어 주면 두 입력 경로가 생기고,
 * 둘이 다른 값을 담았을 때 어느 쪽을 따르는지가 구현에 숨는다.
 */
class SyncMethodIT extends AbstractSyncIT {

    /** 임의 메서드로 {@code /sync} 를 부르고 HTTP 상태를 본다. */
    private MockHttpServletResponse call(String method, String url, String token, String body)
            throws Exception {
        var request = switch (method) {
            case "GET" -> MockMvcRequestBuilders.get(url);
            case "PATCH" -> MockMvcRequestBuilders.patch(url);
            case "PUT" -> MockMvcRequestBuilders.put(url);
            default -> MockMvcRequestBuilders.post(url);
        };
        request.contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.content(body);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    @Test
    @DisplayName("#38 Body 로 보내면 동작한다")
    void bodyIsTheOnlyAcceptedForm() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        assertThat(resCode(postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(thisMonth().getYear(), thisMonth().getMonthValue()))))
                .isEqualTo(200);
    }

    @Test
    @DisplayName("#38 Query 로 보내면 받지 않는다 — 3403 이다")
    void queryParametersAreNotRead() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        // Body 를 비우고 Query 에만 담는다. 조용히 Query 를 읽어 주면 여기서 200 이 난다.
        assertThat(resCode(postJson(SYNC_URL + "?year=" + thisMonth().getYear()
                + "&month=" + thisMonth().getMonthValue(), fixture.token(), "{}")))
                .isEqualTo(3403);
    }

    @Test
    @DisplayName("#38 Query 값이 Body 를 덮지 않는다")
    void queryDoesNotOverrideBody() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        // Body 는 유효하고 Query 는 엉뚱한 달이다. Query 를 읽으면 다른 달이 재작성된다.
        var response = postJson(SYNC_URL + "?year=2030&month=1", fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(thisMonth().getYear(), thisMonth().getMonthValue()));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("year").asInt()).isEqualTo(thisMonth().getYear());
        assertThat(response.get("data").get("month").asInt())
                .isEqualTo(thisMonth().getMonthValue());
        assertThat(countMonthly(fixture.member(), 2030, 1)).isZero();
    }

    /** 응답 본문의 {@code resCode}. <b>이 앱은 실패도 HTTP 200</b>이라 본문을 봐야 한다. */
    private int codeOf(MockHttpServletResponse response) throws Exception {
        return resCode(objectMapper.readTree(
                response.getContentAsString(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("#38 Path 에 연·월을 붙인 경로는 존재하지 않는다")
    void thereIsNoPathVariant() throws Exception {
        Fixture fixture = prepare();

        MockHttpServletResponse response = call("POST",
                SYNC_URL + "/" + thisMonth().getYear() + "/" + thisMonth().getMonthValue(),
                fixture.token(), null);

        // 성공하면 두 번째 입력 경로가 생긴 것이다. HTTP 상태가 아니라 resCode 로 본다 —
        // 이 앱은 실패도 HTTP 200 으로 돌려주기 때문이다(헌장 원칙 III).
        assertThat(codeOf(response)).isNotEqualTo(200);
        assertThat(countMonthly(fixture.member(), thisMonth().getYear(),
                thisMonth().getMonthValue())).isZero();
    }

    @Test
    @DisplayName("#38 GET·PATCH·PUT 으로는 부를 수 없다 — POST 전용이다")
    void onlyPostIsAllowed() throws Exception {
        Fixture fixture = prepare();
        String body = """
                {"year":%d,"month":%d}
                """.formatted(thisMonth().getYear(), thisMonth().getMonthValue());

        for (String method : new String[] {"GET", "PATCH", "PUT"}) {
            // 메서드 불일치는 GlobalExceptionHandler 가 9001 로 접는다.
            assertThat(codeOf(call(method, SYNC_URL, fixture.token(), body)))
                    .as("%s /sync", method)
                    .isEqualTo(9001);
        }
        // 거절됐으니 아무 행도 만들어지지 않았다.
        assertThat(countMonthly(fixture.member(), thisMonth().getYear(),
                thisMonth().getMonthValue())).isZero();
    }

    @Test
    @DisplayName("PUT 매핑이 없다 — 헌장 원칙 III(PUT 을 쓰지 않는다)")
    void putIsNeverUsed() throws Exception {
        Fixture fixture = prepare();

        // 200 이면 PUT 매핑이 생긴 것이다. 전 API 를 훑는 시험은 OpenApiDocumentIT 가 한다.
        assertThat(codeOf(call("PUT", SYNC_URL, fixture.token(), "{}"))).isEqualTo(9001);
    }

    @Test
    @DisplayName("Body 가 아예 없으면 9001 이다 — 형식 오류다")
    void missingBodyIsARequestFormatError() throws Exception {
        Fixture fixture = prepare();

        // 연·월 범위 오류(3403)가 아니라 요청 자체가 성립하지 않는다.
        assertThat(codeOf(call("POST", SYNC_URL, fixture.token(), null))).isEqualTo(9001);
    }
}

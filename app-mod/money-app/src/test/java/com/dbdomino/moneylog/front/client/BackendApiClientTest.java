package com.dbdomino.moneylog.front.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dbdomino.moneylog.front.web.Paging;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 봉투를 푸는 자리가 이 클래스 하나라는 것을 고정한다.
 *
 * <p>다섯 갈래를 건다 — 성공 봉투 · 실패 봉투 · 연결 실패 · 봉투가 아닌 본문 · 파일 본문.
 * 이 다섯이 화면 31개가 만날 수 있는 응답 전부다.
 */
class BackendApiClientTest {

    private static final String BASE_URL = "http://backend.test/api/v1";

    private MockRestServiceServer server;
    private BackendApiClient client;

    /** 봉투 안의 값이 호출부 타입으로 오는지 보기 위한 최소 타입. */
    record PaymentMethod(Long idx, String name) {
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        ObjectMapper objectMapper = JsonMapper.builder().build();
        client = new BackendApiClient(builder.build(), objectMapper);
    }

    @Test
    @DisplayName("성공 봉투는 data 안의 값을 그대로 돌려준다")
    void 성공_봉투는_값을_돌려준다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"idx":7,"name":"국민카드"}}""",
                        MediaType.APPLICATION_JSON));

        PaymentMethod result = client.get("/payment-methods/{idx}", PaymentMethod.class, 7);

        assertThat(result.idx()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("국민카드");
        server.verify();
    }

    @Test
    @DisplayName("실패 봉투의 코드는 바뀌지 않고 그대로 올라온다")
    void 실패_봉투는_코드를_그대로_올린다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andRespond(withSuccess("""
                        {"resCode":3003,"data":{"message":"사용할 수 없는 수단입니다."}}""",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.get("/payment-methods/{idx}", PaymentMethod.class, 7))
                .isInstanceOf(BackendApiException.class)
                .hasMessage("사용할 수 없는 수단입니다.")
                .extracting(e -> ((BackendApiException) e).getResCode())
                .isEqualTo(3003);
        server.verify();
    }

    @Test
    @DisplayName("연결하지 못하면 코드 없는 미도달 실패가 된다")
    void 연결_실패는_미도달이다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andRespond(withException(new ConnectException("connection refused")));

        assertThatThrownBy(() -> client.get("/payment-methods/{idx}", PaymentMethod.class, 7))
                .isInstanceOf(BackendUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("봉투가 아닌 본문도 미도달로 본다 — 성공으로 읽지 않는다")
    void 봉투가_아니면_미도달이다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andRespond(withSuccess("<html><body>Gateway Timeout</body></html>",
                        MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.get("/payment-methods/{idx}", PaymentMethod.class, 7))
                .isInstanceOf(BackendUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("resCode 가 없는 JSON 도 봉투가 아니다")
    void resCode_가_없으면_미도달이다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andRespond(withSuccess("{\"idx\":7}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.get("/payment-methods/{idx}", PaymentMethod.class, 7))
                .isInstanceOf(BackendUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("파일 본문은 봉투 해석을 거치지 않고 바이트 그대로 온다")
    void 파일_본문은_바이트로_온다() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        server.expect(requestTo(BASE_URL + "/expend-groups/icons/1_2.png"))
                .andRespond(withSuccess(png, MediaType.IMAGE_PNG));

        BinaryPayload payload = client.getBinary("/expend-groups/icons/{filename}", "1_2.png");

        assertThat(payload.bytes()).isEqualTo(png);
        assertThat(payload.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        server.verify();
    }

    @Test
    @DisplayName("파일을 기다린 호출에 실패 봉투가 오면 그 코드로 실패한다")
    void 파일_호출에_실패_봉투가_오면_실패다() {
        server.expect(requestTo(BASE_URL + "/expend-groups/icons/1_2.png"))
                .andRespond(withSuccess("""
                        {"resCode":1001,"data":{"message":"로그인이 필요합니다."}}""",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getBinary("/expend-groups/icons/{filename}", "1_2.png"))
                .isInstanceOf(BackendApiException.class)
                .extracting(e -> ((BackendApiException) e).getResCode())
                .isEqualTo(1001);
        server.verify();
    }

    @Test
    @DisplayName("목록 조회는 Query 로만 나간다")
    void 목록_조회는_Query_로_나간다() {
        // Query 는 순서를 보장하지 않으므로 경로와 값을 따로 본다.
        server.expect(requestTo(startsWith(BASE_URL + "/payment-methods?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("offset", "20"))
                .andExpect(queryParam("limit", "10"))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"idx":1,"name":"목록"}}""",
                        MediaType.APPLICATION_JSON));

        client.getByQuery("/payment-methods", Paging.of(2, 10).toQuery(), PaymentMethod.class);

        server.verify();
    }

    @Test
    @DisplayName("PUT 은 부를 방법 자체가 없다")
    void PUT_메서드가_없다() {
        boolean hasPut = Arrays.stream(BackendApiClient.class.getMethods())
                .map(Method::getName)
                .anyMatch(name -> name.equalsIgnoreCase("put"));

        assertThat(hasPut)
                .as("규칙을 검증으로 세는 대신 부를 수 없게 한다")
                .isFalse();
    }

    @Test
    @DisplayName("Path 용 조회에 Query 를 직접 붙이면 거부한다")
    void Path_조회에_Query_를_붙이면_거부한다() {
        assertThatThrownBy(() -> client.get("/payment-methods?offset=0", PaymentMethod.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Query 용 조회에 경로 자리표시자를 쓰면 거부한다")
    void Query_조회에_자리표시자를_쓰면_거부한다() {
        assertThatThrownBy(() -> client.getByQuery("/payment-methods/{idx}", Map.of(), PaymentMethod.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** {@link IOException} 을 던지는 응답도 미도달로 묶인다는 것을 문서 대신 시험으로 남긴다. */
    @Test
    @DisplayName("읽기 도중 끊겨도 미도달이다")
    void 읽기_실패도_미도달이다() {
        server.expect(requestTo(BASE_URL + "/payment-methods/7"))
                .andRespond(withException(new IOException("stream closed")));

        assertThatThrownBy(() -> client.get("/payment-methods/{idx}", PaymentMethod.class, 7))
                .isInstanceOf(BackendUnavailableException.class);
    }
}

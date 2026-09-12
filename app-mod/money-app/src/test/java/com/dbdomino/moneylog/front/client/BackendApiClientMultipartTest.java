package com.dbdomino.moneylog.front.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dbdomino.moneylog.front.session.LoginSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 파일을 실은 <b>수정</b> 통로. 009 가 007 의 호출 통로에 더한 자리다.
 *
 * <p>007 은 파일을 생성에만 실을 것으로 보고 생성용만 두었는데, 백엔드 지출유형 수정이
 * 처음부터 그 조합이었다. 009 가 자기 호출 코드를 만들지 않고 여기 더한 이유는 <b>봉투를
 * 푸는 자리를 하나로 유지</b>하기 위해서다.
 *
 * <p>시험을 007 의 기존 시험 파일에 끼워 넣지 않고 새 파일로 둔다 — 007 이 남긴 시험은
 * 그대로 두고 009 가 더한 것만 여기 모은다.
 */
class BackendApiClientMultipartTest {

    private static final String BASE_URL = BackendClientFixture.BASE_URL;

    private MockRestServiceServer server;
    private BackendApiClient client;

    /** 봉투 안의 값이 호출부 타입으로 오는지 보기 위한 최소 타입. */
    record ExpendGroup(Long expendGroupId, String name) {
    }

    @BeforeEach
    void setUp() {
        BackendClientFixture.bindRequest();
        RestClient.Builder builder = BackendClientFixture.builder();
        server = BackendClientFixture.bindServer(builder);
        client = BackendClientFixture.client(builder, new LoginSession(), () -> null);
    }

    @AfterEach
    void tearDown() {
        BackendClientFixture.unbindRequest();
    }

    @Test
    @DisplayName("파일을 실은 수정이 PATCH 와 multipart 로 나간다")
    void 파일을_실은_수정이_나간다() {
        server.expect(requestTo(BASE_URL + "/expend-groups/7"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.CONTENT_TYPE,
                        org.hamcrest.Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"expendGroupId":7,"name":"식비"}}""",
                        MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("name", "식비");

        ExpendGroup result = client.patchMultipart("/expend-groups/{id}", parts,
                ExpendGroup.class, 7);

        assertThat(result.expendGroupId()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("식비");
        server.verify();
    }

    @Test
    @DisplayName("담지 않은 칸은 본문에 나가지 않는다")
    void 담지_않은_칸은_나가지_않는다() {
        // 파일 칸을 빼면 기존 파일이 남는다. 빈 칸을 담으면 0바이트 파일을 올리는 요청이 되어
        // 형식 오류로 거절되고, 사용자에게는 "이름만 고쳤는데 아이콘이 잘못됐다"로 보인다.
        server.expect(requestTo(BASE_URL + "/expend-groups/7"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("iconFile"))))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"expendGroupId":7,"name":"식비"}}""",
                        MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("name", "식비");

        client.patchMultipart("/expend-groups/{id}", parts, ExpendGroup.class, 7);

        server.verify();
    }

    @Test
    @DisplayName("실패 봉투는 코드를 그대로 올린다")
    void 실패_봉투가_코드를_올린다() {
        server.expect(requestTo(BASE_URL + "/expend-groups/7"))
                .andRespond(withSuccess("""
                        {"resCode":3102,"data":{"message":"아이콘 파일의 형식 또는 크기가 올바르지 않습니다."}}""",
                        MediaType.APPLICATION_JSON));

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("name", "식비");

        // 봉투를 푸는 자리가 하나라는 것이 이 통로를 007 에 더한 이유다. 화면이 자기 호출
        // 코드를 만들었다면 이 확인을 빠뜨릴 수 있었다.
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> client.patchMultipart("/expend-groups/{id}", parts,
                        ExpendGroup.class, 7))
                .isInstanceOf(BackendApiException.class)
                .satisfies(e -> assertThat(((BackendApiException) e).getResCode()).isEqualTo(3102));
    }

    @Test
    @DisplayName("경로에 Query 를 직접 붙이면 거절한다")
    void 경로에_Query_를_붙이지_않는다() {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        // 007 이 다른 메서드에 건 규칙과 같다. 호출 규칙을 검증이 아니라 형태로 막는다.
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> client.patchMultipart("/expend-groups?x=1", parts,
                        ExpendGroup.class))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.dbdomino.moneylog.front.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendClientFixture;
import com.dbdomino.moneylog.front.session.LoginSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 빈 칸의 두 뜻이 <b>실제로 나가는 본문에서</b> 갈리는지 본다.
 *
 * <p>지도에 키를 넣고 빼는 것만으로는 부족하다 — 직렬화가 {@code null} 값을 걸러내 버리면
 * "비워 달라"가 조용히 "그대로 두라"가 되고, 사용자는 이메일을 지우고 저장했는데 그대로
 * 남아 있는 화면을 보게 된다. 그 실패는 화면에도 로그에도 흔적이 없어 오래 간다.
 *
 * <p>그래서 지도가 아니라 <b>나간 JSON</b> 을 확인한다.
 */
class PatchBodyTest {

    private MockRestServiceServer server;
    private BackendApiClient client;

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
    @DisplayName("비운 칸은 null 로 나가고 건드리지 않은 칸은 아예 나가지 않는다")
    void 비움과_유지가_본문에서_갈린다() {
        server.expect(requestTo(BackendClientFixture.BASE_URL + "/members/me"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("""
                        {"nickname":"홍길동","email":null,"phone":null,"intro":null}""", true))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"memberId":"hong"}}""",
                        MediaType.APPLICATION_JSON));

        // 닉네임만 남기고 나머지를 비운 뒤, 새 비밀번호는 건드리지 않은 상태.
        client.patch("/members/me", new PatchBody()
                .always("nickname", "홍길동")
                .clearIfBlank("email", "")
                .phoneClearIfBlank("phone", "   ")
                .clearIfBlank("intro", null)
                .omitIfBlank("password", "")
                .toMap(), Void.class);

        // 엄격 비교라 password 키가 섞여 있으면 위에서 이미 실패한다.
        server.verify();
    }

    @Test
    @DisplayName("값을 넣은 칸은 그 값 그대로 나가고 폰은 숫자만 남는다")
    void 값이_있으면_그대로_나간다() {
        server.expect(requestTo(BackendClientFixture.BASE_URL + "/members/me"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("""
                        {"nickname":"홍길동","email":"hong@example.com","phone":"01012345678",\
                        "password":"NewPass1!"}""", true))
                .andRespond(withSuccess("""
                        {"resCode":200,"data":{"memberId":"hong"}}""",
                        MediaType.APPLICATION_JSON));

        client.patch("/members/me", new PatchBody()
                .always("nickname", "홍길동")
                .clearIfBlank("email", "hong@example.com")
                .phoneClearIfBlank("phone", "010-1234-5678")
                .omitIfBlank("password", "NewPass1!")
                .toMap(), Void.class);

        server.verify();
    }

    @Test
    @DisplayName("지도는 바꿀 수 없다")
    void 지도를_바꿀_수_없다() {
        // 만들어진 본문을 호출 직전에 누가 고치면 화면이 보낸 것과 나간 것이 달라진다.
        var body = new PatchBody().always("nickname", "홍길동").toMap();

        assertThat(body).containsEntry("nickname", "홍길동");
        assertThat(body.getClass().getSimpleName()).contains("Unmodifiable");
    }
}

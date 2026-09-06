package com.dbdomino.moneylog.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * US4 — 관리자 API 의 인가.
 *
 * <p>quickstart.md §3 의 시나리오 #21·#38 에 대응한다(SC-106).
 */
class AdminAuthorizationIT extends AbstractApiIT {

    @Test
    @DisplayName("#21 일반 회원 토큰으로 관리자 API 5건을 부르면 전부 1002 다 (SC-106)")
    void memberTokenIsRejectedFromEveryAdminApi() throws Exception {
        User member = createMember();
        Tokens tokens = login(member);
        String accessToken = tokens.accessToken();

        assertThat(resCode(postJson("/api/v1/admin/members", accessToken, """
                {"memberId":"ittestnew1","password":"Test1234!","nickname":"새회원","role":3}
                """))).as("1.12 회원 추가").isEqualTo(1002);
        assertThat(resCode(getJson("/api/v1/admin/members?offset=0&limit=10", accessToken)))
                .as("1.13 목록").isEqualTo(1002);
        assertThat(resCode(getJson("/api/v1/admin/members/" + member.getUserId(), accessToken)))
                .as("1.14 상세").isEqualTo(1002);
        assertThat(resCode(patch("/api/v1/admin/members/" + member.getUserId(), accessToken, """
                {"nickname":"바뀜"}
                """))).as("1.15 수정").isEqualTo(1002);
        assertThat(resCode(patch(
                "/api/v1/admin/members/" + member.getUserId() + "/deactivate", accessToken, null)))
                .as("1.16 정지").isEqualTo(1002);
    }

    @Test
    @DisplayName("#38 인가 실패 응답도 { resCode, data } 규격이다 — Spring 기본 403 JSON 이 아니다")
    void accessDeniedFollowsResponseContract() throws Exception {
        User member = createMember();
        Tokens tokens = login(member);

        var result = mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/admin/members?offset=0&limit=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andReturn().getResponse();

        // 비즈니스 실패와 같은 규칙이다 — 상태 코드가 아니라 resCode 로 가른다.
        assertThat(result.getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(
                result.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(body.get("resCode").asInt()).isEqualTo(1002);
        assertThat(body.get("data").get("message").asString()).isNotBlank();
    }

    @Test
    @DisplayName("#21 관리자 토큰으로는 같은 API 가 통과한다")
    void adminTokenPasses() throws Exception {
        User admin = createAdmin();
        Tokens tokens = login(admin);

        assertThat(resCode(getJson("/api/v1/admin/members?offset=0&limit=10", tokens.accessToken())))
                .isEqualTo(200);
    }

    private JsonNode patch(String url, String accessToken, String body) throws Exception {
        var request = MockMvcRequestBuilders.patch(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.content(body);
        }
        String response = mockMvc.perform(request).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }
}

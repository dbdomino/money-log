package com.dbdomino.moneylog.backend.member;

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
 * US2 — 본인 정보 조회(1.7)·수정(1.8).
 *
 * <p>quickstart.md §3 의 시나리오 #14·#15 에 대응한다(SC-107 · PATCH omit 규칙).
 */
class MemberMeIT extends AbstractApiIT {

    @Test
    @DisplayName("#14 본인 정보 조회 응답에 비밀번호가 없다 (SC-107)")
    void getMeNeverExposesPassword() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);

        JsonNode response = getJson("/api/v1/members/me", tokens.accessToken());

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("memberId").asString()).isEqualTo(user.getUserId());
        assertThat(data.get("nickname").asString()).isEqualTo(user.getNickname());
        assertThat(data.get("role").asInt()).isEqualTo(User.ROLE_MEMBER);
        // 응답 어디에도 비밀번호 해시가 없다. 타입에 필드 자체가 없어서다.
        assertThat(data.has("pw")).isFalse();
        assertThat(data.has("password")).isFalse();
        assertThat(response.toString()).doesNotContain(user.getPw());
    }

    @Test
    @DisplayName("#15 omit 한 필드는 유지되고 보낸 필드만 바뀐다")
    void omittedFieldsStayUnchanged() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        patch(tokens, """
                {"email":"before@example.com","intro":"처음 소개","phone":"01011112222"}
                """);

        JsonNode response = patch(tokens, """
                {"nickname":"바뀐닉네임"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("nickname").asString()).isEqualTo("바뀐닉네임");
        // 보내지 않은 셋은 그대로여야 한다.
        assertThat(data.get("email").asString()).isEqualTo("before@example.com");
        assertThat(data.get("intro").asString()).isEqualTo("처음 소개");
        assertThat(data.get("phone").asString()).isEqualTo("01011112222");
    }

    @Test
    @DisplayName("#15 null 을 보내면 값을 지운다 — omit 과 다르게 동작한다")
    void explicitNullClearsTheField() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        patch(tokens, """
                {"email":"clear@example.com","intro":"지워질 소개"}
                """);

        JsonNode response = patch(tokens, """
                {"email":null}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        // 이 구분이 없으면 "이메일을 지우는" 조작이 아예 불가능해진다.
        assertThat(data.get("email").isNull()).isTrue();
        assertThat(data.get("intro").asString()).isEqualTo("지워질 소개");
    }

    @Test
    @DisplayName("#15 비밀번호를 바꾸면 활성 세션이 폐기된다 (FR-119)")
    void changingPasswordRevokesActiveSession() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);

        JsonNode response = patch(tokens, """
                {"password":"NewPass1!"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(countActiveSessions(user)).isZero();
        // 바꾸기 전에 쓰던 토큰은 더 이상 통하지 않는다.
        assertThat(resCode(getJson("/api/v1/members/me", tokens.accessToken()))).isEqualTo(1006);
    }

    @Test
    @DisplayName("#15 규칙을 어긴 새 비밀번호는 2004 다")
    void weakNewPasswordIsRejected() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);

        JsonNode response = patch(tokens, """
                {"password":"abcdefghij"}
                """);

        assertThat(resCode(response)).isEqualTo(2004);
        // 거절됐으니 세션도 그대로여야 한다.
        assertThat(countActiveSessions(user)).isEqualTo(1);
    }

    @Test
    @DisplayName("#14 토큰 없이 부르면 1001 이다")
    void anonymousRequestIsRejected() throws Exception {
        assertThat(resCode(getJson("/api/v1/members/me", null))).isEqualTo(1001);
    }

    private JsonNode patch(Tokens tokens, String body) throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }
}

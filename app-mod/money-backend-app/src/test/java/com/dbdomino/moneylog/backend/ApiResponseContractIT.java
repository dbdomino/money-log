package com.dbdomino.moneylog.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 응답 규격 전수 확인 — API 16건이 모두 {@code { resCode, data }} 로 답하는가.
 *
 * <p>quickstart.md §3 의 시나리오 #35~#38 에 대응한다(SC-101).
 *
 * <p>개별 API 의 동작은 각 스토리의 테스트가 본다. 여기서 보는 것은 <b>형식</b> 하나다 —
 * 성공이든 실패든 HTTP 200 에 {@code resCode} 와 {@code data} 가 있어야 하고,
 * 서버 오류만 500 이다. 이 성질이 깨지면 클라이언트는 API 마다 다른 파싱을 갖게 된다.
 */
class ApiResponseContractIT extends AbstractApiIT {

    @Test
    @DisplayName("#35 API 16건 전부 HTTP 200 + resCode + data 로 답한다 (SC-101)")
    void everyEndpointFollowsTheResponseContract() throws Exception {
        User member = createMember();
        User admin = createAdmin();
        String memberToken = login(member).accessToken();
        String adminToken = login(admin).accessToken();
        String newId = TEST_USER_PREFIX + "contract1";

        List<Call> calls = List.of(
                new Call("1.1 헬스체크", HttpMethod.GET, "/api/v1/ha", null, null),
                new Call("1.2 가입", HttpMethod.POST, "/api/v1/auth/signup", null, """
                        {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"규격확인"}
                        """.formatted(newId, TEST_PASSWORD, TEST_PASSWORD)),
                new Call("1.3 로그인", HttpMethod.POST, "/api/v1/auth/login", null, """
                        {"memberId":"%s","password":"%s"}
                        """.formatted(member.getUserId(), TEST_PASSWORD)),
                new Call("1.4 토큰 검증", HttpMethod.GET, "/api/v1/auth/validate", memberToken, null),
                new Call("1.5 토큰 갱신", HttpMethod.POST, "/api/v1/auth/refresh", null, """
                        {"refreshToken":"없는토큰"}
                        """),
                new Call("1.6 로그아웃", HttpMethod.POST, "/api/v1/auth/revoke", memberToken, null),
                new Call("1.7 본인 조회", HttpMethod.GET, "/api/v1/members/me", memberToken, null),
                new Call("1.8 본인 수정", HttpMethod.PATCH, "/api/v1/members/me", memberToken, """
                        {"nickname":"규격확인"}
                        """),
                new Call("1.9 아이디 찾기", HttpMethod.POST, "/api/v1/auth/find-id", null, """
                        {"email":"none@example.com"}
                        """),
                new Call("1.10 비밀번호 찾기", HttpMethod.POST, "/api/v1/auth/find-password", null, """
                        {"memberId":"%s","nickname":"%s"}
                        """.formatted(member.getUserId(), member.getNickname())),
                new Call("1.11 비밀번호 재설정", HttpMethod.POST, "/api/v1/auth/reset-password", null, """
                        {"memberId":"%s","nickname":"%s","newPassword":"Reset1234!","newPasswordConfirm":"Reset1234!"}
                        """.formatted(member.getUserId(), member.getNickname())),
                new Call("1.12 관리자 추가", HttpMethod.POST, "/api/v1/admin/members", adminToken, """
                        {"memberId":"%s","password":"%s","nickname":"규격확인","role":3}
                        """.formatted(TEST_USER_PREFIX + "contract2", TEST_PASSWORD)),
                new Call("1.13 관리자 목록", HttpMethod.GET,
                        "/api/v1/admin/members?offset=0&limit=10", adminToken, null),
                new Call("1.14 관리자 상세", HttpMethod.GET,
                        "/api/v1/admin/members/" + member.getUserId(), adminToken, null),
                new Call("1.15 관리자 수정", HttpMethod.PATCH,
                        "/api/v1/admin/members/" + member.getUserId(), adminToken, """
                        {"nickname":"관리자수정"}
                        """),
                new Call("1.16 관리자 정지", HttpMethod.PATCH,
                        "/api/v1/admin/members/" + member.getUserId() + "/deactivate", adminToken, null));

        assertThat(calls).hasSize(16);
        for (Call call : calls) {
            MockHttpServletResponse response = perform(call);
            assertThat(response.getStatus()).as("%s 는 HTTP 200 이어야 한다", call.label()).isEqualTo(200);

            JsonNode body = objectMapper.readTree(
                    response.getContentAsString(StandardCharsets.UTF_8));
            assertThat(body.has("resCode")).as("%s 응답에 resCode 가 있어야 한다", call.label()).isTrue();
            assertThat(body.has("data")).as("%s 응답에 data 가 있어야 한다", call.label()).isTrue();
            int resCode = body.get("resCode").asInt();
            // 성공은 200, 실패는 네 자리다. 그 사이 값이 나오면 규격을 벗어난 것이다.
            assertThat(resCode == 200 || (resCode >= 1000 && resCode <= 9999))
                    .as("%s 의 resCode(%d)는 200 이거나 4자리여야 한다", call.label(), resCode)
                    .isTrue();
            // 서버 오류가 섞이면 안 된다 — 이 호출들은 전부 정상 경로이거나 예상된 실패다.
            assertThat(resCode).as("%s 에서 서버 오류가 났다", call.label()).isNotEqualTo(9000);
        }
    }

    @Test
    @DisplayName("#36 비즈니스 실패도 HTTP 200 + 4자리 resCode 다 — 상태 코드로 분기하지 않는다")
    void businessFailureUsesHttp200() throws Exception {
        User existing = createMember();

        MockHttpServletResponse response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"중복"}
                                        """.formatted(existing.getUserId(), TEST_PASSWORD, TEST_PASSWORD)))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("resCode").asInt()).isEqualTo(2002);
        assertThat(body.get("data").get("message").asString()).isNotBlank();
    }

    private MockHttpServletResponse perform(Call call) throws Exception {
        // HttpMethod 는 Spring 6 부터 enum 이 아니라 클래스라 switch 로 가르지 않는다.
        MockHttpServletRequestBuilder request;
        if (HttpMethod.GET.equals(call.method())) {
            request = MockMvcRequestBuilders.get(call.url());
        } else if (HttpMethod.POST.equals(call.method())) {
            request = MockMvcRequestBuilders.post(call.url());
        } else if (HttpMethod.PATCH.equals(call.method())) {
            request = MockMvcRequestBuilders.patch(call.url());
        } else {
            // PUT·DELETE 는 이 기능에서 쓰지 않는다(헌장 원칙 III).
            throw new IllegalArgumentException("쓰지 않는 메서드: " + call.method());
        }
        request.contentType(MediaType.APPLICATION_JSON);
        if (call.accessToken() != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + call.accessToken());
        }
        if (call.body() != null) {
            request.content(call.body());
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    /** 호출 1건. {@code label} 은 실패했을 때 어느 API 인지 바로 보이라고 둔다. */
    private record Call(String label, HttpMethod method, String url, String accessToken,
                        String body) {
    }
}

package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 지출유형 통합 테스트의 공통 바탕.
 *
 * <h2>가입 API 로 회원을 만든다</h2>
 *
 * <p>{@code createMember()} 는 Repository 로 회원 행만 만들어 <b>기본 지출유형 10종이 생기지
 * 않는다</b>. 기본 유형은 {@code 3105}(이름 변경 불가)·{@code 3107}(삭제 불가) 판정의 대상이라
 * 없으면 그 시나리오를 세울 수 없다.
 *
 * <h2>2.7·2.11 은 multipart 다</h2>
 *
 * <p>아이콘을 파트로 받기 때문에 JSON 이 아니다. 아이콘을 올리지 않는 요청도 같은 형식으로
 * 보내야 하므로 여기서 폼 필드만 싣는 헬퍼를 둔다.
 *
 * <p>PATCH 는 {@code multipart(...)} 뒤에 메서드를 바꿔 넣는다 — 서블릿 규격상 multipart 는
 * POST 를 전제하므로 그 한 줄이 없으면 파트가 파싱되지 않는다.
 */
abstract class AbstractExpendGroupIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/expend-groups";

    /** 가입한 회원. 아이디를 함께 들고 다녀야 그 회원의 행만 골라 볼 수 있다. */
    protected record Member(String memberId, String token) {
    }

    /** 가입하고 로그인한다. 기본 지출유형 10종과 아이콘이 함께 생긴다. */
    protected Member signupAndLogin() throws Exception {
        String memberId = TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        JsonNode signup = postJson("/api/v1/auth/signup", """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"테스트회원"}
                """.formatted(memberId, TEST_PASSWORD, TEST_PASSWORD));
        assertThat(resCode(signup)).isEqualTo(200);

        JsonNode login = postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(memberId, TEST_PASSWORD));
        assertThat(resCode(login)).isEqualTo(200);
        return new Member(memberId, login.get("data").get("accessToken").asString());
    }

    /** 2.7 등록. 아이콘 없이 폼 필드만 보낸다. */
    protected JsonNode createGroup(String token, String name, Boolean inUse) throws Exception {
        return createGroup(token, name, inUse, null);
    }

    /** 2.7 등록. */
    protected JsonNode createGroup(String token, String name, Boolean inUse,
                                   MockMultipartFile iconFile) throws Exception {
        return send(MockMvcRequestBuilders.multipart(URL), token, name, inUse, iconFile);
    }

    /** 2.11 수정. {@code null} 인 항목은 파트를 만들지 않는다(omit = 기존 값 유지). */
    protected JsonNode updateGroup(String token, long expendGroupId, String name, Boolean inUse)
            throws Exception {
        return updateGroup(token, expendGroupId, name, inUse, null);
    }

    /** 2.11 수정. */
    protected JsonNode updateGroup(String token, long expendGroupId, String name, Boolean inUse,
                                   MockMultipartFile iconFile) throws Exception {
        MockMultipartHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(URL + "/" + expendGroupId);
        request.with(servletRequest -> {
            servletRequest.setMethod("PATCH");
            return servletRequest;
        });
        return send(request, token, name, inUse, iconFile);
    }

    /** 폼 필드와 선택 파일을 실어 보낸다. */
    private JsonNode send(MockMultipartHttpServletRequestBuilder request, String token,
                          String name, Boolean inUse, MockMultipartFile iconFile)
            throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (name != null) {
            request.part(new MockPart("name", name.getBytes(StandardCharsets.UTF_8)));
        }
        if (inUse != null) {
            request.part(new MockPart("inUse",
                    String.valueOf(inUse).getBytes(StandardCharsets.UTF_8)));
        }
        if (iconFile != null) {
            request.file(iconFile);
        }
        String response = mockMvc.perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** 등록에 성공했다고 보고 그 PK 를 꺼낸다. */
    protected long idOf(JsonNode response) {
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("expendGroupId").asLong();
    }

    /** 그 회원의 기본 유형 하나(이름으로 고른다)의 PK. */
    protected long defaultGroupId(Member member, String name) {
        return jdbc.queryForObject("""
                select g.idx from moneylog.tbl_user_expend_group g
                  join moneylog.tbl_user u on u.id_key = g.id_key
                 where u.user_id = ? and g.name = ?
                """, Long.class, member.memberId(), name);
    }
}

package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 지출유형 통합 테스트의 공통 바탕.
 *
 * <h2>가입 헬퍼는 {@link AbstractApiIT} 에 있다</h2>
 *
 * <p>{@code signupAndLogin()}·{@code Member}·{@code defaultGroupId(...)} 는 004(지출·소득)의
 * 네 테스트 패키지도 필요로 해 상위로 올렸다. 여기서 다시 정의하지 않는다 — 복제하면
 * 가입 절차가 두 곳에서 갈린다.
 *
 * <p>{@code createMember()} 를 쓰지 않는 이유는 그대로다. 그쪽은 Repository 로 회원 행만
 * 만들어 <b>기본 지출유형 10종이 생기지 않는데</b>, 기본 유형은 {@code 3105}(이름 변경
 * 불가)·{@code 3107}(삭제 불가) 판정의 대상이라 없으면 그 시나리오를 세울 수 없다.
 *
 * <h2>2.7·2.11 은 multipart 다</h2>
 *
 * <p>아이콘을 파트로 받기 때문에 JSON 이 아니다. 아이콘을 올리지 않는 요청도 같은 형식으로
 * 보내야 하므로 여기서 폼 필드만 싣는 헬퍼를 둔다.
 *
 * <p>PATCH 는 {@code multipart(...)} 뒤에 메서드를 바꿔 넣는다 — 서블릿 규격상 multipart 는
 * POST 를 전제하므로 그 한 줄이 없으면 파트가 파싱되지 않는다.
 */
public abstract class AbstractExpendGroupIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/expend-groups";

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
}

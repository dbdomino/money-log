package com.dbdomino.moneylog.backend.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dbdomino.moneylog.backend.expendgroup.AbstractExpendGroupIT;
import com.dbdomino.moneylog.backend.service.ExpendGroupIconService;
import com.dbdomino.moneylog.common.error.BusinessException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 2.10 아이콘 조회 — quickstart #30·#31·#32·#36.
 *
 * <p><b>세 갈래 응답을 함께 본다.</b> 하나라도 어긋나면 이 API 의 계약이 깨진 것이다
 * (icon-storage.md §3).
 *
 * <table border="1">
 *   <caption>2.10 의 응답</caption>
 *   <tr><th>상황</th><th>HTTP</th><th>본문</th></tr>
 *   <tr><td>성공</td><td>200</td><td>이미지 바이너리</td></tr>
 *   <tr><td>파일 없음</td><td>200</td><td>{@code { resCode: 3104, ... }} 래퍼</td></tr>
 *   <tr><td>인증 실패</td><td>401</td><td>없음</td></tr>
 * </table>
 *
 * <p>이 클래스는 {@code resCode} 만 보지 않고 <b>실제 응답 헤더</b>를 확인한다 —
 * {@code GlobalExceptionHandler} 나 {@code produces} 협상이 {@code Content-Type} 을
 * 덮어쓰면 그 자리에서만 드러난다(T045 의 완료 기준).
 */
class IconDownloadIT extends AbstractExpendGroupIT {

    private static final String ICON_URL = "/api/v1/expend-groups/icons/";

    @Autowired
    private ExpendGroupIconService iconService;

    /** 아이콘을 올린 유형을 만들고 저장된 파일명을 돌려준다. */
    private String uploadIcon(String token, String name) throws Exception {
        long id = idOf(createGroup(token, name, true,
                IconTestImages.part("hobby.png", MediaType.IMAGE_PNG_VALUE, IconTestImages.png())));
        return jdbc.queryForObject(
                "select icon_filename from moneylog.tbl_user_expend_group where idx = ?",
                String.class, id);
    }

    private MockHttpServletResponse fetch(String filename, String token) throws Exception {
        var request = MockMvcRequestBuilders.get(ICON_URL + filename);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    @Test
    @DisplayName("#31 성공은 래퍼가 아니라 이미지 바이너리다")
    void successIsBinaryNotWrapper() throws Exception {
        String token = signupAndLogin().token();
        String filename = uploadIcon(token, "취미");

        MockHttpServletResponse response = fetch(filename, token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(response.getContentAsByteArray())
                .isNotEmpty()
                .isEqualTo(IconTestImages.png());
        // 래퍼가 섞여 나오면 본문이 JSON 이 된다.
        assertThat(response.getContentAsString(StandardCharsets.ISO_8859_1))
                .doesNotContain("resCode");
    }

    @Test
    @DisplayName("#30 Bearer 없이 부르면 래퍼 없는 HTTP 401 이다")
    void withoutTokenIsEmpty401() throws Exception {
        String token = signupAndLogin().token();
        String filename = uploadIcon(token, "취미");

        MockHttpServletResponse response = fetch(filename, null);

        // 다른 API 는 HTTP 200 + { resCode: 1001 } 이다. 이 하나만 다르다 —
        // 4자리 코드를 실을 JSON 본문 자체가 없는 API 이기 때문이다.
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    @DisplayName("#32 없는 파일명은 3104 이고 래퍼를 쓴다")
    void missingFileIsWrapped3104() throws Exception {
        String token = signupAndLogin().token();

        MockHttpServletResponse response = fetch("999999_999999.png", token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode body = objectMapper.readTree(
                response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(resCode(body)).isEqualTo(3104);
        assertThat(body.get("data").get("message").asString()).isNotBlank();
    }

    @Test
    @DisplayName("이미지가 아닌 확장자를 물어도 3104 다 — 형식을 말할 수 없는 바이트를 내보내지 않는다")
    void unservableExtensionIs3104() throws Exception {
        String token = signupAndLogin().token();

        MockHttpServletResponse response = fetch("1_1.txt", token);

        JsonNode body = objectMapper.readTree(
                response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(resCode(body)).isEqualTo(3104);
    }

    @Test
    @DisplayName("#36 filename 에 .. 을 넣어도 저장 루트 밖을 읽지 않는다")
    void pathTraversalIsRefused() {
        // HTTP 계층은 정규화·방화벽이 먼저 걸러 무엇이 나올지 환경에 달렸다. 방어의 실체는
        // 저장소 경로 정규화이므로 그 자리를 직접 부른다 — 파일명은 서버가 만들지만
        // 조회 요청의 값은 클라이언트가 보내므로 믿을 수 없다(icon-storage.md §3 주의 4).
        assertThatThrownBy(() -> iconService.read("../../../etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("아이콘");

        assertThatThrownBy(() -> iconService.read("../../../etc/passwd.png"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("남의 아이콘 파일명을 알면 받을 수 있다 — 현재 계약이 그렇다")
    void anyLoggedInMemberCanFetchAnyFilename() throws Exception {
        String ownerToken = signupAndLogin().token();
        String filename = uploadIcon(ownerToken, "취미");
        String otherToken = signupAndLogin().token();

        // 파일명이 {id_key}_{expendGroupId} 라 남의 것을 맞히려면 두 ID 를 모두 알아야 한다.
        // 2.10 의 명세는 "로그인 필요"까지만 정했다 — 소유자 검사를 넣으려면 명세를 먼저
        // 고쳐야 한다(헌장 원칙 V). 지금 상태를 시험으로 못박아 둔다.
        assertThat(fetch(filename, otherToken).getStatus()).isEqualTo(200);
    }
}

package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

/**
 * 003 이 만든 API 13건의 응답 규격 — quickstart #42·#14 (SC-201·FR-217).
 *
 * <p>두 가지를 <b>한 자리에서</b> 본다.
 *
 * <ol>
 *   <li><b>#42</b> — 13건 중 12건이 {@code { resCode, data }} 이고 <b>2.10 만 예외</b>다.
 *       예외를 인정하는 것과 나머지를 규격으로 묶는 것이 같은 자리에 있어야 "예외가
 *       하나뿐"임이 드러난다. 따로 두면 두 번째 예외가 생겨도 아무도 알아채지 못한다.</li>
 *   <li><b>#14</b> — 목록 4건(2.2·2.6·2.8·2.13)의 {@code data} 에 {@code list} 만 있고
 *       {@code offset}·{@code limit}·{@code totalCount} 가 없다. 002 의 관리자 회원
 *       목록(1.13)이 페이징 3필드를 싣는 것과 형태가 다른 것은 <b>의도된 차이</b>다.</li>
 * </ol>
 *
 * <p>네 목록이 모두 선 뒤라야 한 자리에서 볼 수 있어 Polish 단계에 둔다.
 */
class ExpendGroupResponseContractIT extends AbstractExpendGroupIT {

    private static final String METHOD_URL = "/api/v1/payment-methods";

    /** 페이징 필드 이름. 목록 응답에 하나라도 있으면 FR-217 이 깨진 것이다. */
    private static final List<String> PAGING_FIELDS = List.of("offset", "limit", "totalCount");

    private MockHttpServletResponse raw(String url, String token) throws Exception {
        var request = MockMvcRequestBuilders.get(url);
        if (token != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn().getResponse();
    }

    /** 그 응답이 {@code { resCode, data }} 규격인가. */
    private void assertWrapped(JsonNode response, String api) {
        assertThat(response.has("resCode")).as("%s 에 resCode 가 없다", api).isTrue();
        assertThat(response.has("data")).as("%s 에 data 가 없다", api).isTrue();
        assertThat(response.size()).as("%s 의 최상위 필드는 둘뿐이어야 한다", api).isEqualTo(2);
        assertThat(response.get("resCode").asInt()).as("%s", api).isEqualTo(200);
    }

    /** 그 목록 응답의 {@code data} 에 {@code list} 만 있는가. */
    private void assertListOnly(JsonNode response, String api) {
        JsonNode data = response.get("data");
        assertThat(data.has("list")).as("%s 에 list 가 없다", api).isTrue();
        for (String field : PAGING_FIELDS) {
            assertThat(data.has(field))
                    .as("%s 에 페이징 필드 %s 가 있다 — 003 의 목록은 페이징을 두지 않는다",
                            api, field)
                    .isFalse();
        }
        assertThat(data.size()).as("%s 의 data 는 list 하나뿐이어야 한다", api).isEqualTo(1);
    }

    @Test
    @DisplayName("#42 003 의 12건은 { resCode, data } 규격이다")
    void twelveApisUseTheWrapper() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        // 2.1 등록
        JsonNode method = postJson(METHOD_URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);
        assertWrapped(method, "2.1 PaymentMethodCreate");
        long methodId = method.get("data").get("paymentMethodId").asLong();

        assertWrapped(getJson(METHOD_URL, token), "2.2 PaymentMethodList");
        assertWrapped(getJson(METHOD_URL + "/" + methodId, token), "2.3 PaymentMethodGet");
        assertWrapped(patchJson(METHOD_URL + "/" + methodId, token, """
                {"name":"국민카드(메인)"}
                """), "2.4 PaymentMethodUpdate");
        assertWrapped(getJson(METHOD_URL + "/active/EXPENSE", token),
                "2.6 PaymentMethodListActive");
        assertWrapped(deleteJson(METHOD_URL + "/" + methodId, token),
                "2.5 PaymentMethodDelete");

        // 2.7 등록
        JsonNode group = createGroup(token, "취미", true);
        assertWrapped(group, "2.7 ExpendGroupCreate");
        long groupId = group.get("data").get("expendGroupId").asLong();

        assertWrapped(getJson(URL, token), "2.8 ExpendGroupList");
        assertWrapped(getJson(URL + "/" + groupId, token), "2.9 ExpendGroupGet");
        assertWrapped(updateGroup(token, groupId, "여가", null), "2.11 ExpendGroupUpdate");
        assertWrapped(getJson(URL + "/active", token), "2.13 ExpendGroupListActive");
        assertWrapped(deleteJson(URL + "/" + groupId, token), "2.12 ExpendGroupDelete");
    }

    @Test
    @DisplayName("#42 2.10 만 규격의 예외다 — 성공 본문이 이미지 바이너리다")
    void onlyIconGetIsExempt() throws Exception {
        String token = signupAndLogin().token();
        long groupId = idOf(createGroup(token, "취미", true,
                new org.springframework.mock.web.MockMultipartFile("iconFile", "hobby.png",
                        MediaType.IMAGE_PNG_VALUE, pngBytes())));
        String filename = jdbc.queryForObject(
                "select icon_filename from moneylog.tbl_user_expend_group where idx = ?",
                String.class, groupId);

        MockHttpServletResponse response = raw("/api/v1/expend-groups/icons/" + filename, token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        // 이 하나만 래퍼가 아니다. 두 번째 예외가 생기면 위 시험이 먼저 깨진다.
        assertThat(response.getContentAsString(StandardCharsets.ISO_8859_1))
                .doesNotContain("resCode");
    }

    @Test
    @DisplayName("#14 목록 4건의 data 에는 list 만 있고 totalCount 가 없다")
    void everyListCarriesOnlyList() throws Exception {
        String token = signupAndLogin().token();
        assertThat(resCode(postJson(METHOD_URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """))).isEqualTo(200);
        idOf(createGroup(token, "취미", true));

        assertListOnly(getJson(METHOD_URL, token), "2.2 PaymentMethodList");
        assertListOnly(getJson(METHOD_URL + "/active/EXPENSE", token),
                "2.6 PaymentMethodListActive");
        assertListOnly(getJson(URL, token), "2.8 ExpendGroupList");
        assertListOnly(getJson(URL + "/active", token), "2.13 ExpendGroupListActive");
    }

    @Test
    @DisplayName("실패 응답도 같은 규격이고 data 는 message 한 칸이다")
    void failuresUseTheSameShape() throws Exception {
        String token = signupAndLogin().token();

        JsonNode response = getJson(METHOD_URL + "/999999999", token);

        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get("resCode").asInt()).isEqualTo(3003);
        assertThat(response.get("data").size()).isEqualTo(1);
        assertThat(response.get("data").get("message").asString()).isNotBlank();
    }

    /** 30×30 PNG. {@code icon} 패키지의 헬퍼와 같은 방식이며 여기서는 한 번만 쓴다. */
    private static byte[] pngBytes() throws Exception {
        var image = new java.awt.image.BufferedImage(30, 30,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}

package com.dbdomino.moneylog.backend.icon;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.expendgroup.AbstractExpendGroupIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/**
 * 아이콘 업로드(2.7 등록 · 2.11 수정) — quickstart #27·#28·#29·#37.
 *
 * <p><b>#28 이 FR-219 의 핵심이다.</b> 확장자만 {@code .png} 로 바꾼 텍스트 파일도
 * {@code 3102} 여야 한다 — 확장자만 검사하는 구현은 다른 시험을 전부 통과하고 이 하나에서만
 * 걸린다. 통과시키면 2.10 이 저장된 확장자를 보고 {@code image/png} 라고 말하면서 전혀 다른
 * 바이트를 내보내게 된다.
 */
class IconUploadIT extends AbstractExpendGroupIT {

    /** 그 유형에 저장된 아이콘 파일명. 없으면 {@code null}. */
    private String iconFilenameOf(long expendGroupId) {
        return jdbc.queryForObject(
                "select icon_filename from moneylog.tbl_user_expend_group where idx = ?",
                String.class, expendGroupId);
    }

    @Test
    @DisplayName("png·jpg·gif 는 등록에서 저장되고 iconUrl 이 실린다")
    void allowedFormatsAreStored() throws Exception {
        String token = signupAndLogin().token();

        for (String format : new String[] {"png", "jpg", "gif"}) {
            JsonNode response = createGroup(token, "취미-" + format, true,
                    IconTestImages.part("hobby." + format, MediaType.IMAGE_PNG_VALUE,
                            IconTestImages.image(format, 30)));

            assertThat(resCode(response)).as(format).isEqualTo(200);
            // 확장자는 업로드 파일명이 아니라 내용에서 정해진다.
            assertThat(response.get("data").get("iconUrl").asString())
                    .as(format)
                    .isEqualTo("/api/v1/expend-groups/icons/"
                            + iconFilenameOf(response.get("data").get("expendGroupId").asLong()));
        }
    }

    @Test
    @DisplayName("#27 이미지가 아닌 파일은 3102 다")
    void nonImageIs3102() throws Exception {
        String token = signupAndLogin().token();

        JsonNode response = createGroup(token, "취미", true,
                IconTestImages.part("hobby.txt", MediaType.TEXT_PLAIN_VALUE,
                        "그냥 텍스트".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertThat(resCode(response)).isEqualTo(3102);
    }

    @Test
    @DisplayName("#28 확장자만 .png 로 바꾼 텍스트 파일도 3102 다 — 내용으로 판정한다")
    void disguisedExtensionIs3102() throws Exception {
        String token = signupAndLogin().token();

        JsonNode response = createGroup(token, "취미", true, IconTestImages.disguisedText());

        assertThat(resCode(response)).isEqualTo(3102);
    }

    @Test
    @DisplayName("#29 1MB 를 넘으면 3102 다")
    void oversizedIs3102() throws Exception {
        String token = signupAndLogin().token();

        JsonNode response = createGroup(token, "취미", true, IconTestImages.oversized());

        assertThat(resCode(response)).isEqualTo(3102);
    }

    @Test
    @DisplayName("아이콘이 거절되면 유형도 만들어지지 않는다 — 반쯤 성공한 등록을 남기지 않는다")
    void rejectedIconRollsBackTheRow() throws Exception {
        String token = signupAndLogin().token();

        assertThat(resCode(createGroup(token, "취미", true, IconTestImages.disguisedText())))
                .isEqualTo(3102);

        // 행이 남았다면 같은 이름의 재등록이 3101 로 막힌다.
        assertThat(resCode(createGroup(token, "취미", true))).isEqualTo(200);
    }

    @Test
    @DisplayName("#37 2.11 에서 iconFile 을 omit 하면 기존 아이콘이 유지된다")
    void omittingIconFileKeepsTheExistingOne() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true,
                IconTestImages.part("hobby.png", MediaType.IMAGE_PNG_VALUE, IconTestImages.png())));
        String before = iconFilenameOf(id);
        assertThat(before).isNotNull();

        JsonNode response = updateGroup(token, id, "여가", false);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(iconFilenameOf(id)).isEqualTo(before);
        assertThat(response.get("data").get("iconUrl").asString()).endsWith(before);
    }

    @Test
    @DisplayName("2.11 로 아이콘을 보내면 교체된다 — 파일명이 같아 덮어쓴다")
    void sendingIconFileReplacesIt() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));
        assertThat(iconFilenameOf(id)).isNull();

        JsonNode response = updateGroup(token, id, null, null,
                IconTestImages.part("hobby.png", MediaType.IMAGE_PNG_VALUE, IconTestImages.png()));

        assertThat(resCode(response)).isEqualTo(200);
        // 파일명 규칙은 {id_key}_{expendGroupId}.{확장자} 다.
        assertThat(iconFilenameOf(id)).endsWith("_" + id + ".png");
    }

    @Test
    @DisplayName("2.11 에서 아이콘이 거절되면 이름·사용 여부 변경도 함께 되돌아간다")
    void rejectedIconRollsBackTheUpdate() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));

        assertThat(resCode(updateGroup(token, id, "여가", false, IconTestImages.disguisedText())))
                .isEqualTo(3102);

        String name = jdbc.queryForObject(
                "select name from moneylog.tbl_user_expend_group where idx = ?", String.class, id);
        Boolean inUse = jdbc.queryForObject(
                "select in_use from moneylog.tbl_user_expend_group where idx = ?", Boolean.class, id);
        assertThat(name).isEqualTo("취미");
        assertThat(inUse).isTrue();
    }

    @Test
    @DisplayName("이름 판정이 아이콘 판정보다 먼저다 — 기본 유형 이름 변경은 3105 다")
    void nameCheckComesBeforeIconCheck() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");

        // 이름(3105)과 아이콘(3102) 둘 다 잘못된 요청. api-contract.md §5 는 이름이 먼저다.
        assertThat(resCode(updateGroup(member.token(), id, "밥값", null,
                IconTestImages.disguisedText()))).isEqualTo(3105);
    }
}

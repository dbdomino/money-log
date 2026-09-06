package com.dbdomino.moneylog.backend.icon;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.config.IconProperties;
import com.dbdomino.moneylog.backend.expendgroup.AbstractExpendGroupIT;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/**
 * 아이콘의 수명 — quickstart #25·#26·#33·#34·#35.
 *
 * <p>이 클래스가 확인하는 것은 <b>파일명이 무엇을 담고 무엇을 담지 않는가</b>다.
 * 파일명은 {@code {id_key}_{expendGroupId}.{확장자}} 하나이며(FR-224), 유형 이름이 들어가면
 * 조회 URL 경로가 {@code /}·{@code ..}·공백으로 깨지고 이름을 바꿀 때마다 파일이 무효가 된다.
 */
class IconLifecycleIT extends AbstractExpendGroupIT {

    @Autowired
    private IconProperties iconProperties;

    private String iconFilenameOf(long expendGroupId) {
        return jdbc.queryForObject(
                "select icon_filename from moneylog.tbl_user_expend_group where idx = ?",
                String.class, expendGroupId);
    }

    /** 아이콘을 올린 유형을 만들고 PK 를 돌려준다. */
    private long createWithIcon(String token, String name) throws Exception {
        return idOf(createGroup(token, name, true,
                IconTestImages.part("hobby.png", MediaType.IMAGE_PNG_VALUE, IconTestImages.png())));
    }

    @Test
    @DisplayName("#25 아이콘이 있는 유형의 iconUrl 은 조회 경로 문자열이다")
    void iconUrlIsAPath() throws Exception {
        String token = signupAndLogin().token();
        long id = createWithIcon(token, "취미");

        JsonNode data = getJson(URL + "/" + id, token).get("data");

        assertThat(data.get("iconUrl").asString())
                .isEqualTo("/api/v1/expend-groups/icons/" + iconFilenameOf(id));
    }

    @Test
    @DisplayName("#26 아이콘이 없는 유형도 iconUrl 필드가 있고 값이 null 이다")
    void missingIconIsNullNotAbsent() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));

        JsonNode data = getJson(URL + "/" + id, token).get("data");

        // 필드가 빠지면 프론트의 'iconUrl' in obj 분기가 다른 결과를 낸다(SC-209).
        assertThat(data.has("iconUrl")).isTrue();
        assertThat(data.get("iconUrl").isNull()).isTrue();
    }

    @Test
    @DisplayName("#33 유형을 삭제 표시해도 아이콘 파일은 남는다")
    void softDeleteKeepsTheFile() throws Exception {
        String token = signupAndLogin().token();
        long id = createWithIcon(token, "취미");
        String filename = iconFilenameOf(id);

        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        // 파일을 지우면 "행은 있는데 파일이 없는" 상태가 되어 2.10 이 3104 를 낸다(FR-215).
        assertThat(Files.exists(iconProperties.directory().resolve(filename))).isTrue();
        assertThat(iconFilenameOf(id)).isEqualTo(filename);
    }

    @Test
    @DisplayName("#34 유형 이름을 바꿔도 아이콘이 그대로 유효하다")
    void renamingKeepsTheIcon() throws Exception {
        String token = signupAndLogin().token();
        long id = createWithIcon(token, "취미");
        String before = iconFilenameOf(id);

        assertThat(resCode(updateGroup(token, id, "여가", null))).isEqualTo(200);

        // 파일명이 이름을 담지 않으므로 바뀔 이유가 없다.
        assertThat(iconFilenameOf(id)).isEqualTo(before);
        assertThat(Files.exists(iconProperties.directory().resolve(before))).isTrue();
    }

    @Test
    @DisplayName("#35 저장된 파일명은 전부 {id_key}_{expendGroupId}.{확장자} 이고 유형 이름이 없다")
    void everyFilenameFollowsTheIdRule() throws Exception {
        Member member = signupAndLogin();
        createWithIcon(member.token(), "취미");
        Long idKey = jdbc.queryForObject(
                "select id_key from moneylog.tbl_user where user_id = ?",
                Long.class, member.memberId());

        List<Map<String, Object>> rows = jdbc.queryForList("""
                select idx, name, icon_filename from moneylog.tbl_user_expend_group
                 where id_key = ? and icon_filename is not null
                """, idKey);

        // 기본 10종 + 방금 만든 1종.
        assertThat(rows).hasSize(11);
        for (Map<String, Object> row : rows) {
            String filename = (String) row.get("icon_filename");
            assertThat(filename)
                    .as("파일명 규칙")
                    .isEqualTo(idKey + "_" + row.get("idx") + "." + extensionOf(filename));
            assertThat(filename)
                    .as("유형 이름이 파일명에 들어가면 조회 URL 경로가 깨진다")
                    .doesNotContain((String) row.get("name"))
                    .doesNotContain(member.memberId());
        }
    }

    private static String extensionOf(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}

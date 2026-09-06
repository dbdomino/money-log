package com.dbdomino.moneylog.backend.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.backend.config.IconProperties;
import com.dbdomino.moneylog.backend.service.DefaultExpendGroupService;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * US2 — 가입이 만드는 기본 지출유형 10종(FR-106 · SC-105).
 *
 * <p>quickstart.md §3 의 시나리오 #10 에 대응한다.
 */
class DefaultExpendGroupIT extends AbstractApiIT {

    @Autowired
    private IconProperties iconProperties;

    @Test
    @DisplayName("#10 가입 직후 지출유형이 정확히 10건이고 전부 defaultGroup=true 다 (SC-105)")
    void signupCreatesExactlyTenDefaultGroups() throws Exception {
        String memberId = signup();
        Long idKey = idKeyOf(memberId);

        List<Map<String, Object>> groups = groupRows(idKey);

        assertThat(groups).hasSize(10);
        assertThat(groups).extracting(row -> row.get("name"))
                .containsExactlyInAnyOrderElementsOf(DefaultExpendGroupService.defaultNames());
        assertThat(groups).allSatisfy(row -> {
            assertThat(row.get("default_group")).isEqualTo(Boolean.TRUE);
            assertThat(row.get("in_use")).isEqualTo(Boolean.TRUE);
            assertThat(row.get("deleted")).isEqualTo(Boolean.FALSE);
            // 이 행들의 주인은 그 회원이다. 관리자가 추가한 경우에도 마찬가지다.
            assertThat(row.get("created_by")).isEqualTo(idKey);
        });
    }

    @Test
    @DisplayName("#10 아이콘 파일명이 {id_key}_{expendGroupId}.png 이고 실제 파일이 존재한다")
    void iconFilesFollowIdBasedNamingAndExist() throws Exception {
        String memberId = signup();
        Long idKey = idKeyOf(memberId);

        for (Map<String, Object> row : groupRows(idKey)) {
            Long expendGroupId = ((Number) row.get("idx")).longValue();
            String filename = (String) row.get("icon_filename");

            assertThat(filename).isEqualTo(idKey + "_" + expendGroupId + ".png");
            // 유형 이름이 파일명에 들어가면 조회 URL 경로가 깨진다(003 FR-224).
            assertThat(filename).doesNotContain((String) row.get("name")).doesNotContain(memberId);
            assertThat(Files.exists(iconProperties.directory().resolve(filename)))
                    .as("복사본 파일이 디스크에 있어야 한다: %s", filename)
                    .isTrue();
        }
    }

    /** 가입하고 그 아이디를 돌려준다. */
    private String signup() throws Exception {
        String memberId = TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        JsonNode response = postJson("/api/v1/auth/signup", """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"테스트회원"}
                """.formatted(memberId, TEST_PASSWORD, TEST_PASSWORD));
        assertThat(resCode(response)).isEqualTo(200);
        return memberId;
    }

    private Long idKeyOf(String memberId) {
        return jdbc.queryForObject(
                "select id_key from moneylog.tbl_user where user_id = ?", Long.class, memberId);
    }

    private List<Map<String, Object>> groupRows(Long idKey) {
        return jdbc.queryForList("""
                select idx, name, in_use, default_group, deleted, icon_filename, created_by
                  from moneylog.tbl_user_expend_group
                 where id_key = ?
                 order by idx
                """, idKey);
    }
}

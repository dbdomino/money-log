package com.dbdomino.moneylog.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US4 — 관리자 회원 추가(1.12)와 수정(1.15).
 *
 * <p>quickstart.md §3 의 시나리오 #29·#30 에 대응한다(FR-121·SC-105).
 */
class AdminMemberCreateIT extends AbstractApiIT {

    @Test
    @DisplayName("#29 관리자가 만든 회원의 created_by 는 관리자의 id_key 다 (FR-121)")
    void createdByIsTheAdminIdKey() throws Exception {
        User admin = createAdmin();
        Tokens tokens = login(admin);
        String newMemberId = newMemberId();

        JsonNode response = postJson("/api/v1/admin/members", tokens.accessToken(),
                createBody(newMemberId, User.ROLE_MEMBER));

        assertThat(resCode(response)).isEqualTo(200);
        Map<String, Object> row = userRow(newMemberId);
        // 본인 가입은 null 이다. 여기는 조작한 관리자가 남는다.
        assertThat(row.get("created_by")).isEqualTo(admin.getIdKey());
        assertThat(row.get("updated_by")).isEqualTo(admin.getIdKey());
        assertThat(response.get("data").get("active").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("#30 관리자가 추가한 회원에게도 기본 지출유형이 정확히 10건 생긴다 (SC-105)")
    void adminCreatedMemberAlsoGetsDefaultGroups() throws Exception {
        Tokens tokens = login(createAdmin());
        String newMemberId = newMemberId();

        assertThat(resCode(postJson("/api/v1/admin/members", tokens.accessToken(),
                createBody(newMemberId, User.ROLE_MEMBER)))).isEqualTo(200);

        Long idKey = (Long) userRow(newMemberId).get("id_key");
        Integer groups = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_user_expend_group
                 where id_key = ? and default_group = true
                """, Integer.class, idKey);
        assertThat(groups).isEqualTo(10);
    }

    @Test
    @DisplayName("#29 관리자는 관리자 계정을 만들 수 있다 — 가입으로는 못 만드는 것이 이 API 의 존재 이유다")
    void adminCanCreateAnotherAdmin() throws Exception {
        Tokens tokens = login(createAdmin());
        String newAdminId = newMemberId();

        JsonNode response = postJson("/api/v1/admin/members", tokens.accessToken(),
                createBody(newAdminId, User.ROLE_ADMIN));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("role").asInt()).isEqualTo(User.ROLE_ADMIN);
    }

    @Test
    @DisplayName("#29 role 이 1·3 밖이면 9001 이다")
    void invalidRoleIsRejected() throws Exception {
        Tokens tokens = login(createAdmin());

        JsonNode response = postJson("/api/v1/admin/members", tokens.accessToken(),
                createBody(newMemberId(), (short) 2));

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("#29 수정(1.15)도 관리자의 id_key 를 updated_by 에 남기고 권한을 바꿀 수 있다")
    void updateStampsAdminAndCanChangeRole() throws Exception {
        User admin = createAdmin();
        User target = createMember();
        Tokens tokens = login(admin);

        JsonNode response = patchJson("/api/v1/admin/members/" + target.getUserId(),
                tokens.accessToken(), """
                {"role":1,"nickname":"승격된회원"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("role").asInt()).isEqualTo(User.ROLE_ADMIN);
        assertThat(response.get("data").get("nickname").asString()).isEqualTo("승격된회원");
        assertThat(userRow(target.getUserId()).get("updated_by")).isEqualTo(admin.getIdKey());
    }

    @Test
    @DisplayName("#29 이미 쓰이는 아이디로 추가하면 2002 다")
    void duplicatedMemberIdIsRejected() throws Exception {
        User admin = createAdmin();
        User existing = createMember();
        Tokens tokens = login(admin);

        JsonNode response = postJson("/api/v1/admin/members", tokens.accessToken(),
                createBody(existing.getUserId(), User.ROLE_MEMBER));

        assertThat(resCode(response)).isEqualTo(2002);
    }

    private String createBody(String memberId, short role) {
        return """
                {"memberId":"%s","password":"%s","nickname":"관리자가만든회원","role":%d}
                """.formatted(memberId, TEST_PASSWORD, role);
    }

    private String newMemberId() {
        return TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> userRow(String memberId) {
        return jdbc.queryForMap("""
                select id_key, role, active, created_by, updated_by
                  from moneylog.tbl_user
                 where user_id = ?
                """, memberId);
    }
}

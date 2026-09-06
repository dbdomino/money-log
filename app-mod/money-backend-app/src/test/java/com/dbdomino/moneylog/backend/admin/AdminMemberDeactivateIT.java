package com.dbdomino.moneylog.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US4 — 회원 정지(1.16).
 *
 * <p>quickstart.md §3 의 시나리오 #25·#26·#27·#28 에 대응한다(FR-118·FR-119).
 */
class AdminMemberDeactivateIT extends AbstractApiIT {

    @Test
    @DisplayName("#25 정지된 회원은 로그인이 1004 로 막히지만 회원 행과 데이터는 남는다")
    void deactivatedMemberCannotLoginButRowsRemain() throws Exception {
        User admin = createAdmin();
        User target = createMember();
        // 그 회원이 소유한 데이터가 있는 상태에서 정지한다.
        int groupsBefore = countExpendGroups(target);
        Tokens adminTokens = login(admin);

        JsonNode response = deactivate(adminTokens, target);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("active").asBoolean()).isFalse();
        assertThat(resCode(loginAttempt(target))).isEqualTo(1004);
        // 정지는 표시만 바꾸는 것이다 — 지우는 것이 아니다(FR-118).
        assertThat(userExists(target)).isTrue();
        assertThat(countExpendGroups(target)).isEqualTo(groupsBefore);
    }

    @Test
    @DisplayName("#26 정지하면 그 회원의 활성 세션이 폐기된다 (FR-119)")
    void deactivateRevokesTargetSession() throws Exception {
        User admin = createAdmin();
        User target = createMember();
        Tokens targetTokens = login(target);
        assertThat(countActiveSessions(target)).isEqualTo(1);
        Tokens adminTokens = login(admin);

        assertThat(resCode(deactivate(adminTokens, target))).isEqualTo(200);

        assertThat(countActiveSessions(target)).isZero();
        // 이미 로그인해 있던 사람도 즉시 막힌다. 남겨 두면 정지가 절반만 된다.
        assertThat(resCode(getJson("/api/v1/members/me", targetTokens.accessToken())))
                .isEqualTo(1006);
    }

    @Test
    @DisplayName("#27 이미 정지된 회원을 다시 정지하면 9001 이다 — 성공으로 흘리지 않는다")
    void secondDeactivateIsRejected() throws Exception {
        User admin = createAdmin();
        User target = createInactiveMember();
        Tokens adminTokens = login(admin);

        JsonNode response = deactivate(adminTokens, target);

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("#28 관리자가 자기 계정을 정지하면 9001 로 거절한다")
    void adminCannotDeactivateSelf() throws Exception {
        User admin = createAdmin();
        Tokens tokens = login(admin);

        JsonNode response = deactivate(tokens, admin);

        // 마지막 관리자가 스스로를 잠그면 되살릴 방법이 없다(재활성화 API 가 없다).
        assertThat(resCode(response)).isEqualTo(9001);
        assertThat(resCode(loginAttempt(admin))).isEqualTo(200);
    }

    @Test
    @DisplayName("#27 없는 아이디를 정지하면 2001 이다")
    void unknownMemberIsRejectedWith2001() throws Exception {
        Tokens tokens = login(createAdmin());

        JsonNode response = patchJson(
                "/api/v1/admin/members/ittestnobodyxx/deactivate", tokens.accessToken(), null);

        assertThat(resCode(response)).isEqualTo(2001);
    }

    private JsonNode deactivate(Tokens adminTokens, User target) throws Exception {
        return patchJson("/api/v1/admin/members/" + target.getUserId() + "/deactivate",
                adminTokens.accessToken(), null);
    }

    private JsonNode loginAttempt(User user) throws Exception {
        return postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD));
    }

    private boolean userExists(User user) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user where id_key = ?",
                Integer.class, user.getIdKey());
        return count != null && count == 1;
    }

    private int countExpendGroups(User user) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_expend_group where id_key = ?",
                Integer.class, user.getIdKey());
        return count == null ? 0 : count;
    }
}

package com.dbdomino.moneylog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US1 — 실패 경로와 로그인 이력.
 *
 * <p>quickstart.md §3 의 시나리오 #7·#31·#32·#33 에 대응한다
 * (SC-108·SC-110·SC-111 · FR-125·FR-127·FR-128).
 */
class LoginFailureAndHistoryIT extends AbstractApiIT {

    @Test
    @DisplayName("#7 비활성 계정으로 로그인하면 1004 이고 실패 이력이 남는다")
    void inactiveAccountIsRejectedWith1004() throws Exception {
        User user = createInactiveMember();

        JsonNode response = postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD));

        assertThat(resCode(response)).isEqualTo(1004);
        assertThat(countActiveSessions(user)).isZero();
        // 회원이 특정되므로 이력을 남긴다(FR-127).
        assertThat(historyRows(user)).singleElement()
                .satisfies(row -> assertThat(row.get("success")).isEqualTo(Boolean.FALSE));
    }

    @Test
    @DisplayName("#31 성공 1회 + 비밀번호 오류 1회면 이력이 각 1건이고 success 가 true·false 다 (SC-108)")
    void historyRecordsBothSuccessAndFailure() throws Exception {
        User user = createMember();

        assertThat(resCode(postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD)))).isEqualTo(200);

        JsonNode failed = postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"WrongPass1!"}
                """.formatted(user.getUserId()));
        // 아이디가 없을 때와 같은 코드다. 나누면 "그 아이디는 존재한다"를 알려 주게 된다.
        assertThat(resCode(failed)).isEqualTo(1003);

        List<Map<String, Object>> rows = historyRows(user);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("success"))
                .containsExactlyInAnyOrder(Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    @DisplayName("#32 존재하지 않는 아이디로 시도하면 이력 행이 늘지 않는다 (SC-110·FR-127)")
    void unknownMemberLeavesNoHistoryRow() throws Exception {
        int before = countAllHistory();

        JsonNode response = postJson("/api/v1/auth/login",
                """
                {"memberId":"ittestnobody%s","password":"%s"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), TEST_PASSWORD));

        assertThat(resCode(response)).isEqualTo(1003);
        // id_key 가 NOT NULL 이라 채울 값이 없다. 그 시도는 로그로만 남긴다.
        assertThat(countAllHistory()).isEqualTo(before);
    }

    @Test
    @DisplayName("#33 이력 어느 행에도 비밀번호·토큰 값이 들어가지 않는다 (SC-111)")
    void historyNeverStoresSecrets() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"WrongPass1!"}
                """.formatted(user.getUserId()));

        // 이력 테이블은 회원·시각·IP·성공 여부만 갖는다. 값이 새어 들어갈 컬럼 자체가 없어야 한다.
        List<Map<String, Object>> rows = historyRows(user);
        assertThat(rows).hasSize(2);
        for (Map<String, Object> row : rows) {
            String dumped = row.toString();
            assertThat(dumped).doesNotContain(TEST_PASSWORD);
            assertThat(dumped).doesNotContain("WrongPass1!");
            assertThat(dumped).doesNotContain(tokens.accessToken());
            assertThat(dumped).doesNotContain(tokens.refreshToken());
        }
    }

    @Test
    @DisplayName("#36 필수 필드를 빠뜨리면 HTTP 200 + 9001 이다 — 인증 실패와 구분된다")
    void missingFieldIsRejectedWith9001() throws Exception {
        JsonNode response = postJson("/api/v1/auth/login", """
                {"memberId":"ittestsomeone"}
                """);

        assertThat(resCode(response)).isEqualTo(9001);
    }

    private List<Map<String, Object>> historyRows(User user) {
        return jdbc.queryForList("""
                select idx, id_key, login_at, login_ip, success, created_by, updated_by
                  from moneylog.tbl_user_login_history
                 where id_key = ?
                 order by idx
                """, user.getIdKey());
    }

    private int countAllHistory() {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_login_history", Integer.class);
        return count == null ? 0 : count;
    }
}

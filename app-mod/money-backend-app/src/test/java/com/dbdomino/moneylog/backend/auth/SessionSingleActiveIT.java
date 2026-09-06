package com.dbdomino.moneylog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US1 — 회원당 활성 세션은 언제나 1건이고, 폐기된 토큰은 통하지 않는다.
 *
 * <p>quickstart.md §3 의 시나리오 #2·#4·#6 에 대응한다(SC-103·SC-104).
 */
class SessionSingleActiveIT extends AbstractApiIT {

    @Test
    @DisplayName("#2 같은 회원이 다시 로그인하면 기존 세션은 해시가 비워지고 revoked 가 서며 활성은 1건뿐이다")
    void reloginRevokesPreviousSession() throws Exception {
        User user = createMember();
        login(user);
        Long firstSessionIdx = ((Number) sessionRows(user).get(0).get("idx")).longValue();

        login(user);

        assertThat(countActiveSessions(user)).isEqualTo(1);
        Map<String, Object> revoked = jdbc.queryForMap("""
                select access_token_hash, refresh_token_hash, revoked
                  from moneylog.tbl_user_session
                 where idx = ?
                """, firstSessionIdx);
        // 폐기는 행 삭제가 아니다. 두 해시를 비우고 revoked 를 세운 흔적이 남아야 한다(FR-111).
        assertThat(revoked.get("access_token_hash")).isNull();
        assertThat(revoked.get("refresh_token_hash")).isNull();
        assertThat(revoked.get("revoked")).isEqualTo(Boolean.TRUE);
        assertThat(sessionRows(user)).hasSize(2);
    }

    @Test
    @DisplayName("#4 재로그인으로 폐기된 Access 로 보호 API 를 부르면 1006 이다 (SC-104)")
    void revokedAccessTokenIsRejectedWith1006() throws Exception {
        User user = createMember();
        Tokens first = login(user);
        login(user);

        JsonNode response = getJson("/api/v1/auth/validate", first.accessToken());

        // 토큰 자체는 서명·만료 모두 멀쩡하다. 세션이 갈린 것이므로 1001 이 아니라 1006 이다.
        assertThat(resCode(response)).isEqualTo(1006);
    }

    @Test
    @DisplayName("#6 로그아웃한 뒤 그 Refresh 로 갱신하면 1006 이 아니라 1005 다")
    void refreshAfterLogoutIsRejectedWith1005() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        assertThat(resCode(postJson("/api/v1/auth/revoke", tokens.accessToken(), null))).isEqualTo(200);

        JsonNode response = postJson("/api/v1/auth/refresh",
                """
                {"refreshToken":"%s"}
                """.formatted(tokens.refreshToken()));

        // 로그아웃은 해시를 NULL 로 만든다 → 해시 조회가 실패하는 단계에서 걸리므로 1005 다.
        // 1006 은 "다른 곳에서 로그인해 세션이 교체된" 경우에 쓴다(api-contract.md §5).
        assertThat(resCode(response)).isEqualTo(1005);
    }

    @Test
    @DisplayName("#6 이미 로그아웃한 세션으로 다시 로그아웃하면 1006 이다 — 성공으로 흘리지 않는다")
    void secondRevokeIsRejected() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        assertThat(resCode(postJson("/api/v1/auth/revoke", tokens.accessToken(), null))).isEqualTo(200);

        JsonNode response = postJson("/api/v1/auth/revoke", tokens.accessToken(), null);

        assertThat(resCode(response)).isEqualTo(1006);
    }

    private java.util.List<Map<String, Object>> sessionRows(User user) {
        return jdbc.queryForList("""
                select idx, session_id, revoked
                  from moneylog.tbl_user_session
                 where id_key = ?
                 order by idx
                """, user.getIdKey());
    }
}

package com.dbdomino.moneylog.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US1 — 발급·검증·갱신이 한 바퀴 도는가.
 *
 * <p>quickstart.md §3 의 시나리오 #1·#3·#5·#8 에 대응한다.
 */
class TokenLifecycleIT extends AbstractApiIT {

    @Test
    @DisplayName("#1 로그인하면 Access(1일)·Refresh(7일)가 발급되고 세션 1건이 저장된다")
    void loginIssuesTokensAndCreatesSession() throws Exception {
        User user = createMember();

        JsonNode response = postJson("/api/v1/auth/login",
                """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD));

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("memberId").asString()).isEqualTo(user.getUserId());
        assertThat(data.get("nickname").asString()).isEqualTo(user.getNickname());
        assertThat(data.get("role").asInt()).isEqualTo(User.ROLE_MEMBER);
        assertThat(data.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(data.get("accessToken").asString()).isNotBlank();
        assertThat(data.get("refreshToken").asString()).isNotBlank();
        // 남은 초는 설정값(1일·7일)에서 발급 처리 시간만큼만 줄어 있어야 한다.
        assertThat(data.get("expiresIn").asLong()).isBetween(86_000L, 86_400L);
        assertThat(data.get("refreshExpiresIn").asLong()).isBetween(604_400L, 604_800L);
        // 비밀번호는 응답에 실리지 않는다(SC-107). 필드가 없는지로 본다 —
        // 토큰은 Base64 라 "pw" 같은 짧은 문자열이 우연히 들어갈 수 있어 문자열 검사는 못 쓴다.
        assertThat(data.has("pw")).isFalse();
        assertThat(data.has("password")).isFalse();
        assertThat(response.toString()).doesNotContain(TEST_PASSWORD);

        assertThat(countActiveSessions(user)).isEqualTo(1);
    }

    @Test
    @DisplayName("#3 유효한 Access 로 검증하면 valid=true 와 남은 만료 시간이 온다")
    void validateReturnsRemainingTime() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);

        JsonNode response = getJson("/api/v1/auth/validate", tokens.accessToken());

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("valid").asBoolean()).isTrue();
        assertThat(data.get("memberId").asString()).isEqualTo(user.getUserId());
        assertThat(data.get("role").asInt()).isEqualTo(User.ROLE_MEMBER);
        assertThat(data.get("expiresIn").asLong()).isPositive();
    }

    @Test
    @DisplayName("#5 갱신하면 새 토큰 쌍이 오고 session_id 는 그대로다 — 재로그인이 아니라 Rotation 이다")
    void refreshRotatesTokensWithinSameSession() throws Exception {
        User user = createMember();
        Tokens issued = login(user);
        Map<String, Object> before = sessionRow(user);

        JsonNode response = postJson("/api/v1/auth/refresh",
                """
                {"refreshToken":"%s"}
                """.formatted(issued.refreshToken()));

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("accessToken").asString()).isNotEqualTo(issued.accessToken());
        assertThat(data.get("refreshToken").asString()).isNotEqualTo(issued.refreshToken());

        Map<String, Object> after = sessionRow(user);
        // 같은 행이어야 한다. 새 행이 생기거나 UUID 가 바뀌었으면 Rotation 이 아니다.
        assertThat(after.get("idx")).isEqualTo(before.get("idx"));
        assertThat(after.get("session_id")).isEqualTo(before.get("session_id"));
        assertThat(after.get("access_token_hash")).isNotEqualTo(before.get("access_token_hash"));
        assertThat(after.get("refresh_token_hash")).isNotEqualTo(before.get("refresh_token_hash"));
        assertThat(countActiveSessions(user)).isEqualTo(1);
    }

    @Test
    @DisplayName("#8 로그인 → 검증 → 갱신 → 로그아웃 한 바퀴가 전부 성공한다 (SC-102)")
    void fullLifecycleSucceeds() throws Exception {
        User user = createMember();

        Tokens issued = login(user);
        assertThat(resCode(getJson("/api/v1/auth/validate", issued.accessToken()))).isEqualTo(200);

        JsonNode refreshed = postJson("/api/v1/auth/refresh",
                """
                {"refreshToken":"%s"}
                """.formatted(issued.refreshToken()));
        assertThat(resCode(refreshed)).isEqualTo(200);
        String newAccessToken = refreshed.get("data").get("accessToken").asString();

        assertThat(resCode(postJson("/api/v1/auth/revoke", newAccessToken, null))).isEqualTo(200);
        assertThat(countActiveSessions(user)).isZero();
    }

    /** 그 회원의 세션 행 1건을 원시 컬럼으로 읽는다. Rotation 이 같은 행을 고쳤는지 보려는 것이다. */
    private Map<String, Object> sessionRow(User user) {
        return jdbc.queryForMap("""
                select idx, session_id, access_token_hash, refresh_token_hash
                  from moneylog.tbl_user_session
                 where id_key = ?
                """, user.getIdKey());
    }
}

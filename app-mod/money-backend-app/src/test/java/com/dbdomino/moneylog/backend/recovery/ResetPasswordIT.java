package com.dbdomino.moneylog.backend.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US3 — 비밀번호 찾기(1.10)와 재설정(1.11).
 *
 * <p>quickstart.md §3 의 시나리오 #18·#19·#20 에 대응한다.
 */
class ResetPasswordIT extends AbstractApiIT {

    private static final String NEW_PASSWORD = "Reset1234!";

    @Test
    @DisplayName("#18 재설정한 뒤 옛 비밀번호로 로그인하면 1003 이고, 새 비밀번호로는 된다")
    void oldPasswordStopsWorkingAfterReset() throws Exception {
        User user = createMember();

        assertThat(resCode(resetPassword(user, NEW_PASSWORD, NEW_PASSWORD))).isEqualTo(200);

        assertThat(resCode(loginWith(user, TEST_PASSWORD))).isEqualTo(1003);
        assertThat(resCode(loginWith(user, NEW_PASSWORD))).isEqualTo(200);
    }

    @Test
    @DisplayName("#19 재설정하면 기존 활성 세션이 폐기된다")
    void resetRevokesActiveSession() throws Exception {
        User user = createMember();
        Tokens tokens = login(user);
        assertThat(countActiveSessions(user)).isEqualTo(1);

        assertThat(resCode(resetPassword(user, NEW_PASSWORD, NEW_PASSWORD))).isEqualTo(200);

        assertThat(countActiveSessions(user)).isZero();
        // 재설정 전에 쓰던 토큰도 더는 통하지 않는다.
        assertThat(resCode(getJson("/api/v1/members/me", tokens.accessToken()))).isEqualTo(1006);
    }

    @Test
    @DisplayName("#20 1.10 을 건너뛰고 1.11 만 불러도 판정이 같다 — 재설정 토큰을 두지 않는다")
    void resetWorksWithoutCallingFindPasswordFirst() throws Exception {
        User withFind = createMember();
        User withoutFind = createMember();

        // 한쪽만 찾기를 거친다.
        assertThat(resCode(findPassword(withFind))).isEqualTo(200);

        assertThat(resCode(resetPassword(withFind, NEW_PASSWORD, NEW_PASSWORD))).isEqualTo(200);
        assertThat(resCode(resetPassword(withoutFind, NEW_PASSWORD, NEW_PASSWORD))).isEqualTo(200);
    }

    @Test
    @DisplayName("#20 닉네임이 다르면 1.10 도 1.11 도 2001 이다 — 같은 대조를 두 번 한다")
    void nicknameMismatchIsRejectedByBoth() throws Exception {
        User user = createMember();

        JsonNode find = postJson("/api/v1/auth/find-password", """
                {"memberId":"%s","nickname":"다른닉네임"}
                """.formatted(user.getUserId()));
        JsonNode reset = postJson("/api/v1/auth/reset-password", """
                {"memberId":"%s","nickname":"다른닉네임","newPassword":"%s","newPasswordConfirm":"%s"}
                """.formatted(user.getUserId(), NEW_PASSWORD, NEW_PASSWORD));

        assertThat(resCode(find)).isEqualTo(2001);
        assertThat(resCode(reset)).isEqualTo(2001);
    }

    @Test
    @DisplayName("#20 비활성 계정은 1004 로 거절한다")
    void inactiveMemberIsRejectedWith1004() throws Exception {
        User user = createInactiveMember();

        assertThat(resCode(findPassword(user))).isEqualTo(1004);
        assertThat(resCode(resetPassword(user, NEW_PASSWORD, NEW_PASSWORD))).isEqualTo(1004);
    }

    @Test
    @DisplayName("#20 새 비밀번호 확인이 다르면 2005, 규칙을 어기면 2004 다")
    void newPasswordIsValidatedBeforeMemberLookup() throws Exception {
        User user = createMember();

        assertThat(resCode(resetPassword(user, NEW_PASSWORD, "Other1234!"))).isEqualTo(2005);
        assertThat(resCode(resetPassword(user, "abcdefghij", "abcdefghij"))).isEqualTo(2004);
        // 거절됐으니 비밀번호는 그대로다.
        assertThat(resCode(loginWith(user, TEST_PASSWORD))).isEqualTo(200);
    }

    private JsonNode findPassword(User user) throws Exception {
        return postJson("/api/v1/auth/find-password", """
                {"memberId":"%s","nickname":"%s"}
                """.formatted(user.getUserId(), user.getNickname()));
    }

    private JsonNode resetPassword(User user, String newPassword, String confirm) throws Exception {
        return postJson("/api/v1/auth/reset-password", """
                {"memberId":"%s","nickname":"%s","newPassword":"%s","newPasswordConfirm":"%s"}
                """.formatted(user.getUserId(), user.getNickname(), newPassword, confirm));
    }

    private JsonNode loginWith(User user, String password) throws Exception {
        return postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), password));
    }
}

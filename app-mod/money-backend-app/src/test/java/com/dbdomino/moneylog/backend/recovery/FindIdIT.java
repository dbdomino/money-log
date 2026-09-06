package com.dbdomino.moneylog.backend.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US3 — 아이디 찾기(1.9).
 *
 * <p>quickstart.md §3 의 시나리오 #16·#17 에 대응한다(SC-109).
 */
class FindIdIT extends AbstractApiIT {

    @Test
    @DisplayName("#16 가입 이메일로 아이디를 찾으면 원문과 다른 값과 masked=true 가 온다 (SC-109)")
    void findIdReturnsMaskedMemberId() throws Exception {
        String memberId = TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        String email = memberId + "@example.com";
        signupWithEmail(memberId, email);

        JsonNode response = postJson("/api/v1/auth/find-id", """
                {"email":"%s"}
                """.formatted(email));

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("masked").asBoolean()).isTrue();
        // 이메일 하나로 남의 아이디를 온전히 얻지 못해야 한다.
        assertThat(data.get("memberId").asString()).isNotEqualTo(memberId).contains("***");
        // 그래도 본인은 알아볼 수 있어야 한다 — 앞뒤가 남는다.
        assertThat(data.get("memberId").asString()).startsWith(memberId.substring(0, 3));
    }

    @Test
    @DisplayName("#17 그 이메일로 가입한 회원이 없으면 2001 이다 — 존재 여부를 감추지 않는다")
    void unknownEmailIsRejectedWith2001() throws Exception {
        JsonNode response = postJson("/api/v1/auth/find-id", """
                {"email":"nobody-%s@example.com"}
                """.formatted(UUID.randomUUID().toString().substring(0, 8)));

        assertThat(resCode(response)).isEqualTo(2001);
    }

    @Test
    @DisplayName("#17 이메일 형식이 어긋나면 9001 이다 — 회원 없음(2001)과 코드가 갈린다")
    void malformedEmailIsRejectedWith9001() throws Exception {
        JsonNode response = postJson("/api/v1/auth/find-id", """
                {"email":"not-an-email"}
                """);

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("#17 이메일 없이 가입한 회원은 빈 이메일 요청으로 찾아지지 않는다")
    void memberWithoutEmailIsNotFoundByBlankEmail() throws Exception {
        // 이메일은 선택 항목이라 비어 있는 회원이 여럿이다. 파생 쿼리를 그대로 썼다면
        // email IS NULL 로 바뀌어 아무 회원이나 걸렸을 자리다.
        createMember();

        JsonNode response = postJson("/api/v1/auth/find-id", """
                {"email":""}
                """);

        assertThat(resCode(response)).isEqualTo(9001);
    }

    private void signupWithEmail(String memberId, String email) throws Exception {
        JsonNode response = postJson("/api/v1/auth/signup", """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s",
                 "nickname":"테스트회원","email":"%s"}
                """.formatted(memberId, TEST_PASSWORD, TEST_PASSWORD, email));
        assertThat(resCode(response)).isEqualTo(200);
    }
}

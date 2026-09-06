package com.dbdomino.moneylog.backend.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US2 — 가입이 규칙대로 막고 통과시키는가.
 *
 * <p>quickstart.md §3 의 시나리오 #9·#11·#12·#13 에 대응한다.
 */
class SignupIT extends AbstractApiIT {

    @Test
    @DisplayName("#9 가입하면 role=3 으로 저장되고 비밀번호는 bcrypt 해시로만 남는다")
    void signupStoresMemberWithHashedPassword() throws Exception {
        String memberId = newMemberId();

        JsonNode response = postJson("/api/v1/auth/signup", signupBody(memberId, TEST_PASSWORD,
                TEST_PASSWORD, """
                , "email": "%s@example.com", "phone": "01012345678", "intro": "안녕하세요"
                """.formatted(memberId)));

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("memberId").asString()).isEqualTo(memberId);
        assertThat(data.get("role").asInt()).isEqualTo(User.ROLE_MEMBER);
        // 응답에 비밀번호가 실릴 자리가 없다(SC-107).
        assertThat(response.toString()).doesNotContain(TEST_PASSWORD);

        Map<String, Object> row = userRow(memberId);
        // JDBC 는 SMALLINT 를 Integer 로 돌려준다. 타입이 아니라 값을 비교한다.
        assertThat(((Number) row.get("role")).shortValue()).isEqualTo(User.ROLE_MEMBER);
        assertThat(row.get("active")).isEqualTo(Boolean.TRUE);
        // bcrypt 해시는 $2a$/$2b$ 로 시작한다. 평문이 저장되면 여기서 걸린다.
        assertThat((String) row.get("pw")).startsWith("$2").isNotEqualTo(TEST_PASSWORD);
        // 본인 가입이라 감사 컬럼은 비어 있다 — tbl_user 만 허용되는 예외다(FR-121).
        assertThat(row.get("created_by")).isNull();
        assertThat(row.get("updated_by")).isNull();
    }

    @Test
    @DisplayName("#11 이미 쓰이는 아이디로 가입하면 2002 다")
    void duplicatedMemberIdIsRejected() throws Exception {
        User existing = createMember();

        JsonNode response = postJson("/api/v1/auth/signup",
                signupBody(existing.getUserId(), TEST_PASSWORD, TEST_PASSWORD, ""));

        assertThat(resCode(response)).isEqualTo(2002);
    }

    @Test
    @DisplayName("#12 이메일 없이 두 명이 가입하면 둘 다 성공한다 — 부분 유니크라 NULL 은 중복이 아니다")
    void twoMembersWithoutEmailBothSucceed() throws Exception {
        JsonNode first = postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), TEST_PASSWORD, TEST_PASSWORD, ""));
        JsonNode second = postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), TEST_PASSWORD, TEST_PASSWORD, ""));

        assertThat(resCode(first)).isEqualTo(200);
        assertThat(resCode(second)).isEqualTo(200);
    }

    @Test
    @DisplayName("#13 비밀번호와 확인이 다르면 2005 다")
    void passwordConfirmMismatchIsRejected() throws Exception {
        JsonNode response = postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), TEST_PASSWORD, "Different1!", ""));

        assertThat(resCode(response)).isEqualTo(2005);
    }

    @Test
    @DisplayName("#13 비밀번호가 규칙을 어기면 2004 다 — 형식 오류(9001)와 코드가 다르다")
    void weakPasswordIsRejectedWith2004() throws Exception {
        // 8자 이상이지만 소문자 한 종류뿐이다(3종류 이상 필요).
        JsonNode response = postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), "abcdefghij", "abcdefghij", ""));

        assertThat(resCode(response)).isEqualTo(2004);
    }

    @Test
    @DisplayName("#36 아이디 형식이 어긋나면 9001 이다 — 비밀번호 규칙(2004)과 갈린다")
    void malformedMemberIdIsRejectedWith9001() throws Exception {
        JsonNode response = postJson("/api/v1/auth/signup",
                signupBody("ab", TEST_PASSWORD, TEST_PASSWORD, ""));

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("#12 이미 쓰이는 이메일로 가입하면 2003 이다")
    void duplicatedEmailIsRejected() throws Exception {
        String email = newMemberId() + "@example.com";
        String optional = ", \"email\": \"" + email + "\"";
        assertThat(resCode(postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), TEST_PASSWORD, TEST_PASSWORD, optional)))).isEqualTo(200);

        JsonNode response = postJson("/api/v1/auth/signup",
                signupBody(newMemberId(), TEST_PASSWORD, TEST_PASSWORD, optional));

        assertThat(resCode(response)).isEqualTo(2003);
    }

    /** 가입 요청 Body. {@code optionalFields} 는 앞에 쉼표를 포함한 조각이거나 빈 문자열이다. */
    private String signupBody(String memberId, String password, String confirm,
                              String optionalFields) {
        return """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"테스트회원"%s}
                """.formatted(memberId, password, confirm, optionalFields);
    }

    private String newMemberId() {
        return TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    private Map<String, Object> userRow(String memberId) {
        return jdbc.queryForMap("""
                select pw, role, active, created_by, updated_by
                  from moneylog.tbl_user
                 where user_id = ?
                """, memberId);
    }
}

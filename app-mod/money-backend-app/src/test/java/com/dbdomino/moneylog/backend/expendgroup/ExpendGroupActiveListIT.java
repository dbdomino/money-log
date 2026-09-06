package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.13 사용 중 지출유형 목록 — quickstart #12.
 *
 * <h2>가입 API 로 회원을 만든다</h2>
 *
 * <p>{@code createMember()} 는 Repository 로 회원 행만 만들므로 <b>기본 지출유형 10종이
 * 생기지 않는다</b>. 이 목록의 대상이 바로 그 10종이라, 여기서는 가입(1.2)을 실제로 불러
 * 유형과 아이콘까지 만들어진 상태에서 확인한다.
 *
 * <h2>등록·수정 API 없이 상태를 만든다</h2>
 *
 * <p>2.7(등록)·2.11(수정)은 US3 에서야 선다. 그래서 {@code in_use}·{@code deleted} 를
 * JDBC 로 직접 바꾼다 — 여기서 검증할 것은 <b>목록의 필터</b>이지 수정 API 의 동작이 아니다.
 *
 * <p>JDBC 갱신은 <b>반드시 트랜잭션 안에서</b> 한다. datasource 가 {@code auto-commit: false}
 * 라 트랜잭션 밖 갱신은 커밋되지 않고 조용히 사라진다.
 */
class ExpendGroupActiveListIT extends AbstractApiIT {

    private static final String ACTIVE_URL = "/api/v1/expend-groups/active";

    /** 가입한 회원. 아이디를 함께 들고 다녀야 그 회원의 유형만 골라 바꿀 수 있다. */
    private record Member(String memberId, String token) {
    }

    /** 가입하고 로그인한다. 기본 지출유형 10종과 아이콘이 함께 생긴다. */
    private Member signupAndLogin() throws Exception {
        String memberId = TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        JsonNode signup = postJson("/api/v1/auth/signup", """
                {"memberId":"%s","password":"%s","passwordConfirm":"%s","nickname":"테스트회원"}
                """.formatted(memberId, TEST_PASSWORD, TEST_PASSWORD));
        assertThat(resCode(signup)).isEqualTo(200);

        JsonNode login = postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(memberId, TEST_PASSWORD));
        assertThat(resCode(login)).isEqualTo(200);
        return new Member(memberId, login.get("data").get("accessToken").asString());
    }

    /**
     * 그 회원의 지출유형 하나에서 참/거짓 컬럼을 바꾼다.
     *
     * <p>컬럼 이름을 문자열로 끼워 넣지만 <b>호출부가 리터럴만 넘긴다</b> — 값은 전부
     * 바인딩한다. 회원은 아이디로 고른다: 다른 테스트가 만든 회원과 섞이면 엉뚱한 행을
     * 고쳐 놓고 필터가 동작한다고 착각하게 된다.
     */
    private void updateGroupFlag(Member member, String name, String column, boolean value) {
        tx.executeWithoutResult(status -> jdbc.update("""
                update moneylog.tbl_user_expend_group set %s = ?
                 where name = ?
                   and id_key = (select id_key from moneylog.tbl_user where user_id = ?)
                """.formatted(column), value, name, member.memberId()));
    }

    private List<String> namesOf(JsonNode listResponse) {
        List<String> names = new ArrayList<>();
        for (JsonNode node : listResponse.get("data").get("list")) {
            names.add(node.get("name").asString());
        }
        return names;
    }

    @Test
    @DisplayName("가입 직후에는 기본 10종이 전부 사용 중 목록에 나온다")
    void defaultsAreAllActive() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        JsonNode list = getJson(ACTIVE_URL, token);

        assertThat(resCode(list)).isEqualTo(200);
        assertThat(namesOf(list)).containsExactly(
                "식비", "교통", "주거", "통신", "쇼핑", "장보기", "의료", "교육", "문화", "기타");
    }

    @Test
    @DisplayName("#12 inUse=false 인 유형은 사용 중 목록에서 빠진다")
    void notInUseIsExcluded() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        updateGroupFlag(member, "문화", "in_use", false);

        assertThat(namesOf(getJson(ACTIVE_URL, token)))
                .doesNotContain("문화")
                .hasSize(9);
    }

    @Test
    @DisplayName("삭제 표시된 유형도 사용 중 목록에서 빠진다 — 조건이 둘이다")
    void deletedIsExcluded() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        updateGroupFlag(member, "의료", "deleted", true);

        assertThat(namesOf(getJson(ACTIVE_URL, token)))
                .doesNotContain("의료")
                .hasSize(9);
    }

    @Test
    @DisplayName("항목에 iconUrl 이 조회 경로로 실리고 defaultGroup 이 true 다")
    void itemCarriesIconUrlAndDefaultFlag() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        JsonNode item = getJson(ACTIVE_URL, token).get("data").get("list").get(0);

        assertThat(item.get("name").asString()).isEqualTo("식비");
        assertThat(item.get("defaultGroup").asBoolean()).isTrue();
        // DB 에는 파일명만 있고 경로 앞부분은 Mapper 가 붙인다.
        assertThat(item.get("iconUrl").asString())
                .startsWith("/api/v1/expend-groups/icons/")
                .endsWith(".png");
    }

    @Test
    @DisplayName("아이콘이 없으면 iconUrl 이 null 이고 필드는 생략되지 않는다(SC-209)")
    void missingIconStaysAsNullField() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        tx.executeWithoutResult(status -> jdbc.update("""
                update moneylog.tbl_user_expend_group set icon_filename = null
                 where name = '식비'
                   and id_key = (select id_key from moneylog.tbl_user where user_id = ?)
                """, member.memberId()));

        JsonNode item = getJson(ACTIVE_URL, token).get("data").get("list").get(0);

        // 필드가 빠지면 프론트의 'iconUrl' in obj 분기가 다른 결과를 낸다.
        assertThat(item.has("iconUrl")).isTrue();
        assertThat(item.get("iconUrl").isNull()).isTrue();
    }

    @Test
    @DisplayName("응답 항목에 inUse·deleted 가 없다 — 필터가 이미 값을 정했다")
    void activeItemsCarryOnlyWhatTheFormNeeds() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();

        JsonNode item = getJson(ACTIVE_URL, token).get("data").get("list").get(0);

        assertThat(item.has("expendGroupId")).isTrue();
        assertThat(item.has("inUse")).isFalse();
        assertThat(item.has("deleted")).isFalse();
    }

    @Test
    @DisplayName("남의 지출유형은 섞이지 않는다")
    void othersGroupsAreNotListed() throws Exception {
        String otherToken = signupAndLogin().token();
        long otherFirstId = getJson(ACTIVE_URL, otherToken)
                .get("data").get("list").get(0).get("expendGroupId").asLong();

        Member member = signupAndLogin();
        String token = member.token();

        for (JsonNode node : getJson(ACTIVE_URL, token).get("data").get("list")) {
            assertThat(node.get("expendGroupId").asLong()).isNotEqualTo(otherFirstId);
        }
    }

    @Test
    @DisplayName("토큰 없이 부르면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        assertThat(resCode(getJson(ACTIVE_URL, null))).isEqualTo(1001);
    }
}

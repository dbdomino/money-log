package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.11 지출유형 수정과 2.8 관리 목록 — quickstart #21·#22·#24.
 *
 * <p><b>#21 과 #22 가 한 쌍이다.</b> 기본 유형은 <b>이름만</b> 잠긴다({@code 3105}) —
 * 사용 여부와 아이콘은 바꿀 수 있어야 한다(SC-208·FR-220). 판정 순서를 "기본 유형인가"
 * 부터 보면 기본 유형이 아무것도 못 바꾸게 되어 #22 가 깨진다. 먼저 볼 것은
 * <b>"{@code name} 을 실제로 보냈는가"</b> 다.
 */
class ExpendGroupUpdateIT extends AbstractExpendGroupIT {

    @Test
    @DisplayName("#21 기본 유형의 이름을 바꾸려 하면 3105 다")
    void renamingDefaultGroupIs3105() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");

        assertThat(resCode(updateGroup(member.token(), id, "밥값", null))).isEqualTo(3105);

        String name = jdbc.queryForObject(
                "select name from moneylog.tbl_user_expend_group where idx = ?", String.class, id);
        assertThat(name).isEqualTo("식비");
    }

    @Test
    @DisplayName("#22 기본 유형이라도 사용 여부는 바꿀 수 있다 — 이름만 잠긴다")
    void togglingInUseOnDefaultGroupSucceeds() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");

        JsonNode response = updateGroup(member.token(), id, null, false);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("inUse").asBoolean()).isFalse();
        assertThat(response.get("data").get("defaultGroup").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("#22 사용 여부를 끈 기본 유형은 사용 중 목록(2.13)에서 빠진다")
    void disabledDefaultGroupLeavesActiveList() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");
        assertThat(resCode(updateGroup(member.token(), id, null, false))).isEqualTo(200);

        for (JsonNode node : getJson(URL + "/active", member.token()).get("data").get("list")) {
            assertThat(node.get("name").asString()).isNotEqualTo("식비");
        }
    }

    @Test
    @DisplayName("사용자 유형은 이름을 바꿀 수 있다")
    void renamingUserGroupSucceeds() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));

        JsonNode response = updateGroup(token, id, "여가", null);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("name").asString()).isEqualTo("여가");
    }

    @Test
    @DisplayName("#24 관리 목록(2.8)은 삭제분을 포함하고 각 행에 deleted 가 있다")
    void managementListIncludesDeletedRows() throws Exception {
        String token = signupAndLogin().token();
        long alive = idOf(createGroup(token, "취미", true));
        long removed = idOf(createGroup(token, "옛 취미", true));
        assertThat(resCode(deleteJson(URL + "/" + removed, token))).isEqualTo(200);

        JsonNode list = getJson(URL, token);

        assertThat(resCode(list)).isEqualTo(200);
        boolean sawAlive = false;
        boolean sawRemoved = false;
        for (JsonNode node : list.get("data").get("list")) {
            assertThat(node.has("deleted")).isTrue();
            if (node.get("expendGroupId").asLong() == alive) {
                sawAlive = true;
                assertThat(node.get("deleted").asBoolean()).isFalse();
            }
            if (node.get("expendGroupId").asLong() == removed) {
                sawRemoved = true;
                assertThat(node.get("deleted").asBoolean()).isTrue();
            }
        }
        assertThat(sawAlive).isTrue();
        assertThat(sawRemoved).isTrue();
        // 기본 10종 + 사용자 2종.
        assertThat(list.get("data").get("list").size()).isEqualTo(12);
    }

    @Test
    @DisplayName("#24 관리 목록의 data 에는 list 만 있다 — totalCount 를 싣지 않는다")
    void managementListCarriesOnlyList() throws Exception {
        String token = signupAndLogin().token();

        JsonNode data = getJson(URL, token).get("data");

        assertThat(data.has("list")).isTrue();
        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("2.9 상세는 삭제 표시된 유형도 돌려준다")
    void detailIncludesDeletedGroup() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));
        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        JsonNode detail = getJson(URL + "/" + id, token);

        assertThat(resCode(detail)).isEqualTo(200);
        assertThat(detail.get("data").get("deleted").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("삭제 표시된 유형도 수정된다 — 삭제는 읽기 전용이 아니다")
    void deletedGroupIsStillUpdatable() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));
        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        JsonNode response = updateGroup(token, id, "옛 취미", null);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("name").asString()).isEqualTo("옛 취미");
        assertThat(response.get("data").get("deleted").asBoolean()).isTrue();
    }
}

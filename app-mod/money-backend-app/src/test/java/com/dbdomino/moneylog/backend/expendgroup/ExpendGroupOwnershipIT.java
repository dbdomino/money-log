package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 남의 지출유형에는 손댈 수 없다 — SC-207 의 지출유형 쪽.
 *
 * <p>{@code PaymentMethodOwnershipIT}({@code 3003})와 한 쌍이다. 둘이 함께 있어야
 * SC-207("수단 {@code 3003}, 지출유형 {@code 3103} 100% 거부")이 검증된다.
 *
 * <p><b>"없는 ID"와 "남의 ID"가 같은 {@code 3103} 이어야 한다</b>(FR-201). 코드가 갈리면
 * ID 를 훑는 것만으로 남의 자원이 존재한다는 사실이 새어 나간다.
 */
class ExpendGroupOwnershipIT extends AbstractExpendGroupIT {

    /** 남이 가진 지출유형 1건을 만들고 PK 를 돌려준다. */
    private long createOthersGroup() throws Exception {
        String ownerToken = signupAndLogin().token();
        return idOf(createGroup(ownerToken, "남의취미", true));
    }

    @Test
    @DisplayName("남의 지출유형 상세 조회는 3103 이다")
    void readingOthersIs3103() throws Exception {
        long othersId = createOthersGroup();
        String intruder = signupAndLogin().token();

        assertThat(resCode(getJson(URL + "/" + othersId, intruder))).isEqualTo(3103);
    }

    @Test
    @DisplayName("남의 지출유형 수정은 3103 이다")
    void updatingOthersIs3103() throws Exception {
        long othersId = createOthersGroup();
        String intruder = signupAndLogin().token();

        assertThat(resCode(updateGroup(intruder, othersId, "바꿔치기", null))).isEqualTo(3103);

        String name = jdbc.queryForObject(
                "select name from moneylog.tbl_user_expend_group where idx = ?",
                String.class, othersId);
        assertThat(name).isEqualTo("남의취미");
    }

    @Test
    @DisplayName("남의 지출유형 삭제는 3103 이다")
    void deletingOthersIs3103() throws Exception {
        long othersId = createOthersGroup();
        String intruder = signupAndLogin().token();

        assertThat(resCode(deleteJson(URL + "/" + othersId, intruder))).isEqualTo(3103);

        Boolean deleted = jdbc.queryForObject(
                "select deleted from moneylog.tbl_user_expend_group where idx = ?",
                Boolean.class, othersId);
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("없는 ID 도 같은 3103 이다 — 코드가 갈리면 존재 여부가 새어 나간다")
    void missingIdIsAlso3103() throws Exception {
        String token = signupAndLogin().token();

        assertThat(resCode(getJson(URL + "/999999999", token))).isEqualTo(3103);
        assertThat(resCode(updateGroup(token, 999999999L, "이름", null))).isEqualTo(3103);
        assertThat(resCode(deleteJson(URL + "/999999999", token))).isEqualTo(3103);
    }

    @Test
    @DisplayName("남의 기본 유형이라도 3105 가 아니라 3103 이다 — 소유자 판정이 먼저다")
    void othersDefaultGroupIs3103NotThe3105() throws Exception {
        Member owner = signupAndLogin();
        long othersDefault = defaultGroupId(owner, "식비");
        String intruder = signupAndLogin().token();

        // 3105 가 나오면 "그 ID 가 남의 기본 유형이다"까지 알려 주는 셈이 된다.
        assertThat(resCode(updateGroup(intruder, othersDefault, "밥값", null))).isEqualTo(3103);
    }

    @Test
    @DisplayName("관리 목록(2.8)에는 남의 유형이 섞이지 않는다")
    void listShowsOnlyOwnGroups() throws Exception {
        long othersId = createOthersGroup();
        String token = signupAndLogin().token();

        for (JsonNode node : getJson(URL, token).get("data").get("list")) {
            assertThat(node.get("expendGroupId").asLong()).isNotEqualTo(othersId);
        }
    }
}

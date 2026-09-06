package com.dbdomino.moneylog.backend.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 남의 수단에는 손댈 수 없다 — quickstart #9 (SC-207).
 *
 * <p><b>"없는 ID"와 "남의 ID"가 같은 코드({@code 3003})여야 한다</b>(FR-201). 코드가 갈리면
 * ID 를 훑는 것만으로 "이 번호의 수단이 존재한다"를 알아낼 수 있다 — 남의 자원 개수와
 * 등록 시점이 새어 나간다.
 */
class PaymentMethodOwnershipIT extends AbstractApiIT {

    private static final String URL = "/api/v1/payment-methods";

    /** 남이 가진 수단 1건을 만들고 그 PK 를 돌려준다. */
    private long createOthersMethod() throws Exception {
        String ownerToken = login(createMember()).accessToken();
        JsonNode response = postJson(URL, ownerToken, """
                {"name":"남의카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("paymentMethodId").asLong();
    }

    @Test
    @DisplayName("#9 남의 수단 상세 조회는 3003 이다")
    void readingOthersIs3003() throws Exception {
        long othersId = createOthersMethod();
        String intruder = login(createMember()).accessToken();

        assertThat(resCode(getJson(URL + "/" + othersId, intruder))).isEqualTo(3003);
    }

    @Test
    @DisplayName("#9 남의 수단 수정은 3003 이다")
    void updatingOthersIs3003() throws Exception {
        long othersId = createOthersMethod();
        String intruder = login(createMember()).accessToken();

        JsonNode response = patchJson(URL + "/" + othersId, intruder, """
                {"name":"바꿔치기"}
                """);

        assertThat(resCode(response)).isEqualTo(3003);

        // 거절만으로는 부족하다 — 값이 바뀌지 않았는지 DB 로 확인한다.
        String name = jdbc.queryForObject(
                "select name from moneylog.tbl_user_payment_method where idx = ?",
                String.class, othersId);
        assertThat(name).isEqualTo("남의카드");
    }

    @Test
    @DisplayName("#9 남의 수단 삭제는 3003 이다")
    void deletingOthersIs3003() throws Exception {
        long othersId = createOthersMethod();
        String intruder = login(createMember()).accessToken();

        assertThat(resCode(deleteJson(URL + "/" + othersId, intruder))).isEqualTo(3003);

        Boolean deleted = jdbc.queryForObject(
                "select deleted from moneylog.tbl_user_payment_method where idx = ?",
                Boolean.class, othersId);
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("없는 ID 도 3003 이다 — 남의 것과 코드가 갈리면 존재 여부가 새어 나간다")
    void missingIdIsAlso3003() throws Exception {
        String token = login(createMember()).accessToken();

        assertThat(resCode(getJson(URL + "/999999999", token))).isEqualTo(3003);
        assertThat(resCode(deleteJson(URL + "/999999999", token))).isEqualTo(3003);
    }

    @Test
    @DisplayName("관리 목록(2.2)에는 남의 수단이 섞이지 않는다")
    void listShowsOnlyOwnMethods() throws Exception {
        long othersId = createOthersMethod();
        String token = login(createMember()).accessToken();
        postJson(URL, token, """
                {"name":"내카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);

        JsonNode list = getJson(URL, token);

        assertThat(resCode(list)).isEqualTo(200);
        for (JsonNode node : list.get("data").get("list")) {
            assertThat(node.get("paymentMethodId").asLong()).isNotEqualTo(othersId);
        }
    }
}

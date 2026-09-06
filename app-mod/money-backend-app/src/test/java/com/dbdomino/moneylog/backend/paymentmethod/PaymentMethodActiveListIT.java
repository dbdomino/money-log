package com.dbdomino.moneylog.backend.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.6 사용 중 수단 목록 — quickstart #7·#11·#13.
 *
 * <p>이 목록이 <b>세 조건을 모두</b> 거는지가 핵심이다(FR-207). 하나만 빠져도 지출 입력
 * 화면에 소득 수단이나 삭제된 수단이 섞여 나오고, 그렇게 등록된 지출은 나중에 월별 집계와
 * 통계에서 걸러 낼 방법이 없다.
 */
class PaymentMethodActiveListIT extends AbstractApiIT {

    private static final String URL = "/api/v1/payment-methods";
    private static final String ACTIVE_EXPENSE = URL + "/active/EXPENSE";

    /** 수단 1건을 등록하고 PK 를 돌려준다. */
    private long create(String token, String name, String purpose, boolean inUse) throws Exception {
        JsonNode response = postJson(URL, token, """
                {"name":"%s","type":"CARD","purpose":"%s","inUse":%s,"cardExpiry":"2028-12"}
                """.formatted(name, purpose, inUse));
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("paymentMethodId").asLong();
    }

    /** 목록에 실린 이름들. */
    private java.util.List<String> namesOf(JsonNode listResponse) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonNode node : listResponse.get("data").get("list")) {
            names.add(node.get("name").asString());
        }
        return names;
    }

    @Test
    @DisplayName("#7 삭제 표시된 수단은 사용 중 목록에서 빠진다")
    void deletedIsExcluded() throws Exception {
        String token = login(createMember()).accessToken();
        long id = create(token, "옛 카드", "EXPENSE", true);

        assertThat(namesOf(getJson(ACTIVE_EXPENSE, token))).contains("옛 카드");

        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        // 관리 목록(2.2)에는 남고 여기서만 빠진다 — 두 목록의 차이가 이 시나리오다.
        assertThat(namesOf(getJson(ACTIVE_EXPENSE, token))).doesNotContain("옛 카드");
        assertThat(namesOf(getJson(URL, token))).contains("옛 카드");
    }

    @Test
    @DisplayName("#11 지출용·소득용·미사용·삭제된 수단 4건 중 EXPENSE 사용 중 목록은 정확히 1건이다")
    void allFourConditionsApply() throws Exception {
        String token = login(createMember()).accessToken();

        create(token, "살아있는지출카드", "EXPENSE", true);   // 유일하게 남아야 하는 것
        create(token, "소득카드", "INCOME", true);            // purpose 불일치
        create(token, "미사용카드", "EXPENSE", false);        // in_use=false
        long deletedId = create(token, "삭제카드", "EXPENSE", true);
        assertThat(resCode(deleteJson(URL + "/" + deletedId, token))).isEqualTo(200);

        JsonNode list = getJson(ACTIVE_EXPENSE, token);

        assertThat(resCode(list)).isEqualTo(200);
        // 건수만 세지 않는다 — 엉뚱한 1건이 남아도 건수는 맞기 때문이다.
        assertThat(namesOf(list)).containsExactly("살아있는지출카드");
    }

    @Test
    @DisplayName("#11 INCOME 목록에는 소득용 수단만 나온다")
    void incomeListHasOnlyIncomeMethods() throws Exception {
        String token = login(createMember()).accessToken();
        create(token, "살아있는지출카드", "EXPENSE", true);
        create(token, "소득카드", "INCOME", true);

        assertThat(namesOf(getJson(URL + "/active/INCOME", token)))
                .containsExactly("소득카드");
    }

    @Test
    @DisplayName("#13 purpose 가 허용 값 밖이면 3001 이다")
    void invalidPurposeIs3001() throws Exception {
        String token = login(createMember()).accessToken();

        assertThat(resCode(getJson(URL + "/active/BOTH", token))).isEqualTo(3001);
        // 소문자도 허용 값이 아니다 — DB CHECK 이 대문자만 받는다.
        assertThat(resCode(getJson(URL + "/active/expense", token))).isEqualTo(3001);
    }

    @Test
    @DisplayName("응답 항목에 purpose·inUse·deleted 가 없다 — 필터가 이미 값을 정했다")
    void activeItemsCarryOnlyWhatTheFormNeeds() throws Exception {
        String token = login(createMember()).accessToken();
        create(token, "국민카드", "EXPENSE", true);

        JsonNode item = getJson(ACTIVE_EXPENSE, token).get("data").get("list").get(0);

        assertThat(item.has("paymentMethodId")).isTrue();
        assertThat(item.has("name")).isTrue();
        assertThat(item.has("type")).isTrue();
        assertThat(item.has("cardExpiry")).isTrue();
        assertThat(item.has("purpose")).isFalse();
        assertThat(item.has("inUse")).isFalse();
        assertThat(item.has("deleted")).isFalse();
    }

    @Test
    @DisplayName("남의 수단은 사용 중 목록에도 섞이지 않는다")
    void othersMethodsAreNotListed() throws Exception {
        String otherToken = login(createMember()).accessToken();
        create(otherToken, "남의카드", "EXPENSE", true);
        String token = login(createMember()).accessToken();

        assertThat(namesOf(getJson(ACTIVE_EXPENSE, token))).doesNotContain("남의카드");
    }

    @Test
    @DisplayName("토큰 없이 부르면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        assertThat(resCode(getJson(ACTIVE_EXPENSE, null))).isEqualTo(1001);
    }
}

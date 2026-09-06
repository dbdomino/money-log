package com.dbdomino.moneylog.backend.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.1 지출·소득 수단 등록 — quickstart #1~#5.
 *
 * <p>{@code @DisplayName} 의 {@code #N} 은 quickstart.md §3 의 시나리오 번호와 1:1 이다.
 * 번호가 어긋나면 "어느 시나리오가 깨졌는지"를 문서에서 되짚을 수 없다.
 */
class PaymentMethodCreateIT extends AbstractApiIT {

    private static final String URL = "/api/v1/payment-methods";

    @Test
    @DisplayName("#1 CARD+EXPENSE 로 등록하면 본인 소유로 저장되고 deleted=false 다")
    void createCardExpense() throws Exception {
        User user = createMember();
        String token = login(user).accessToken();

        JsonNode response = postJson(URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true,"cardExpiry":"2028-12"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("name").asString()).isEqualTo("국민카드");
        assertThat(data.get("type").asString()).isEqualTo("CARD");
        assertThat(data.get("purpose").asString()).isEqualTo("EXPENSE");
        assertThat(data.get("cardExpiry").asString()).isEqualTo("2028-12");
        assertThat(data.get("deleted").asBoolean()).isFalse();

        // 소유자는 응답에 실리지 않으므로 DB 로 확인한다 — 응답만 보면 남의 것으로 저장돼도 모른다.
        Long owner = jdbc.queryForObject(
                "select id_key from moneylog.tbl_user_payment_method where idx = ?",
                Long.class, data.get("paymentMethodId").asLong());
        assertThat(owner).isEqualTo(user.getIdKey());
    }

    @Test
    @DisplayName("#2 ACCOUNT 로 등록하면 cardExpiry 가 null 이다")
    void accountHasNoCardExpiry() throws Exception {
        String token = login(createMember()).accessToken();

        // 계좌인데 유효기간을 실어 보낸다. 서버가 버려야 한다(FR-204).
        JsonNode response = postJson(URL, token, """
                {"name":"월급통장","type":"ACCOUNT","purpose":"INCOME","inUse":true,"cardExpiry":"2028-12"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("cardExpiry").isNull()).isTrue();
    }

    @Test
    @DisplayName("#3 type 이 허용 값 밖이면 3001 이다")
    void invalidTypeIs3001() throws Exception {
        String token = login(createMember()).accessToken();

        JsonNode response = postJson(URL, token, """
                {"name":"현금","type":"CASH","purpose":"EXPENSE","inUse":true}
                """);

        // 9001(Bean Validation)이 아니라 3001 이어야 한다 — 명세가 값 오류를 따로 가른다.
        assertThat(resCode(response)).isEqualTo(3001);
    }

    @Test
    @DisplayName("#4 purpose 가 허용 값 밖이면 3001 이다")
    void invalidPurposeIs3001() throws Exception {
        String token = login(createMember()).accessToken();

        JsonNode response = postJson(URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"BOTH","inUse":true}
                """);

        assertThat(resCode(response)).isEqualTo(3001);
    }

    @Test
    @DisplayName("#5 cardExpiry 가 YYYY-MM 이 아니면 3002 다")
    void invalidCardExpiryIs3002() throws Exception {
        String token = login(createMember()).accessToken();

        JsonNode response = postJson(URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true,"cardExpiry":"2028/12"}
                """);

        assertThat(resCode(response)).isEqualTo(3002);
    }

    @Test
    @DisplayName("#5 월이 13 이면 형식이 맞아도 3002 다")
    void monthOutOfRangeIs3002() throws Exception {
        String token = login(createMember()).accessToken();

        JsonNode response = postJson(URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true,"cardExpiry":"2028-13"}
                """);

        // card_expiry 가 CHAR(7)이라 길이만 보면 통과한다. 값 검증이 없으면 DB 에 그대로 들어간다.
        assertThat(resCode(response)).isEqualTo(3002);
    }

    @Test
    @DisplayName("소유자는 토큰이 정한다 — Body 에 남의 idKey 를 실어도 무시된다(FR-201)")
    void ownerComesFromTokenNotBody() throws Exception {
        User victim = createMember();
        User attacker = createMember();
        String token = login(attacker).accessToken();

        // idKey·memberId 는 등록 요청에 없는 필드다. 조용히 무시되지 않고 9001 로 막혀야
        // 하지만, 어느 쪽이든 victim 소유로 저장되어서는 안 된다.
        JsonNode response = postJson(URL, token, """
                {"name":"탈취카드","type":"CARD","purpose":"EXPENSE","inUse":true,
                 "idKey":%d,"memberId":"%s"}
                """.formatted(victim.getIdKey(), victim.getUserId()));

        Integer stolen = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_payment_method where id_key = ?",
                Integer.class, victim.getIdKey());
        assertThat(stolen).isZero();

        if (resCode(response) == 200) {
            Long owner = jdbc.queryForObject(
                    "select id_key from moneylog.tbl_user_payment_method where idx = ?",
                    Long.class, response.get("data").get("paymentMethodId").asLong());
            assertThat(owner).isEqualTo(attacker.getIdKey());
        }
    }

    @Test
    @DisplayName("필수 필드를 빠뜨리면 9001 이다")
    void missingRequiredFieldIs9001() throws Exception {
        String token = login(createMember()).accessToken();

        JsonNode response = postJson(URL, token, """
                {"type":"CARD","purpose":"EXPENSE","inUse":true}
                """);

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("토큰 없이 등록하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        JsonNode response = postJson(URL, null, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);

        assertThat(resCode(response)).isEqualTo(1001);
    }
}

package com.dbdomino.moneylog.backend.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.5 수단 삭제 — quickstart #6·#8·#10.
 *
 * <p>삭제는 <b>표시</b>다. 이 클래스가 확인하는 것은 "행이 남는가"이며, 행이 실제로 지워지면
 * 과거 지출·소득의 FK 참조가 끊긴다(FR-206).
 *
 * <p><b>#7(삭제 후 사용 중 목록에서 빠진다)은 여기 없다.</b> 그 시나리오는 2.6 을 부르는데
 * 2.6 은 US2 에서 생긴다 — 지금 두면 API 가 없어 404 로 실패한다.
 */
class PaymentMethodDeleteIT extends AbstractApiIT {

    private static final String URL = "/api/v1/payment-methods";

    /** 수단 1건을 등록하고 그 PK 를 돌려준다. */
    private long createMethod(String token, String name) throws Exception {
        JsonNode response = postJson(URL, token, """
                {"name":"%s","type":"CARD","purpose":"EXPENSE","inUse":true,"cardExpiry":"2028-12"}
                """.formatted(name));
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("paymentMethodId").asLong();
    }

    @Test
    @DisplayName("#6 삭제해도 관리 목록(2.2)에 deleted=true 로 남는다")
    void deletedStaysInManagementList() throws Exception {
        String token = login(createMember()).accessToken();
        long id = createMethod(token, "옛 카드");

        JsonNode deleted = deleteJson(URL + "/" + id, token);
        assertThat(resCode(deleted)).isEqualTo(200);
        assertThat(deleted.get("data").get("deleted").asBoolean()).isTrue();
        assertThat(deleted.get("data").get("paymentMethodId").asLong()).isEqualTo(id);

        // 행이 남아 있어야 한다 — 물리 삭제였다면 여기서 0 이 된다.
        Integer rows = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_user_payment_method where idx = ?",
                Integer.class, id);
        assertThat(rows).isEqualTo(1);

        JsonNode list = getJson(URL, token);
        assertThat(resCode(list)).isEqualTo(200);
        JsonNode entry = findById(list, id);
        assertThat(entry).isNotNull();
        assertThat(entry.get("deleted").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("#8 이미 삭제된 수단을 다시 삭제하면 3004 다")
    void deletingTwiceIs3004() throws Exception {
        String token = login(createMember()).accessToken();
        long id = createMethod(token, "옛 카드");

        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        // 멱등 성공으로 흘리면 화면이 "방금 지웠다"와 "이미 지워져 있었다"를 구분할 수 없다.
        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(3004);
    }

    @Test
    @DisplayName("#10 삭제 표시된 수단도 수정된다 — 삭제는 읽기 전용이 아니다")
    void deletedMethodIsStillUpdatable() throws Exception {
        String token = login(createMember()).accessToken();
        long id = createMethod(token, "옛 카드");
        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        JsonNode updated = patchJson(URL + "/" + id, token, """
                {"name":"옛 카드(정리됨)"}
                """);

        assertThat(resCode(updated)).isEqualTo(200);
        assertThat(updated.get("data").get("name").asString()).isEqualTo("옛 카드(정리됨)");
        // 수정해도 삭제 표시는 풀리지 않는다.
        assertThat(updated.get("data").get("deleted").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("삭제 표시된 수단도 상세 조회(2.3)된다 — 관리 화면이 본다")
    void deletedMethodIsStillReadable() throws Exception {
        String token = login(createMember()).accessToken();
        long id = createMethod(token, "옛 카드");
        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        JsonNode detail = getJson(URL + "/" + id, token);

        assertThat(resCode(detail)).isEqualTo(200);
        assertThat(detail.get("data").get("deleted").asBoolean()).isTrue();
    }

    /** 목록에서 PK 가 일치하는 항목. 없으면 {@code null} 이다. */
    private JsonNode findById(JsonNode listResponse, long id) {
        for (JsonNode node : listResponse.get("data").get("list")) {
            if (node.get("paymentMethodId").asLong() == id) {
                return node;
            }
        }
        return null;
    }
}

package com.dbdomino.moneylog.backend.income;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 소득의 소유권과 수정·삭제 — quickstart #18·#19 (SC-307).
 *
 * <p><b>지출의 {@code 3202} 와 섞이지 않는지</b>가 이 클래스의 두 번째 목적이다. 두 자원이
 * 같은 구조를 갖지만 코드 대역이 갈리며(api-contract.md §7), 한쪽 서비스를 베껴 만들면
 * 코드까지 함께 따라온다.
 */
class IncomeOwnershipIT extends AbstractIncomeIT {

    @Test
    @DisplayName("#19 남의 소득 상세 조회는 3302 다 — 3202 가 아니다")
    void readingOthersIs3302() throws Exception {
        Fixture owner = prepare();
        long othersId = createIncome(owner);
        Fixture intruder = prepare();

        assertThat(resCode(get(intruder, othersId))).isEqualTo(3302);
    }

    @Test
    @DisplayName("#19 남의 소득 수정은 3302 이고 값도 바뀌지 않는다")
    void updatingOthersIs3302() throws Exception {
        Fixture owner = prepare();
        long othersId = createIncome(owner);
        Fixture intruder = prepare();

        assertThat(resCode(patchJson(URL + "/" + othersId, intruder.token(), """
                {"amount":999999}
                """))).isEqualTo(3302);

        assertThat(row(othersId).get("amount")).isEqualTo(3000000L);
    }

    @Test
    @DisplayName("#19 남의 소득 삭제는 3302 이고 행도 남는다")
    void deletingOthersIs3302() throws Exception {
        Fixture owner = prepare();
        long othersId = createIncome(owner);
        Fixture intruder = prepare();

        assertThat(resCode(deleteJson(URL + "/" + othersId, intruder.token()))).isEqualTo(3302);

        assertThat(countIncomes(owner.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("#19 없는 ID 도 같은 3302 다")
    void missingIdIsAlso3302() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(get(fixture, 999999999L))).isEqualTo(3302);
        assertThat(resCode(patchJson(URL + "/999999999", fixture.token(), """
                {"amount":1000}
                """))).isEqualTo(3302);
        assertThat(resCode(deleteJson(URL + "/999999999", fixture.token()))).isEqualTo(3302);
    }

    @Test
    @DisplayName("지출 ID 로 소득 API 를 불러도 3302 다 — 자원이 갈린다")
    void expenseIdIsNotAnIncomeId() throws Exception {
        Fixture fixture = prepare();
        long groupId = defaultGroupId(fixture.member(), "식비");
        long expenseMethodId = createExpensePaymentMethod(fixture.token(), "국민카드");
        JsonNode expense = postJson("/api/v1/expenses", fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """.formatted(expenseMethodId, groupId));
        long expenseId = expense.get("data").get("expenseId").asLong();

        // 두 테이블이 각자 시퀀스를 쓰므로 ID 가 겹칠 수 있다. 자원이 다르면 못 찾아야 한다.
        assertThat(resCode(get(fixture, expenseId))).isEqualTo(3302);
    }

    @Test
    @DisplayName("#18 소득 수단을 바꾸면 이름 스냅샷이 갱신된다")
    void changingTheReferenceRefreshesTheSnapshot() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);
        long newMethodId = createIncomePaymentMethod(fixture.token(), "부수입통장");

        JsonNode response = patchJson(URL + "/" + incomeId, fixture.token(), """
                {"paymentMethodId":%d}
                """.formatted(newMethodId));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("paymentMethodId").asLong()).isEqualTo(newMethodId);
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("부수입통장");
        assertThat(row(incomeId).get("payment_method_name")).isEqualTo("부수입통장");
    }

    @Test
    @DisplayName("#18 수단을 omit 하면 스냅샷이 그대로다 — 지출과 같은 규칙이다")
    void omittingTheReferenceKeepsTheSnapshot() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                {"name":"월급통장(주)"}
                """))).isEqualTo(200);

        JsonNode response = patchJson(URL + "/" + incomeId, fixture.token(), """
                {"amount":3500000}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("월급통장");
    }

    @Test
    @DisplayName("content 에 null 을 보내면 비운다 — omit 과 다르다")
    void sendingNullContentClearsIt() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);
        assertThat(row(incomeId).get("content")).isEqualTo("급여");

        JsonNode response = patchJson(URL + "/" + incomeId, fixture.token(), """
                {"content":null}
                """);

        // 지출의 content 는 NOT NULL 이라 이 구분이 없다. 소득에서만 실제로 쓰인다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(row(incomeId).get("content")).isNull();
        assertThat(response.get("data").get("content").isNull()).isTrue();
    }

    @Test
    @DisplayName("content 를 omit 하면 기존 값이 그대로다")
    void omittingContentKeepsIt() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);

        assertThat(resCode(patchJson(URL + "/" + incomeId, fixture.token(), """
                {"amount":3500000}
                """))).isEqualTo(200);

        assertThat(row(incomeId).get("content")).isEqualTo("급여");
    }

    @Test
    @DisplayName("삭제는 물리 삭제이며 재삭제는 3302 다")
    void deleteIsPhysical() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);

        JsonNode deleted = deleteJson(URL + "/" + incomeId, fixture.token());
        assertThat(resCode(deleted)).isEqualTo(200);
        assertThat(deleted.get("data").get("incomeId").asLong()).isEqualTo(incomeId);
        assertThat(deleted.get("data").get("message").asString()).isNotBlank();
        assertThat(deleted.get("data").has("deleted")).isFalse();

        assertThat(countIncomes(fixture.member())).isZero();
        assertThat(resCode(deleteJson(URL + "/" + incomeId, fixture.token()))).isEqualTo(3302);
    }

    @Test
    @DisplayName("수정에서 값이 잘못되면 3301 이다")
    void invalidValueOnUpdateIs3301() throws Exception {
        Fixture fixture = prepare();
        long incomeId = createIncome(fixture);

        assertThat(resCode(patchJson(URL + "/" + incomeId, fixture.token(), """
                {"amount":0}
                """))).isEqualTo(3301);
        assertThat(resCode(patchJson(URL + "/" + incomeId, fixture.token(), """
                {"paymentDate":"2026-02-30"}
                """))).isEqualTo(3301);
    }
}

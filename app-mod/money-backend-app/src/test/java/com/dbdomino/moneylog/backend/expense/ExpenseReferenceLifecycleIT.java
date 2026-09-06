package com.dbdomino.moneylog.backend.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 참조 검증의 비대칭 — quickstart #3·#9·#11 (FR-325 ↔ FR-326).
 *
 * <table border="1">
 *   <caption>참조 대상이 {@code in_use=false} 이거나 {@code deleted=true} 일 때</caption>
 *   <tr><th>경로</th><th>결과</th></tr>
 *   <tr><td>새로 참조를 건다</td><td>거절 — {@code 3003}·{@code 3103}</td></tr>
 *   <tr><td>이미 저장된 행을 조회·수정·삭제</td><td><b>정상 동작</b></td></tr>
 * </table>
 *
 * <p><b>#9 가 함정이다.</b> 수정 경로에서 참조 검증을 무조건 돌리면 죽은 수단을 쓰던 과거
 * 지출을 <b>영영 못 고치게</b> 된다. 참조를 안 보낸 수정은 검증할 것이 없다.
 *
 * <p>이 비대칭이 없으면 003 의 삭제 표시가 004 를 망가뜨린다 — 수단을 삭제 표시하는 순간
 * 그 수단으로 적은 과거 지출 전부가 손댈 수 없게 되고, 003 이 물리 삭제 대신 삭제 표시를
 * 고른 이유("과거 기록 보존")가 무너진다.
 */
class ExpenseReferenceLifecycleIT extends AbstractExpenseIT {

    /** 그 수단을 삭제 표시한다(003 의 2.5). */
    private void softDeletePaymentMethod(Fixture fixture) throws Exception {
        assertThat(resCode(deleteJson(
                "/api/v1/payment-methods/" + fixture.paymentMethodId(), fixture.token())))
                .isEqualTo(200);
    }

    @Test
    @DisplayName("#3 수단을 삭제 표시해도 그 지출은 정상 조회되고 이름도 남는다")
    void expensesSurviveTheirPaymentMethodBeingDeleted() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        softDeletePaymentMethod(fixture);

        JsonNode response = get(fixture, expenseId);
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(response.get("data").get("paymentMethodId").asLong())
                .isEqualTo(fixture.paymentMethodId());
    }

    @Test
    @DisplayName("#9 삭제 표시된 수단을 쓰던 지출의 금액만 수정하면 성공한다")
    void updatingAmountSucceedsEvenWhenTheReferenceIsDead() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        softDeletePaymentMethod(fixture);

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"amount":45000,"content":"저녁"}
                """);

        // 참조를 안 보냈으므로 검증할 것이 없다. 3003 이 나오면 무조건 검증하는 구현이다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("amount").asLong()).isEqualTo(45000L);
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#9 사용 안 함으로 바뀐 지출유형을 쓰던 지출도 금액 수정이 된다")
    void updatingAmountSucceedsWhenTheExpendGroupIsDisabled() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        long groupId = fixture.expendGroupId();
        tx.executeWithoutResult(status -> jdbc.update(
                "update moneylog.tbl_user_expend_group set in_use = false where idx = ?", groupId));

        assertThat(resCode(patchJson(URL + "/" + expenseId, fixture.token(), """
                {"amount":45000}
                """))).isEqualTo(200);
    }

    @Test
    @DisplayName("#9 죽은 참조를 쓰던 지출도 삭제할 수 있다")
    void deletingSucceedsEvenWhenTheReferenceIsDead() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        softDeletePaymentMethod(fixture);

        assertThat(resCode(deleteJson(URL + "/" + expenseId, fixture.token()))).isEqualTo(200);
    }

    @Test
    @DisplayName("#11 삭제 후 재조회하면 없다 — 물리 삭제다(FR-308)")
    void deleteIsPhysical() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        JsonNode deleted = deleteJson(URL + "/" + expenseId, fixture.token());
        assertThat(resCode(deleted)).isEqualTo(200);
        assertThat(deleted.get("data").get("expenseId").asLong()).isEqualTo(expenseId);
        assertThat(deleted.get("data").get("message").asString()).isNotBlank();

        // 003 의 삭제 표시와 정반대다 — 행 자체가 사라진다.
        assertThat(countExpenses(fixture.member())).isZero();
        assertThat(resCode(get(fixture, expenseId))).isEqualTo(3202);
    }

    @Test
    @DisplayName("#11 이미 지운 지출을 다시 지우면 3202 다 — '이미 삭제됨' 코드가 없다")
    void deletingTwiceIs3202() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        assertThat(resCode(deleteJson(URL + "/" + expenseId, fixture.token()))).isEqualTo(200);
        // 003 은 3004·3108 로 "이미 삭제됨"을 구분하지만 004 는 행이 없어 그럴 수 없다.
        assertThat(resCode(deleteJson(URL + "/" + expenseId, fixture.token()))).isEqualTo(3202);
    }

    @Test
    @DisplayName("삭제 응답에 deleted 필드가 없다 — 물리 삭제라 의미를 갖지 않는다")
    void deleteResponseHasNoDeletedFlag() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        JsonNode data = deleteJson(URL + "/" + expenseId, fixture.token()).get("data");

        assertThat(data.has("deleted")).isFalse();
        assertThat(data.size()).isEqualTo(2);
    }
}

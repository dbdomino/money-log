package com.dbdomino.moneylog.backend.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 이름 스냅샷의 갱신 조건 — quickstart #2·#6·#7·#8 (FR-304·SC-305).
 *
 * <p><b>#6·#7·#8 이 한 묶음이며 세 갈래를 전부 덮는다.</b>
 *
 * <pre>{@code
 * 요청이 paymentMethodId 를 보냈는가?
 *   ├ 아니오(omit)        → 스냅샷 그대로   (#7)
 *   ├ 예, 값이 기존과 같음 → 스냅샷 그대로   (#8)
 *   └ 예, 값이 다름        → 새 수단의 현재 이름으로 갱신 (#6)
 * }</pre>
 *
 * <p><b>#7·#8 을 빠뜨리면</b> "수정 요청이 왔으니 최신화한다"는 잘못된 구현이 그대로
 * 통과한다. 그 구현에서는 같은 수단을 유지한 채 금액만 고쳤을 때 이름이 조용히 바뀌고,
 * 003 의 SC-205("이름을 바꾼 뒤 과거 지출을 조회하면 스냅샷이 바뀌지 않는다")가 깨진다.
 * 예외도 오류 응답도 나지 않아 한참 뒤에 발견된다.
 */
class ExpenseSnapshotIT extends AbstractExpenseIT {

    /** 수단 이름을 003 의 2.4 로 바꾼다. */
    private void renamePaymentMethod(Fixture fixture, long methodId, String newName)
            throws Exception {
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + methodId, fixture.token(), """
                {"name":"%s"}
                """.formatted(newName)))).isEqualTo(200);
    }

    @Test
    @DisplayName("#2 등록 후 수단 이름을 바꿔도 그 지출의 스냅샷은 등록 당시 이름이다 — SC-305")
    void renamingTheSourceDoesNotTouchPastExpenses() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        renamePaymentMethod(fixture, fixture.paymentMethodId(), "국민카드(메인)");

        JsonNode data = get(fixture, expenseId).get("data");
        assertThat(data.get("paymentMethodName").asString()).isEqualTo("국민카드");
        // 참조는 그대로 최신 행을 가리킨다 — 스냅샷과 참조는 별개다.
        assertThat(data.get("paymentMethodId").asLong()).isEqualTo(fixture.paymentMethodId());
    }

    @Test
    @DisplayName("#2 지출유형 이름을 바꿔도 스냅샷은 그대로다")
    void renamingTheExpendGroupDoesNotTouchPastExpenses() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        // "식비"는 기본 유형이라 이름을 못 바꾼다(3105). 사용자 유형을 만들어 확인한다.
        long groupId = fixture.expendGroupId();
        tx.executeWithoutResult(status -> jdbc.update(
                "update moneylog.tbl_user_expend_group set default_group = false where idx = ?",
                groupId));

        assertThat(resCode(patchGroupName(fixture, groupId, "밥값"))).isEqualTo(200);

        assertThat(get(fixture, expenseId).get("data").get("expendGroupName").asString())
                .isEqualTo("식비");
    }

    @Test
    @DisplayName("#6 수정에서 paymentMethodId 를 바꾸면 스냅샷이 새 수단의 현재 이름으로 갱신된다")
    void changingTheReferenceRefreshesTheSnapshot() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        long newMethodId = createExpensePaymentMethod(fixture.token(), "신한카드");

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"paymentMethodId":%d}
                """.formatted(newMethodId));

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("paymentMethodId").asLong()).isEqualTo(newMethodId);
        assertThat(data.get("paymentMethodName").asString()).isEqualTo("신한카드");
        assertThat(row(expenseId).get("payment_method_name")).isEqualTo("신한카드");
    }

    @Test
    @DisplayName("#7 paymentMethodId 를 omit 하고 금액만 바꾸면 스냅샷이 그대로다")
    void omittingTheReferenceKeepsTheSnapshot() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        // 원본 이름을 먼저 바꿔 둔다 — "최신화한다" 구현이면 여기서 새 이름이 들어온다.
        renamePaymentMethod(fixture, fixture.paymentMethodId(), "국민카드(메인)");

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"amount":30000}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("amount").asLong()).isEqualTo(30000L);
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(row(expenseId).get("payment_method_name")).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#8 paymentMethodId 에 같은 값을 보내도 스냅샷이 그대로다")
    void sendingTheSameReferenceKeepsTheSnapshot() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        renamePaymentMethod(fixture, fixture.paymentMethodId(), "국민카드(메인)");

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"paymentMethodId":%d,"amount":30000}
                """.formatted(fixture.paymentMethodId()));

        assertThat(resCode(response)).isEqualTo(200);
        // "보냈는가"만 보고 갱신하면 여기서 "국민카드(메인)"이 들어온다.
        assertThat(response.get("data").get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(row(expenseId).get("payment_method_name")).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#6 지출유형을 바꿔도 같은 규칙이다")
    void changingTheExpendGroupRefreshesItsSnapshot() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        long newGroupId = defaultGroupId(fixture.member(), "교통");

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"expendGroupId":%d}
                """.formatted(newGroupId));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("expendGroupId").asLong()).isEqualTo(newGroupId);
        assertThat(response.get("data").get("expendGroupName").asString()).isEqualTo("교통");
    }

    @Test
    @DisplayName("수정에서 새 참조가 사용 중이 아니면 3003 이고 기존 값이 그대로 남는다")
    void changingToAnUnusableReferenceIs3003() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);
        long deadMethodId = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + deadMethodId, fixture.token())))
                .isEqualTo(200);

        JsonNode response = patchJson(URL + "/" + expenseId, fixture.token(), """
                {"paymentMethodId":%d,"amount":30000}
                """.formatted(deadMethodId));

        assertThat(resCode(response)).isEqualTo(3003);
        // 거절했으면 금액도 바뀌지 않아야 한다 — 부분 적용을 남기지 않는다.
        assertThat(row(expenseId).get("amount")).isEqualTo(12000L);
        assertThat(row(expenseId).get("payment_method_name")).isEqualTo("국민카드");
    }

    /** 003 의 2.11 로 유형 이름을 바꾼다. multipart 라 파트로 보낸다. */
    private JsonNode patchGroupName(Fixture fixture, long expendGroupId, String name)
            throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/expend-groups/" + expendGroupId);
        request.with(servletRequest -> {
            servletRequest.setMethod("PATCH");
            return servletRequest;
        });
        request.header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                "Bearer " + fixture.token());
        request.part(new org.springframework.mock.web.MockPart("name",
                name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String response = mockMvc.perform(request).andReturn().getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }
}

package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 수단별 요약 — quickstart #27·#28·#29 (FR-521a).
 *
 * <h2>셋이 한 묶음이다</h2>
 *
 * <table border="1">
 *   <caption>모집단 = ① ∪ ②</caption>
 *   <tr><th>#</th><th>수단 상태</th><th>그 달 지출</th><th>요약에</th></tr>
 *   <tr><td>27</td><td>사용 중</td><td>0원</td><td><b>있다</b> (②)</td></tr>
 *   <tr><td>28</td><td>삭제 표시</td><td>0원</td><td><b>없다</b> (②의 제한)</td></tr>
 *   <tr><td>29</td><td>삭제 표시</td><td>있음</td><td><b>있다</b> (①)</td></tr>
 * </table>
 *
 * <p><b>셋을 함께 봐야 두 집합의 합집합이 확인된다.</b> 하나씩 보면 "회원 소유 수단 전부"
 * (28 이 걸린다)나 "지출이 있는 수단만"(27 이 걸린다)이 각각 통과한다.
 *
 * <p><b>유형별과 정반대라 헷갈리는 지점이다.</b> 유형별은 0원 유형을 빼고 수단별은 넣는다 —
 * "이 달에 어디에 썼나"와 "어느 카드를 얼마나 썼나"의 차이다. 안 쓴 카드도 있어야 비교가
 * 된다.
 */
class StatisticsMethodSummaryIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#27 지출 0원인 사용 중 수단은 요약에 있다")
    void includesUnusedActiveMethod() throws Exception {
        Fixture fixture = prepare();
        long sparecard = createExpensePaymentMethod(fixture.token(), "예비카드");
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());

        JsonNode row = methodSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH), sparecard);

        assertThat(row).isNotNull();
        assertThat(row.get("amount").asLong()).isZero();
        assertThat(row.get("paymentMethodName").asText()).isEqualTo("예비카드");
    }

    @Test
    @DisplayName("#28 지출 0원인 삭제 표시된 수단은 요약에 없다")
    void excludesUnusedDeletedMethod() throws Exception {
        Fixture fixture = prepare();
        long oldCard = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + oldCard, fixture.token())))
                .isEqualTo(200);
        addExpense(fixture, "2026-07-03", 30_000L, fixture.foodGroupId());

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        // 버린 카드의 0원 행이 매달 쌓이지 않아야 한다.
        assertThat(methodSummaryOf(response, oldCard)).isNull();
    }

    @Test
    @DisplayName("#29 그 달 지출이 있는 삭제 표시된 수단은 요약에 있다")
    void includesDeletedMethodThatWasUsed() throws Exception {
        Fixture fixture = prepare();
        long oldCard = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":70000,
                 "paymentDate":"2026-07-03","place":"편의점","content":"지출"}
                """.formatted(oldCard, fixture.foodGroupId())))).isEqualTo(200);
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + oldCard, fixture.token())))
                .isEqualTo(200);

        JsonNode row = methodSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH), oldCard);

        // 그 달에 실제로 쓴 카드를 나중에 정리했다고 과거 통계에서 사라지면 합계가 맞지 않는다.
        assertThat(row).isNotNull();
        assertThat(row.get("amount").asLong()).isEqualTo(70_000L);
    }

    /**
     * ②는 <b>사용 안 함</b>도 뺀다 — 조건이 "사용 중이고 삭제되지 않은" 둘 다이기 때문이다.
     */
    @Test
    @DisplayName("지출 0원인 사용 안 함(inUse=false) 수단도 요약에 없다")
    void excludesUnusedInactiveMethod() throws Exception {
        Fixture fixture = prepare();
        long paused = createExpensePaymentMethod(fixture.token(), "쉬는 카드");
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + paused, fixture.token(),
                "{\"inUse\":false}"))).isEqualTo(200);

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(methodSummaryOf(response, paused)).isNull();
    }

    /** ①은 상태를 묻지 않으므로 사용 안 함 수단도 그 달 지출이 있으면 들어온다. */
    @Test
    @DisplayName("그 달 지출이 있는 사용 안 함 수단은 요약에 있다")
    void includesInactiveMethodThatWasUsed() throws Exception {
        Fixture fixture = prepare();
        long paused = createExpensePaymentMethod(fixture.token(), "쉬는 카드");
        assertThat(resCode(postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":50000,
                 "paymentDate":"2026-07-03","place":"편의점","content":"지출"}
                """.formatted(paused, fixture.foodGroupId())))).isEqualTo(200);
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + paused, fixture.token(),
                "{\"inUse\":false}"))).isEqualTo(200);

        JsonNode row = methodSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH), paused);

        assertThat(row).isNotNull();
        assertThat(row.get("amount").asLong()).isEqualTo(50_000L);
    }

    /**
     * 소득 수단은 들어오지 않는다.
     *
     * <p>②가 {@code purpose=EXPENSE} 로 한정돼 있기 때문이다 — 통계의 이 표는 지출 요약이고
     * 소득은 {@code incomeTotal} 에 합산된다.
     */
    @Test
    @DisplayName("소득 수단은 수단별 요약에 없다")
    void excludesIncomeMethod() throws Exception {
        Fixture fixture = prepare();
        addIncome(fixture, "2026-07-25", 3_500_000L);

        JsonNode response = statistics(fixture, FIXED_YEAR, FIXED_MONTH);

        assertThat(methodSummaryOf(response, fixture.incomeMethodId())).isNull();
        assertThat(response.get("data").get("incomeTotal").asLong()).isEqualTo(3_500_000L);
    }

    @Test
    @DisplayName("같은 수단의 지출 여러 건이 합산된다")
    void sumsExpensesPerMethod() throws Exception {
        Fixture fixture = prepare();
        addExpense(fixture, "2026-07-03", 10_000L, fixture.foodGroupId());
        addExpense(fixture, "2026-07-15", 20_000L, fixture.foodGroupId());

        JsonNode row = methodSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.expenseMethodId());

        assertThat(row.get("amount").asLong()).isEqualTo(30_000L);
    }

    /** 고정지출도 그 수단의 합계에 들어간다 — 수단별 합이 지출 총액과 맞아야 한다. */
    @Test
    @DisplayName("고정지출도 그 수단의 합계에 더해진다")
    void includesFixedExpense() throws Exception {
        Fixture fixture = prepare();
        long fixedExpenseId = createFixedExpense(fixture.token(), "월세",
                fixture.expenseMethodId(), fixture.foodGroupId(), 500_000L, 5,
                "2026-07", "2026-12");
        insertMonthlyRow(fixture.member(), fixedExpenseId, FIXED_YEAR, FIXED_MONTH,
                500_000L, "2026-07-05");

        JsonNode row = methodSummaryOf(statistics(fixture, FIXED_YEAR, FIXED_MONTH),
                fixture.expenseMethodId());

        assertThat(row.get("amount").asLong()).isEqualTo(500_000L);
    }
}

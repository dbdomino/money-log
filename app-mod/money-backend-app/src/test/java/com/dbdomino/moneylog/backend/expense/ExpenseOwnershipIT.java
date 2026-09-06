package com.dbdomino.moneylog.backend.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 남의 지출에는 손댈 수 없다 — quickstart #10 (FR-301·SC-307).
 *
 * <p><b>"없는 ID"와 "남의 ID"가 같은 {@code 3202} 여야 한다</b>(api-contract.md §1).
 * 코드가 갈리면 ID 를 훑는 것만으로 남의 지출이 존재한다는 사실이 새어 나간다 — 건수와
 * 등록 시점이 드러난다.
 *
 * <p>003 의 {@code PaymentMethodOwnershipIT}({@code 3003})·
 * {@code ExpendGroupOwnershipIT}({@code 3103})와 같은 형태이며, 셋이 함께 있어야
 * "본인 데이터만 접근한다"가 자원별로 전부 검증된다.
 */
class ExpenseOwnershipIT extends AbstractExpenseIT {

    @Test
    @DisplayName("#10 남의 지출 상세 조회는 3202 다")
    void readingOthersIs3202() throws Exception {
        Fixture owner = prepare();
        long othersId = createExpense(owner);
        Fixture intruder = prepare();

        assertThat(resCode(get(intruder, othersId))).isEqualTo(3202);
    }

    @Test
    @DisplayName("#10 남의 지출 수정은 3202 이고 값도 바뀌지 않는다")
    void updatingOthersIs3202() throws Exception {
        Fixture owner = prepare();
        long othersId = createExpense(owner);
        Fixture intruder = prepare();

        assertThat(resCode(patchJson(URL + "/" + othersId, intruder.token(), """
                {"amount":999999,"content":"바꿔치기"}
                """))).isEqualTo(3202);

        // 거절만으로는 부족하다 — 값이 그대로인지 DB 로 확인한다.
        assertThat(row(othersId).get("amount")).isEqualTo(12000L);
        assertThat(row(othersId).get("content")).isEqualTo("점심");
    }

    @Test
    @DisplayName("#10 남의 지출 삭제는 3202 이고 행도 남는다")
    void deletingOthersIs3202() throws Exception {
        Fixture owner = prepare();
        long othersId = createExpense(owner);
        Fixture intruder = prepare();

        assertThat(resCode(deleteJson(URL + "/" + othersId, intruder.token()))).isEqualTo(3202);

        assertThat(countExpenses(owner.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("#10 없는 ID 도 같은 3202 다 — 코드가 갈리면 존재 여부가 새어 나간다")
    void missingIdIsAlso3202() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(get(fixture, 999999999L))).isEqualTo(3202);
        assertThat(resCode(patchJson(URL + "/999999999", fixture.token(), """
                {"amount":1000}
                """))).isEqualTo(3202);
        assertThat(resCode(deleteJson(URL + "/999999999", fixture.token()))).isEqualTo(3202);
    }

    @Test
    @DisplayName("남의 지출에 내 수단을 걸려는 수정도 3202 다 — 소유자 판정이 먼저다")
    void ownershipIsCheckedBeforeReferences() throws Exception {
        Fixture owner = prepare();
        long othersId = createExpense(owner);
        Fixture intruder = prepare();

        // 3003 이 나오면 참조 검증이 소유자 판정보다 앞선 것이다.
        assertThat(resCode(patchJson(URL + "/" + othersId, intruder.token(), """
                {"paymentMethodId":%d}
                """.formatted(intruder.paymentMethodId())))).isEqualTo(3202);
    }

    @Test
    @DisplayName("토큰 없이 조회·수정·삭제하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();
        long expenseId = createExpense(fixture);

        assertThat(resCode(getJson(URL + "/" + expenseId, null))).isEqualTo(1001);
        assertThat(resCode(patchJson(URL + "/" + expenseId, null, """
                {"amount":1000}
                """))).isEqualTo(1001);
        assertThat(resCode(deleteJson(URL + "/" + expenseId, null))).isEqualTo(1001);
    }
}

package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 필터와 생성의 순서 — quickstart #21·#22 (FR-406).
 *
 * <p><b>#21 과 #22 가 한 쌍이다.</b> 따로 보면 둘 다 통과하는 잘못된 구현이 있다.
 *
 * <pre>{@code
 * 필터를 생성 대상에도 적용한 구현:
 *   #21 paymentMethodId=A 로 그 달을 처음 연다  → A 것만 생성, 결과도 A 것뿐. 통과
 *   #22 필터 없이 같은 달을 연다               → B 것이 "그때" 생성된다
 * }</pre>
 *
 * <p>#22 의 응답 건수만 보면 이것도 통과한다 — 결국 둘 다 나오기 때문이다. 그래서
 * 이 시험은 <b>#21 직후의 DB 행 수</b>를 본다. 생성이 필터를 타면 그 시점에 1건이고,
 * 타지 않으면 2건이다.
 *
 * <p>이 순서가 중요한 이유는 <b>같은 달의 내역이 "언제 어떤 필터로 처음 열었는가"에
 * 따라 달라지기 때문</b>이다. 게다가 4.8(가계부 목록)은 필터가 있어도 그 달 전체를
 * 만들어야 하는데(FR-418), 4.5 가 부분 생성을 하면 두 API 가 만드는 결과가 갈린다.
 */
class MonthlyFilterOrderIT extends AbstractMonthlyIT {

    @Test
    @DisplayName("#21 필터를 걸고 처음 열어도 그 달 대상 전체가 생성된다")
    void filteredFirstOpenStillCreatesEverything() throws Exception {
        Fixture fixture = prepare();
        long anotherMethod = createExpensePaymentMethod(fixture.token(), "신한카드");
        addFixedExpense(fixture, "통신비", anotherMethod, 60000L);

        JsonNode data = listMonthly(fixture, 2026, 11,
                "paymentMethodId=" + fixture.paymentMethodId()).get("data");

        // 결과는 좁혀진다.
        assertThat(data.get("list")).hasSize(1);
        // 생성은 좁혀지지 않는다. 여기서 1이 나오면 필터가 생성 대상을 탄 것이다.
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(2);
    }

    @Test
    @DisplayName("#22 21번 직후 필터 없이 같은 달을 열면 나머지가 이미 있다")
    void theRestWasAlreadyThere() throws Exception {
        Fixture fixture = prepare();
        long anotherMethod = createExpensePaymentMethod(fixture.token(), "신한카드");
        addFixedExpense(fixture, "통신비", anotherMethod, 60000L);
        listMonthly(fixture, 2026, 11, "paymentMethodId=" + fixture.paymentMethodId());
        // 이 시점에 이미 2건이어야 한다.
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(2);

        JsonNode data = listMonthly(fixture, 2026, 11).get("data");

        assertThat(data.get("list")).hasSize(2);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(2);
    }

    @Test
    @DisplayName("#21 total 은 필터와 무관한 그 달 전체 합계다")
    void totalIgnoresTheFilter() throws Exception {
        Fixture fixture = prepare();
        long anotherMethod = createExpensePaymentMethod(fixture.token(), "신한카드");
        addFixedExpense(fixture, "통신비", anotherMethod, 60000L);

        JsonNode data = listMonthly(fixture, 2026, 11,
                "paymentMethodId=" + fixture.paymentMethodId()).get("data");

        // list 는 1건(500000)인데 total 은 그 달 전체다. 4.8 의 expenseTotal 과 같은 규칙이며
        // 합계는 화면 상단 요약이고 필터는 아래 목록을 좁히는 도구이기 때문이다.
        assertThat(data.get("list")).hasSize(1);
        assertThat(data.get("total").asLong()).isEqualTo(560000L);
    }

    @Test
    @DisplayName("지출유형 필터도 같은 규칙이다")
    void expendGroupFilterFollowsTheSameRule() throws Exception {
        Fixture fixture = prepare();
        long otherGroup = defaultGroupId(fixture.member(), "통신");
        createFixedExpense(fixture.token(), "통신비", fixture.paymentMethodId(),
                otherGroup, 60000L, 10, START, END);

        JsonNode data = listMonthly(fixture, 2026, 11,
                "expendGroupId=" + fixture.expendGroupId()).get("data");

        assertThat(data.get("list")).hasSize(1);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(2);
    }

    @Test
    @DisplayName("두 필터를 함께 걸면 AND 다")
    void bothFiltersAreCombinedWithAnd() throws Exception {
        Fixture fixture = prepare();
        long otherGroup = defaultGroupId(fixture.member(), "통신");
        long anotherMethod = createExpensePaymentMethod(fixture.token(), "신한카드");
        createFixedExpense(fixture.token(), "통신비", anotherMethod,
                otherGroup, 60000L, 10, START, END);

        JsonNode data = listMonthly(fixture, 2026, 11,
                "paymentMethodId=" + fixture.paymentMethodId()
                        + "&expendGroupId=" + otherGroup).get("data");

        // 수단은 첫 번째, 유형은 두 번째 것이라 교집합이 비어야 한다.
        assertThat(data.get("list")).isEmpty();
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(2);
    }

    @Test
    @DisplayName("걸리지 않는 필터 값을 주면 빈 배열이다 — 오류가 아니다")
    void filterWithNoMatchIsAnEmptyList() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = listMonthly(fixture, 2026, 11, "paymentMethodId=999999999");

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("list")).isEmpty();
        // 없는 수단이어도 생성은 그대로 일어난다.
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }
}

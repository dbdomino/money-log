package com.dbdomino.moneylog.backend.income;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.7 소득 등록 — quickstart #15·#16·#17·#20.
 *
 * <p><b>#20 이 이 클래스의 핵심이다.</b> 금액 오류가 {@code 3301} 이어야 하고
 * {@code 3201}(지출)이면 안 된다 — 값 규칙은 같지만 코드 대역이 갈린다(api-contract.md §7).
 * 한 서비스에서 코드를 공유하거나 {@code LedgerFieldRules} 에 코드를 박아 두면 여기서 걸린다.
 *
 * <p><b>#17 은 "컬럼이 아예 없다"를 확인한다</b>(FR-306). 소득에는 장소·지출유형·할부가
 * 비어 있는 것이 아니라 <b>존재하지 않는다</b> — Body 에 실어 보내도 저장되지 않고 응답에도
 * 나타나지 않는다.
 */
class IncomeCreateIT extends AbstractIncomeIT {

    @Test
    @DisplayName("#15 purpose=INCOME 수단으로 등록하면 수단 이름 스냅샷과 함께 저장된다")
    void createStoresTheNameSnapshot() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = create(fixture);

        assertThat(resCode(response)).isEqualTo(200);
        long incomeId = response.get("data").get("incomeId").asLong();

        var row = row(incomeId);
        assertThat(row.get("payment_method_name")).isEqualTo("월급통장");
        assertThat(row.get("payment_method_idx")).isEqualTo(fixture.paymentMethodId());
        assertThat(row.get("amount")).isEqualTo(3000000L);
    }

    @Test
    @DisplayName("등록 응답은 incomeId 한 칸이다 — 상세는 3.8 로 읽는다")
    void createResponseCarriesOnlyThePk() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = create(fixture).get("data");

        assertThat(data.has("incomeId")).isTrue();
        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("#16 content 를 비우고 등록해도 성공한다 — 지출과 다르다")
    void contentIsOptional() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25"}
                """.formatted(fixture.paymentMethodId()));

        assertThat(resCode(response)).isEqualTo(200);
        // 빈 값은 null 로 저장한다 — "안 적었다"를 두 가지로 저장하지 않는다.
        assertThat(row(response.get("data").get("incomeId").asLong()).get("content")).isNull();
    }

    @Test
    @DisplayName("#16 content 에 빈 문자열을 보내도 null 로 저장된다")
    void blankContentBecomesNull() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"  "}
                """.formatted(fixture.paymentMethodId()));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(row(response.get("data").get("incomeId").asLong()).get("content")).isNull();
    }

    @Test
    @DisplayName("#17 place·expendGroupId·할부 필드를 실어 보내도 무시되고 응답에 없다")
    void expenseOnlyFieldsAreIgnored() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"급여",
                 "place":"회사","expendGroupId":1,
                 "installmentGroupId":1,"installmentIndex":1,"installmentTotal":12}
                """.formatted(fixture.paymentMethodId()));

        assertThat(resCode(response)).isEqualTo(200);
        long incomeId = response.get("data").get("incomeId").asLong();

        // tbl_income 에 그 컬럼이 없으므로 저장될 자리가 없다.
        JsonNode data = get(fixture, incomeId).get("data");
        assertThat(data.has("place")).isFalse();
        assertThat(data.has("expendGroupId")).isFalse();
        assertThat(data.has("expendGroupName")).isFalse();
        assertThat(data.has("installmentGroupId")).isFalse();
        assertThat(data.has("installmentIndex")).isFalse();
        assertThat(data.has("installmentTotal")).isFalse();
    }

    @Test
    @DisplayName("#20 금액이 0 이하면 3301 이다 — 3201 이 아니다")
    void nonPositiveAmountIs3301() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":0,"paymentDate":"2026-03-25","content":"급여"}
                """.formatted(fixture.paymentMethodId()));

        // 3201 이 나오면 지출의 코드를 그대로 쓴 것이다.
        assertThat(resCode(response)).isEqualTo(3301);
    }

    @Test
    @DisplayName("#20 날짜 형식 오류도 3301 이다")
    void malformedDateIs3301() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026/03/25"}
                """.formatted(fixture.paymentMethodId())))).isEqualTo(3301);
    }

    @Test
    @DisplayName("#20 내용이 256자면 3301 이다")
    void tooLongContentIs3301() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"%s"}
                """.formatted(fixture.paymentMethodId(), "가".repeat(256)));

        assertThat(resCode(response)).isEqualTo(3301);
    }

    @Test
    @DisplayName("사용 안 함·삭제 표시된 수단으로 등록하면 3003 이다")
    void unusablePaymentMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        assertThat(resCode(create(fixture))).isEqualTo(3003);
    }

    @Test
    @DisplayName("지출용 수단으로 소득을 등록하면 3003 이다 — 한 수단은 한쪽 용도만 갖는다")
    void expensePurposeMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        long expenseMethodId = createExpensePaymentMethod(fixture.token(), "국민카드");

        JsonNode response = createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"급여"}
                """.formatted(expenseMethodId));

        // 통과시키면 003 이 purpose 변경을 참조 0건일 때만 허용하는 이유가 무너진다.
        assertThat(resCode(response)).isEqualTo(3003);
    }

    @Test
    @DisplayName("필수 필드를 빠뜨리면 9001 이다 — 3301 이 아니다")
    void missingRequiredFieldIs9001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000}
                """.formatted(fixture.paymentMethodId())))).isEqualTo(9001);
    }

    @Test
    @DisplayName("토큰 없이 등록하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        assertThat(resCode(postJson(URL, null, """
                {"paymentMethodId":1,"amount":3000000,"paymentDate":"2026-03-25"}
                """))).isEqualTo(1001);
    }
}

package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.4 고정지출 설정 수정 — PATCH omit 규칙과 값 검증.
 *
 * <p><b>자동 반영 범위(FR-412)는 여기서 검증하지 않는다.</b> 그쪽은 US4 의
 * {@code sync/PropagationRangeIT} 가 맡는다 — 지난 달·이번 달·미래 달 내역이 있어야
 * 성립하는데 그건 US2 의 lazy 생성 위에 선다. 여기서는 <b>설정 행 자체가 올바로
 * 갱신되는가</b>만 본다.
 *
 * <p>수정이 4.9(재작성)를 필요하게 만드는 지점이 하나 있다 — <b>적용 기간을 줄이면</b>
 * 기간 밖이 된 월별 내역이 남는다. 자동 반영은 값 갱신만 하고 삭제하지 않는다.
 */
class FixedExpenseUpdateIT extends AbstractFixedExpenseIT {

    private JsonNode update(Fixture fixture, long id, String body) throws Exception {
        return patchJson(URL + "/" + id, fixture.token(), body);
    }

    private JsonNode get(Fixture fixture, long id) throws Exception {
        return getJson(URL + "/" + id, fixture.token()).get("data");
    }

    @Test
    @DisplayName("보낸 필드만 바뀌고 나머지는 유지된다 — omit = 유지")
    void onlySentFieldsChange() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id, """
                {"amount":600000}
                """))).isEqualTo(200);

        JsonNode data = get(fixture, id);
        assertThat(data.get("amount").asLong()).isEqualTo(600000L);
        // 보내지 않은 것들이 그대로여야 한다.
        assertThat(data.get("name").asString()).isEqualTo("월세");
        assertThat(data.get("paymentDayOfMonth").asInt()).isEqualTo(25);
        assertThat(data.get("startYear").asInt()).isEqualTo(2026);
        assertThat(data.get("endMonth").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("이름·결제일·내용을 함께 바꿀 수 있다")
    void multipleFieldsAtOnce() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id, """
                {"name":"전세 이자","paymentDayOfMonth":5,"content":"이자 납입"}
                """))).isEqualTo(200);

        JsonNode data = get(fixture, id);
        assertThat(data.get("name").asString()).isEqualTo("전세 이자");
        assertThat(data.get("paymentDayOfMonth").asInt()).isEqualTo(5);
        assertThat(data.get("content").asString()).isEqualTo("이자 납입");
    }

    @Test
    @DisplayName("적용 기간을 바꿀 수 있다 — 기간을 줄이는 것도 허용된다")
    void periodCanBeChanged() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        // 2027-02 종료 → 2026-12 종료. 기간 밖이 된 월별 내역의 정리는 4.9 의 몫이다.
        assertThat(resCode(update(fixture, id, """
                {"endYear":2026,"endMonth":12}
                """))).isEqualTo(200);

        JsonNode data = get(fixture, id);
        assertThat(data.get("endYear").asInt()).isEqualTo(2026);
        assertThat(data.get("endMonth").asInt()).isEqualTo(12);
    }

    @Test
    @DisplayName("결과 기간이 뒤집히면 3401 이다 — 보낸 필드만으로 판단하지 않는다")
    void resultingPeriodMustStayValid() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        // 시작만 2027-05 로 민다. 종료(2027-02)는 그대로라 결과가 뒤집힌다.
        // 보낸 값만 보고 통과시키면 CHECK ck_fixed_expense_period 가 9000 으로 터진다.
        assertThat(resCode(update(fixture, id, """
                {"startYear":2027,"startMonth":5}
                """))).isEqualTo(3401);

        assertThat(get(fixture, id).get("startMonth").asInt()).isEqualTo(11);
    }

    @Test
    @DisplayName("금액 0 이하·결제일 범위 밖은 3401 이다")
    void invalidValuesAre3401() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id, "{\"amount\":0}"))).isEqualTo(3401);
        assertThat(resCode(update(fixture, id, "{\"paymentDayOfMonth\":32}"))).isEqualTo(3401);
        assertThat(resCode(update(fixture, id, "{\"startMonth\":0}"))).isEqualTo(3401);
    }

    @Test
    @DisplayName("수단을 바꿀 수 있고, 용도가 어긋나면 3401 이다")
    void paymentMethodCanChangeButPurposeIsChecked() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);
        long another = createExpensePaymentMethod(fixture.token(), "신한카드");
        long incomeMethod = createIncomePaymentMethod(fixture.token(), "월급통장");

        assertThat(resCode(update(fixture, id,
                "{\"paymentMethodId\":%d}".formatted(another)))).isEqualTo(200);
        assertThat(get(fixture, id).get("paymentMethodName").asString()).isEqualTo("신한카드");

        // 용도 불일치는 3003 이 아니라 3401 이다(등록과 같은 규칙).
        assertThat(resCode(update(fixture, id,
                "{\"paymentMethodId\":%d}".formatted(incomeMethod)))).isEqualTo(3401);
    }

    @Test
    @DisplayName("없는 수단·유형으로 바꾸려 하면 3003·3103 이다")
    void missingReferencesKeepTheirCodes() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id, "{\"paymentMethodId\":999999999}"))).isEqualTo(3003);
        assertThat(resCode(update(fixture, id, "{\"expendGroupId\":999999999}"))).isEqualTo(3103);
    }

    @Test
    @DisplayName("참조를 안 보내면 검증하지 않는다 — 죽은 수단을 쓰던 설정도 고칠 수 있다")
    void omittedReferenceIsNotValidated() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        // 수단을 삭제 표시한 뒤 금액만 고친다. 참조를 안 보냈으니 검증할 것이 없다.
        // 무조건 검증하면 삭제 표시된 수단을 쓰던 설정을 영영 못 고친다 — 004 의 FR-326 과
        // 같은 함정이다.
        assertThat(resCode(update(fixture, id, "{\"amount\":700000}"))).isEqualTo(200);
        assertThat(get(fixture, id).get("amount").asLong()).isEqualTo(700000L);
    }

    @Test
    @DisplayName("같은 수단을 다시 보내면 통과한다 — 값이 바뀌지 않았다")
    void sendingTheSameReferenceIsFine() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id,
                "{\"paymentMethodId\":%d}".formatted(fixture.paymentMethodId())))).isEqualTo(200);
    }

    @Test
    @DisplayName("대상 밖 필드를 보내면 9001 이다")
    void unknownFieldIs9001() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(update(fixture, id, """
                {"fixedExpenseId":99}
                """))).isEqualTo(9001);
    }

    @Test
    @DisplayName("남의 설정은 3402 다 — 값 검증보다 소유자 판정이 먼저다")
    void ownershipIsCheckedFirst() throws Exception {
        Fixture owner = prepare();
        long id = createDefaultFixedExpense(owner);
        Fixture stranger = prepare();

        // 값도 잘못됐지만 3401 이 아니라 3402 다. 순서가 뒤집히면 남의 설정의 존재가
        // "값 오류"와 "없음"의 차이로 새어 나간다.
        assertThat(resCode(patchJson(URL + "/" + id, stranger.token(), """
                {"amount":0}
                """))).isEqualTo(3402);
    }
}

package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.1 고정지출 등록 — quickstart #1·#2·#3·#4·#5·#11.
 *
 * <p><b>#1 이 이 스토리의 핵심이다.</b> 등록은 <b>관리 행 1건만</b> 만들고 적용 기간
 * 전체의 월별 내역을 만들지 않는다(FR-402). 미리 만들면 10년짜리 설정 하나에 120행이
 * 생기는데 사용자가 실제로 여는 달은 몇 개뿐이고, 설정을 고칠 때마다 그 120행을 전부
 * 손봐야 한다.
 */
class FixedExpenseCreateIT extends AbstractFixedExpenseIT {

    /** 등록 요청 본문. 연·월을 {@code "YYYY-MM"} 으로 받아 4개 필드로 펼친다. */
    private String body(Fixture fixture, long amount, int paymentDayOfMonth,
                        String start, String end) {
        return """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":%d,
                 "paymentDayOfMonth":%d,"content":"매달 월세",
                 "startYear":%d,"startMonth":%d,"endYear":%d,"endMonth":%d}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId(), amount,
                paymentDayOfMonth, yearOf(start), monthOf(start), yearOf(end), monthOf(end));
    }

    @Test
    @DisplayName("#1 등록하면 관리 행 1건만 생기고 월별 내역은 0건이다")
    void createsOnlyTheSettingRow() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = postJson(URL, fixture.token(), body(fixture, 500000L, 25, START, END));

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("fixedExpenseId").asLong()).isPositive();
        // 적용 기간이 2026-11 ~ 2027-02 로 4개월인데 월별 내역은 하나도 없어야 한다.
        // 여기서 4가 나오면 등록이 기간 전체를 펼친 것이다(FR-402 위반).
        assertThat(countMonthlyAll(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#1 응답의 이름 두 개는 조회 시점 현재 이름이다")
    void responseCarriesCurrentNames() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = postJson(URL, fixture.token(),
                body(fixture, 500000L, 25, START, END)).get("data");

        assertThat(data.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(data.get("expendGroupName").asString()).isEqualTo("주거");
    }

    @Test
    @DisplayName("#2 결제일이 32면 3401 이다")
    void paymentDayOutOfRangeIs3401() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 32, START, END)))).isEqualTo(3401);
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 0, START, END)))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#2 결제일 31 은 통과한다 — 말일 보정은 월별 내역을 만들 때 한다")
    void paymentDayThirtyOneIsAccepted() throws Exception {
        Fixture fixture = prepare();

        // 31 을 막으면 매월 말일에 나가는 고정지출을 표현할 수 없다.
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 31, START, END)))).isEqualTo(200);
    }

    @Test
    @DisplayName("#3 종료 연월이 시작보다 앞서면 3401 이다")
    void endBeforeStartIs3401() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 25, "2027-02", "2026-11")))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#3 해를 넘겨 앞서는 경우도 3401 이다 — 월만 비교하면 통과해 버린다")
    void endBeforeStartAcrossYearsIs3401() throws Exception {
        Fixture fixture = prepare();

        // 2027-01 시작 → 2026-12 종료. 월만 비교하면 1 <= 12 라 통과한다.
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 25, "2027-01", "2026-12")))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#4 시작과 종료가 같은 한 달짜리는 성공한다")
    void singleMonthPeriodIsAccepted() throws Exception {
        Fixture fixture = prepare();

        // 경계다. `<` 로 비교하면 여기서 걸린다 — 양 끝을 포함해야 한다.
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 25, "2026-11", "2026-11")))).isEqualTo(200);
    }

    @Test
    @DisplayName("#5 2026-12 시작 2027-01 종료는 성공한다 — 해를 넘겨도 유효하다")
    void periodAcrossYearBoundaryIsAccepted() throws Exception {
        Fixture fixture = prepare();

        // 월만 비교하면 12 <= 1 이 거짓이라 거절된다. 합성 비교가 아니면 여기서 걸린다.
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 500000L, 25, "2026-12", "2027-01")))).isEqualTo(200);
    }

    @Test
    @DisplayName("금액이 0 이하면 3401 이다")
    void nonPositiveAmountIs3401() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, 0L, 25, START, END)))).isEqualTo(3401);
        assertThat(resCode(postJson(URL, fixture.token(),
                body(fixture, -1000L, 25, START, END)))).isEqualTo(3401);
    }

    @Test
    @DisplayName("시작·종료 월이 1~12 밖이면 3401 이다")
    void monthOutOfRangeIs3401() throws Exception {
        Fixture fixture = prepare();

        String outOfRange = """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"매달 월세",
                 "startYear":2026,"startMonth":13,"endYear":2027,"endMonth":2}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId());

        assertThat(resCode(postJson(URL, fixture.token(), outOfRange))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#11 purpose=INCOME 수단으로 등록하면 3401 이다 — 3003 이 아니다")
    void incomePurposeMethodIs3401() throws Exception {
        Fixture fixture = prepare();
        long incomeMethodId = createIncomePaymentMethod(fixture.token(), "월급통장");
        String withIncomeMethod = """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"매달 월세",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(incomeMethodId, fixture.expendGroupId());

        // 004 는 용도 불일치까지 3003 으로 묶지만 005 는 3401 이다. 그 수단은 실재하고
        // 사용자가 자기 목록에서 고른 것이라 존재를 감출 이유가 없고, 취할 조치도
        // "다른 수단을 고른다"가 아니라 "지출용 수단을 고른다"로 다르다.
        assertThat(resCode(postJson(URL, fixture.token(), withIncomeMethod))).isEqualTo(3401);
    }

    @Test
    @DisplayName("없는 수단이면 3003, 없는 지출유형이면 3103 이다 — 참조가 용도보다 먼저다")
    void missingReferencesUseTheirOwnCodes() throws Exception {
        Fixture fixture = prepare();

        String noMethod = """
                {"name":"월세","paymentMethodId":999999999,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(fixture.expendGroupId());
        assertThat(resCode(postJson(URL, fixture.token(), noMethod))).isEqualTo(3003);

        String noGroup = """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":999999999,"amount":500000,
                 "paymentDayOfMonth":25,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(fixture.paymentMethodId());
        assertThat(resCode(postJson(URL, fixture.token(), noGroup))).isEqualTo(3103);
    }

    @Test
    @DisplayName("삭제 표시된 수단으로는 새로 등록할 수 없다 — 3003")
    void deletedMethodCannotBeReferenced() throws Exception {
        Fixture fixture = prepare();
        long deadId = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + deadId, fixture.token())))
                .isEqualTo(200);

        String withDead = """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(deadId, fixture.expendGroupId());

        assertThat(resCode(postJson(URL, fixture.token(), withDead))).isEqualTo(3003);
    }

    @Test
    @DisplayName("남의 수단으로는 등록할 수 없다 — 3003(없음과 같은 코드다)")
    void othersMethodIs3003() throws Exception {
        Fixture other = prepare();
        Fixture fixture = prepare();

        String withOthers = """
                {"name":"월세","paymentMethodId":%d,"expendGroupId":%d,"amount":500000,
                 "paymentDayOfMonth":25,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(other.paymentMethodId(), fixture.expendGroupId());

        assertThat(resCode(postJson(URL, fixture.token(), withOthers))).isEqualTo(3003);
    }

    @Test
    @DisplayName("토큰 없이 등록하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(URL, null, body(fixture, 500000L, 25, START, END))))
                .isEqualTo(1001);
    }
}

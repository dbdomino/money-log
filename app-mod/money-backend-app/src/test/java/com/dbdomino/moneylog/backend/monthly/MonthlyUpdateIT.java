package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.6 월별 내역 단건 수정 — quickstart #23·#24·#29 (FR-410).
 *
 * <p><b>이 API 가 설정과 월별을 나눈 이유 그 자체다.</b> 한 테이블이었다면 "3월만
 * 5만원"을 표현할 자리가 없다.
 *
 * <p>바꿀 수 있는 것은 <b>금액·결제일·내용·수단 넷뿐</b>이다. 이름과 지출유형은 이 경로의
 * 대상이 아니다 — 그것들은 설정(4.4)에서 바꾸고 월별 내역이 따라간다.
 */
class MonthlyUpdateIT extends AbstractMonthlyIT {

    /** 4.6 의 URL. Path 가 세 조각인 것은 그래야 월별 1행이 특정되기 때문이다. */
    private String updateUrl(Fixture fixture, int year, int month) {
        return LIST_URL + "/" + year + "/" + month + "/" + fixture.fixedExpenseId();
    }

    private JsonNode update(Fixture fixture, int year, int month, String body) throws Exception {
        return patchJson(updateUrl(fixture, year, month), fixture.token(), body);
    }

    /** 그 달 행의 저장된 결제일. */
    private LocalDate storedPaymentDate(Fixture fixture, int year, int month) {
        Object value = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), year, month)
                .get("payment_date");
        return value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
    }

    @Test
    @DisplayName("#23 한 달의 금액만 고치면 다른 달은 그대로다")
    void editingOneMonthLeavesOthersAlone() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        listMonthly(fixture, 2026, 12);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(200);

        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("amount")).isEqualTo(300000L);
        // 다른 달이 따라 움직이면 "한 달만 다르게"라는 이 기능의 목적이 무너진다.
        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 12)
                .get("amount")).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#23 설정의 기본 금액은 바뀌지 않는다")
    void theSettingItselfIsUntouched() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(200);

        // 월별 수정이 설정을 바꾸면 이후 만들어지는 달이 전부 새 값으로 생긴다.
        assertThat(getJson("/api/v1/fixed-expenses/" + fixture.fixedExpenseId(), fixture.token())
                .get("data").get("amount").asLong()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#24 직접 고치면 그 행이 modified=true 가 된다")
    void editingMarksTheRowAsModified() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("modified")).isEqualTo(false);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(200);

        // 이 표시가 FR-412 의 자동 반영과 FR-414 의 ③보존을 가르는 유일한 근거다.
        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("modified")).isEqualTo(true);
    }

    @Test
    @DisplayName("#24 응답에도 modified 가 true 로 온다")
    void responseCarriesTheModifiedFlag() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        JsonNode data = update(fixture, 2026, 11, """
                {"amount":300000}
                """).get("data");

        assertThat(data.get("modified").asBoolean()).isTrue();
        assertThat(data.get("amount").asLong()).isEqualTo(300000L);
        assertThat(data.get("year").asInt()).isEqualTo(2026);
        assertThat(data.get("month").asInt()).isEqualTo(11);
    }

    @Test
    @DisplayName("omit = 유지 — 보낸 필드만 바뀐다")
    void onlySentFieldsChange() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"content":"11월 월세 인상분 포함"}
                """))).isEqualTo(200);

        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11);
        assertThat(row.get("content")).isEqualTo("11월 월세 인상분 포함");
        assertThat(row.get("amount")).isEqualTo(500000L);
        assertThat(storedPaymentDate(fixture, 2026, 11)).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    @DisplayName("네 필드를 함께 바꿀 수 있다")
    void allFourFieldsAtOnce() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long another = createExpensePaymentMethod(fixture.token(), "신한카드");

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":420000,"paymentDate":"2026-11-05","content":"할인 적용",
                 "paymentMethodId":%d}
                """.formatted(another)))).isEqualTo(200);

        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11);
        assertThat(row.get("amount")).isEqualTo(420000L);
        assertThat(row.get("content")).isEqualTo("할인 적용");
        assertThat(((Number) row.get("payment_method_idx")).longValue()).isEqualTo(another);
        assertThat(storedPaymentDate(fixture, 2026, 11)).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("수단을 바꾸면 응답의 이름도 새 수단의 현재 이름이다")
    void changedPaymentMethodShowsItsName() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long another = createExpensePaymentMethod(fixture.token(), "신한카드");

        JsonNode data = update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(another)).get("data");

        assertThat(data.get("paymentMethodName").asString()).isEqualTo("신한카드");
    }

    @Test
    @DisplayName("#29 지출유형은 이 경로의 대상이 아니다 — 9001")
    void expendGroupIsNotUpdatableHere() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long otherGroup = defaultGroupId(fixture.member(), "통신");

        assertThat(resCode(update(fixture, 2026, 11,
                "{\"expendGroupId\":%d}".formatted(otherGroup)))).isEqualTo(9001);

        // 거절만으로는 부족하다 — 실제로 안 바뀌었는지 본다.
        assertThat(((Number) monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("expend_group_idx")).longValue()).isEqualTo(fixture.expendGroupId());
    }

    @Test
    @DisplayName("#29 이름도 대상이 아니다 — 9001")
    void nameIsNotUpdatableHere() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        // 조용히 버리면 오타(amout)가 "아무것도 안 바꿈"으로 흘러가 사용자는 고쳤다고 믿는다.
        // 이 저장소의 모든 PATCH 가 같은 규칙이다.
        assertThat(resCode(update(fixture, 2026, 11, """
                {"name":"바꾼 이름"}
                """))).isEqualTo(9001);
        assertThat(resCode(update(fixture, 2026, 11, """
                {"amout":300000}
                """))).isEqualTo(9001);
    }

    @Test
    @DisplayName("#29 거절된 요청은 modified 를 세우지 않는다")
    void rejectedRequestDoesNotSetModified() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"name":"바꾼 이름"}
                """))).isEqualTo(9001);

        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("modified")).isEqualTo(false);
    }

    @Test
    @DisplayName("고친 값이 4.5 목록과 4.8 가계부에 함께 보인다")
    void theEditIsVisibleInBothLists() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(200);

        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list").get(0)
                .get("amount").asLong()).isEqualTo(300000L);
        assertThat(getJson("/api/v1/ledger/monthly?year=2026&month=11&type=FIXED",
                fixture.token()).get("data").get("list").get(0).get("amount").asLong())
                .isEqualTo(300000L);
    }
}

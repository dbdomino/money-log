package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.5 할부 등록 — quickstart #21·#22·#24·#25·#28.
 *
 * <p><b>#22 가 말일 보정 문제를 없앤 근거를 지킨다.</b> 결제일이 매월 1일로 고정이 아니면
 * 31일에 시작한 할부의 2월 회차를 며칠로 할지 정해야 하고, 그 규칙이 명세에 없다.
 */
class InstallmentCreateIT extends AbstractInstallmentIT {

    @Test
    @DisplayName("#21 12개월 할부를 등록하면 12개 행이 같은 그룹으로 생기고 회차가 1~12 다")
    void twelveMonthsCreateTwelveRows() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = createInstallment(fixture, 100000L, 12, "2026-07");

        assertThat(resCode(response)).isEqualTo(200);
        long groupId = response.get("data").get("installmentGroupId").asLong();

        List<Map<String, Object>> rows = rowsOf(groupId);
        assertThat(rows).hasSize(12);
        for (int i = 0; i < 12; i++) {
            Map<String, Object> row = rows.get(i);
            assertThat(row.get("installment_group_id")).as("그룹 공유").isEqualTo(groupId);
            assertThat(row.get("installment_index")).as("회차").isEqualTo(i + 1);
            assertThat(row.get("installment_total")).as("총 개월").isEqualTo(12);
            assertThat(row.get("amount")).as("월 납부액").isEqualTo(100000L);
        }
    }

    @Test
    @DisplayName("#22 startYearMonth=2026-07 이면 결제일이 2026-07-01~2027-06-01 매월 1일이다")
    void paymentDatesAreTheFirstOfEachMonth() throws Exception {
        Fixture fixture = prepare();

        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));

        List<Map<String, Object>> rows = rowsOf(groupId);
        assertThat(paymentDateOf(rows.get(0))).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(paymentDateOf(rows.get(11))).isEqualTo(LocalDate.of(2027, 6, 1));
        for (int i = 0; i < 12; i++) {
            assertThat(paymentDateOf(rows.get(i)))
                    .as("%d 회차", i + 1)
                    .isEqualTo(LocalDate.of(2026, 7, 1).plusMonths(i));
        }
    }

    @Test
    @DisplayName("#22 12월에 시작해도 해가 넘어가며 매월 1일이다")
    void datesRollOverTheYearBoundary() throws Exception {
        Fixture fixture = prepare();

        long groupId = groupIdOf(createInstallment(fixture, 50000L, 3, "2026-12"));

        List<Map<String, Object>> rows = rowsOf(groupId);
        assertThat(paymentDateOf(rows.get(0))).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(paymentDateOf(rows.get(1))).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(paymentDateOf(rows.get(2))).isEqualTo(LocalDate.of(2027, 2, 1));
    }

    @Test
    @DisplayName("#24 응답은 installmentGroupId·createdCount 두 칸이고 지출 목록이 없다")
    void responseCarriesOnlyTheGroupSummary() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = createInstallment(fixture, 100000L, 12, "2026-07").get("data");

        assertThat(data.get("createdCount").asInt()).isEqualTo(12);
        assertThat(data.has("installmentGroupId")).isTrue();
        assertThat(data.size()).isEqualTo(2);
        assertThat(data.has("list")).isFalse();
    }

    @Test
    @DisplayName("#25 할부 회차 하나를 3.2 로 상세 조회한다 — 일시불과 같은 API 다")
    void anInstallmentRowIsReadByTheSameApi() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long thirdRowId = ((Number) rowsOf(groupId).get(2).get("idx")).longValue();

        JsonNode response = getJson(EXPENSE_URL + "/" + thirdRowId, fixture.token());

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("installmentGroupId").asLong()).isEqualTo(groupId);
        assertThat(data.get("installmentIndex").asInt()).isEqualTo(3);
        assertThat(data.get("installmentTotal").asInt()).isEqualTo(12);
        assertThat(data.get("paymentMethodName").asString()).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#28 months=1 이면 3204 다 — 1개월은 일시불이다")
    void oneMonthIs3204() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createInstallment(fixture, 100000L, 1, "2026-07"))).isEqualTo(3204);
        assertThat(resCode(createInstallment(fixture, 100000L, 0, "2026-07"))).isEqualTo(3204);
    }

    @Test
    @DisplayName("#28 월 납부액이 0 이하면 3204 다 — 3201 이 아니다")
    void nonPositiveAmountIs3204() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createInstallment(fixture, 0L, 12, "2026-07"))).isEqualTo(3204);
        assertThat(resCode(createInstallment(fixture, -1000L, 12, "2026-07"))).isEqualTo(3204);
    }

    @Test
    @DisplayName("startYearMonth 형식이 YYYY-MM 이 아니면 3204 다")
    void malformedStartYearMonthIs3204() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createInstallment(fixture, 100000L, 12, "2026-13"))).isEqualTo(3204);
        assertThat(resCode(createInstallment(fixture, 100000L, 12, "2026/07"))).isEqualTo(3204);
    }

    @Test
    @DisplayName("사용 안 함 수단·유형으로 할부를 등록하면 3003·3103 이다")
    void unusableReferencesAreRejected() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        assertThat(resCode(createInstallment(fixture, 100000L, 12, "2026-07"))).isEqualTo(3003);
        // 거절했으면 한 행도 생기지 않아야 한다.
        assertThat(countExpenses(fixture.member())).isZero();
    }

    @Test
    @DisplayName("등록 실패 시 한 행도 남지 않는다 — 참조 검증이 채번보다 먼저다")
    void rejectedRequestCreatesNoRows() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(createInstallment(fixture, 100000L, 1, "2026-07"))).isEqualTo(3204);

        assertThat(countExpenses(fixture.member())).isZero();
    }

    @Test
    @DisplayName("두 할부 그룹은 서로 다른 식별자를 받는다 — 시퀀스가 그룹당 한 번 돈다")
    void eachGroupGetsItsOwnIdentifier() throws Exception {
        Fixture fixture = prepare();

        long first = groupIdOf(createInstallment(fixture, 100000L, 3, "2026-07"));
        long second = groupIdOf(createInstallment(fixture, 50000L, 4, "2026-09"));

        assertThat(first).isNotEqualTo(second);
        assertThat(countRows(first)).isEqualTo(3);
        assertThat(countRows(second)).isEqualTo(4);
    }

    @Test
    @DisplayName("필수 필드를 빠뜨리면 9001 이다")
    void missingRequiredFieldIs9001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(INSTALLMENT_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"monthlyAmount":100000}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId()))))
                .isEqualTo(9001);
    }

    @Test
    @DisplayName("토큰 없이 등록하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        assertThat(resCode(postJson(INSTALLMENT_URL, null, """
                {"paymentMethodId":1,"expendGroupId":1,"monthlyAmount":100000,
                 "installmentMonths":12,"startYearMonth":"2026-07",
                 "place":"백화점","content":"노트북 할부"}
                """))).isEqualTo(1001);
    }
}

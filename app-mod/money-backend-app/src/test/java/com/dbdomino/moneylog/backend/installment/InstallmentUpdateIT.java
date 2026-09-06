package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 할부 회차의 수정·삭제 — quickstart #26·#27 (FR-313·FR-314).
 *
 * <p>할부 회차는 <b>일시불과 같은 API</b>(3.3·3.4)로 다루며 대상은 언제나 <b>그 달 1건</b>
 * 이다. 그룹 전체를 한 번에 바꾸는 연산은 없다 — 남은 회차 정리는 중도상환(3.6)이 맡는다.
 *
 * <p><b>#27 은 할부 건이 아닌 일시불에도 적용된다</b>(api-contract.md §6 의 "할부 건이든
 * 아니든"). 할부 구조를 바꾸려는 요청은 대상이 무엇이든 {@code 3203} 이다.
 */
class InstallmentUpdateIT extends AbstractInstallmentIT {

    @Test
    @DisplayName("#26 할부 회차 하나를 수정하면 그 달 1건만 바뀐다")
    void updatingOneRowLeavesTheRestAlone() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        List<Map<String, Object>> before = rowsOf(groupId);
        long thirdRowId = ((Number) before.get(2).get("idx")).longValue();

        JsonNode response = patchJson(EXPENSE_URL + "/" + thirdRowId, fixture.token(), """
                {"amount":55000,"content":"3회차만 조정"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        List<Map<String, Object>> after = rowsOf(groupId);
        assertThat(after.get(2).get("amount")).isEqualTo(55000L);
        // 나머지 11건은 그대로다.
        for (int i = 0; i < 12; i++) {
            if (i == 2) {
                continue;
            }
            assertThat(after.get(i).get("amount")).as("%d 회차", i + 1).isEqualTo(100000L);
        }
        assertThat(after).hasSize(12);
    }

    @Test
    @DisplayName("#26 할부 회차 하나를 삭제하면 그 달 1건만 사라진다")
    void deletingOneRowLeavesTheRestAlone() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long fifthRowId = ((Number) rowsOf(groupId).get(4).get("idx")).longValue();

        assertThat(resCode(deleteJson(EXPENSE_URL + "/" + fifthRowId, fixture.token())))
                .isEqualTo(200);

        assertThat(countRows(groupId)).isEqualTo(11);
        // 남은 회차의 순번은 다시 매기지 않는다 — 5회차가 빠진 채로 1..4, 6..12 다.
        assertThat(rowsOf(groupId).stream().map(row -> row.get("installment_index")))
                .containsExactly(1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    @DisplayName("#26 회차를 수정해도 할부 3컬럼은 그대로다")
    void updatingDoesNotTouchInstallmentColumns() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long thirdRowId = ((Number) rowsOf(groupId).get(2).get("idx")).longValue();

        assertThat(resCode(patchJson(EXPENSE_URL + "/" + thirdRowId, fixture.token(), """
                {"amount":55000}
                """))).isEqualTo(200);

        Map<String, Object> row = rowsOf(groupId).get(2);
        assertThat(row.get("installment_group_id")).isEqualTo(groupId);
        assertThat(row.get("installment_index")).isEqualTo(3);
        assertThat(row.get("installment_total")).isEqualTo(12);
    }

    @Test
    @DisplayName("#27 할부 개월 수를 바꾸려 하면 3203 이다")
    void changingInstallmentTotalIs3203() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long firstRowId = ((Number) rowsOf(groupId).get(0).get("idx")).longValue();

        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"installmentTotal":6}
                """))).isEqualTo(3203);
        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"installmentMonths":6}
                """))).isEqualTo(3203);
    }

    @Test
    @DisplayName("#27 시작 연월·회차·그룹을 바꾸려 해도 3203 이다")
    void changingAnyInstallmentFieldIs3203() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long firstRowId = ((Number) rowsOf(groupId).get(0).get("idx")).longValue();

        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"startYearMonth":"2026-09"}
                """))).isEqualTo(3203);
        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"installmentIndex":5}
                """))).isEqualTo(3203);
        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"installmentGroupId":99}
                """))).isEqualTo(3203);
    }

    @Test
    @DisplayName("#27 할부 필드를 보내면 다른 필드가 정상이어도 3203 이다 — 판정이 먼저다")
    void installmentFieldRejectsTheWholeRequest() throws Exception {
        Fixture fixture = prepare();
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));
        long firstRowId = ((Number) rowsOf(groupId).get(0).get("idx")).longValue();

        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, fixture.token(), """
                {"amount":55000,"installmentTotal":6}
                """))).isEqualTo(3203);

        // 거절했으면 금액도 바뀌지 않는다.
        assertThat(rowsOf(groupId).get(0).get("amount")).isEqualTo(100000L);
    }

    @Test
    @DisplayName("#27 일시불 지출에 할부 필드를 보내도 3203 이다 — 할부 건이든 아니든")
    void installmentFieldOnALumpSumIsAlso3203() throws Exception {
        Fixture fixture = prepare();
        JsonNode created = postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId()));
        long expenseId = created.get("data").get("expenseId").asLong();

        // 9001(허용하지 않는 필드)이 아니라 3203 이어야 한다 — 명세가 그렇게 정했다.
        assertThat(resCode(patchJson(EXPENSE_URL + "/" + expenseId, fixture.token(), """
                {"installmentTotal":6}
                """))).isEqualTo(3203);
    }

    @Test
    @DisplayName("남의 할부 회차를 수정하면 3202 다 — 소유자 판정이 3203 보다 먼저다")
    void ownershipIsCheckedBeforeInstallmentFields() throws Exception {
        Fixture owner = prepare();
        long groupId = groupIdOf(createInstallment(owner, 100000L, 12, "2026-07"));
        long firstRowId = ((Number) rowsOf(groupId).get(0).get("idx")).longValue();
        Fixture intruder = prepare();

        assertThat(resCode(patchJson(EXPENSE_URL + "/" + firstRowId, intruder.token(), """
                {"installmentTotal":6}
                """))).isEqualTo(3202);
    }
}

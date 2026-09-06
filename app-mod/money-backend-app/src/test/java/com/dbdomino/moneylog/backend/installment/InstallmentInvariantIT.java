package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 할부 3컬럼의 불변식 — quickstart #33.
 *
 * <p><b>셋 다 비거나 셋 다 채워져야 한다.</b> DB 는 이것을 막지 않는다 — CHECK 2건이
 * 각 컬럼의 <b>범위만</b> 보고({@code installment_index} 는 NULL 또는 1 이상,
 * {@code installment_total} 은 NULL 또는 2 이상) 세 컬럼의 동시성을 보는 제약이 없다.
 * 즉 {@code installment_index} 만 채운 행을 DB 가 받아들인다.
 *
 * <p>그런 행이 생기면 "일시불인가 할부인가"를 판정할 수 없고, 조회·집계·중도상환이 전부
 * 그 판정에 의존하므로 <b>조용히 틀린 답</b>이 나온다.
 *
 * <p>API 응답으로는 확인할 수 없어 <b>SQL 로 직접 센다</b>. 저장 진입점 셋 중 둘
 * (3.1 일시불 · 3.5 할부)을 모두 거친 뒤에 확인한다 — 3.12 엑셀은 US4 에서 붙는다.
 */
class InstallmentInvariantIT extends AbstractInstallmentIT {

    /**
     * 세 컬럼이 어긋난 행의 수. <b>0이어야 한다.</b>
     *
     * <p>{@code (a is null) <> (b is null)} 은 "한쪽만 NULL"을 뜻한다. 두 비교를 OR 로
     * 묶으면 세 컬럼 중 어느 조합이 어긋나도 걸린다.
     */
    private int countInconsistentRows() {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_expense
                 where (installment_group_id is null) <> (installment_index is null)
                    or (installment_group_id is null) <> (installment_total is null)
                """, Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("#33 일시불과 할부를 모두 만든 뒤에도 부분 채움 행이 0건이다")
    void noRowHasPartiallyFilledInstallmentColumns() throws Exception {
        Fixture fixture = prepare();

        // 3.1 일시불 — 세 컬럼이 전부 NULL 이어야 한다.
        assertThat(resCode(postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId())))).isEqualTo(200);

        // 3.5 할부 — 세 컬럼이 전부 채워져야 한다.
        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));

        // 3.3 수정과 3.6 중도상환을 거친 뒤에도 유지되는지 함께 본다.
        long thirdRowId = ((Number) rowsOf(groupId).get(2).get("idx")).longValue();
        assertThat(resCode(patchJson(EXPENSE_URL + "/" + thirdRowId, fixture.token(), """
                {"amount":55000}
                """))).isEqualTo(200);

        assertThat(countInconsistentRows())
                .as("셋 다 비거나 셋 다 채워져야 한다 — 부분 채움은 판정 불가 상태다")
                .isZero();
    }

    @Test
    @DisplayName("#33 일시불 등록은 할부 3컬럼을 전부 비운다")
    void lumpSumLeavesAllThreeNull() throws Exception {
        Fixture fixture = prepare();

        var created = postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId()));
        long expenseId = created.get("data").get("expenseId").asLong();

        var row = jdbc.queryForMap("""
                select installment_group_id, installment_index, installment_total
                  from moneylog.tbl_expense where idx = ?
                """, expenseId);
        assertThat(row.get("installment_group_id")).isNull();
        assertThat(row.get("installment_index")).isNull();
        assertThat(row.get("installment_total")).isNull();
    }

    @Test
    @DisplayName("#33 할부 등록은 세 컬럼을 전부 채우고 회차가 1..N 으로 빠짐없다")
    void installmentFillsAllThree() throws Exception {
        Fixture fixture = prepare();

        long groupId = groupIdOf(createInstallment(fixture, 100000L, 12, "2026-07"));

        var rows = rowsOf(groupId);
        assertThat(rows).hasSize(12);
        for (int i = 0; i < 12; i++) {
            assertThat(rows.get(i).get("installment_group_id")).isNotNull();
            assertThat(rows.get(i).get("installment_index")).isEqualTo(i + 1);
            assertThat(rows.get(i).get("installment_total")).isEqualTo(12);
        }
        assertThat(countInconsistentRows()).isZero();
    }
}

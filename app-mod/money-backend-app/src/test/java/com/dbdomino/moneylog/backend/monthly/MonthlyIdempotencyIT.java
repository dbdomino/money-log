package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 같은 달을 여러 번 열어도 행이 늘지 않는다 — quickstart #14 (SC-402 · FR-407).
 *
 * <p>lazy 생성 모델의 기본 전제다. 조회가 쓰기를 일으키므로 <b>조회 횟수가 곧 삽입
 * 시도 횟수</b>이고, 그 시도가 전부 흡수되어야 한다.
 *
 * <p>흡수하는 것은 애플리케이션 검사가 아니라 <b>{@code ON CONFLICT DO NOTHING} 과
 * 유니크 제약</b>이다({@code ux_fixed_expense_monthly}). 애플리케이션 검사만 두면
 * 순차 호출에서는 통과하지만 동시 호출에서 깨진다 — 그쪽은 {@code MonthlyConcurrencyIT}
 * 가 본다.
 */
class MonthlyIdempotencyIT extends AbstractMonthlyIT {

    @Test
    @DisplayName("#14 같은 달을 100번 조회해도 행은 1건이다")
    void hundredReadsLeaveExactlyOneRow() throws Exception {
        Fixture fixture = prepare();

        for (int i = 0; i < 100; i++) {
            assertThat(resCode(listMonthly(fixture, 2026, 11))).isEqualTo(200);
        }

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#14 100번 조회해도 응답 목록이 1건이다 — 중복이 응답에도 없다")
    void hundredReadsReturnOneItem() throws Exception {
        Fixture fixture = prepare();

        JsonNode last = null;
        for (int i = 0; i < 100; i++) {
            last = listMonthly(fixture, 2026, 11);
        }

        assertThat(last.get("data").get("list")).hasSize(1);
        assertThat(last.get("data").get("total").asLong()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#14 두 번째 조회가 첫 번째가 만든 행을 그대로 돌려준다")
    void secondReadReturnsTheSameRow() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        Object firstIdx = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("idx");

        listMonthly(fixture, 2026, 11);

        // 지우고 다시 만들면 idx 가 바뀐다. 그러면 사용자가 직접 고친 값도 함께 날아간다.
        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11).get("idx"))
                .isEqualTo(firstIdx);
    }

    @Test
    @DisplayName("#14 재조회가 사용자가 고친 값을 덮지 않는다")
    void rereadDoesNotOverwriteModifiedRows() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        markMonthlyModified(fixture.member(), fixture.fixedExpenseId(), 2026, 11);

        listMonthly(fixture, 2026, 11);

        // ON CONFLICT DO NOTHING 이 아니라 UPSERT(DO UPDATE)로 짜면 여기서 modified 가
        // 내려가고 금액이 설정값으로 되돌아간다.
        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("modified")).isEqualTo(true);
    }

    @Test
    @DisplayName("서로 다른 달은 각각 만들어진다")
    void differentMonthsEachGetTheirOwnRow() throws Exception {
        Fixture fixture = prepare();

        listMonthly(fixture, 2026, 11);
        listMonthly(fixture, 2026, 12);
        listMonthly(fixture, 2027, 1);

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
        assertThat(countMonthly(fixture.member(), 2026, 12)).isEqualTo(1);
        assertThat(countMonthly(fixture.member(), 2027, 1)).isEqualTo(1);
        assertThat(countMonthlyAll(fixture.member())).isEqualTo(3);
    }
}

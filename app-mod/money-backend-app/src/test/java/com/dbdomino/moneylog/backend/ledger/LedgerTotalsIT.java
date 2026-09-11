package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 합계 — quickstart #50 (FR-423 · ledger-list.md § 정한 것).
 *
 * <p><b>{@code expenseTotal} 은 일반 + 할부 + 고정 셋을 합친 값이다.</b> 할부를 빠뜨리면
 * 이번 달 카드값이 빠지고, 고정지출을 빠뜨리면 월세가 빠진다 — 둘 다 화면 상단 숫자가
 * 조용히 작아지는 종류의 오류다.
 *
 * <p><b>합계는 필터와 무관하다.</b> {@code type=INCOME} 으로 조회해도 {@code expenseTotal}
 * 은 그 달 전체 지출이다. 합계는 "그 달은 얼마 쓰고 얼마 벌었나"라는 화면 상단 요약이고
 * 필터는 아래 목록을 좁히는 도구다 — 필터를 걸 때마다 상단 숫자가 흔들리면 사용자가
 * 기준을 잃는다.
 *
 * <p>그래서 <b>{@code list} 의 합과 {@code expenseTotal} 이 다를 수 있고 그것이 의도다.</b>
 * 프론트가 "보이는 것의 합"을 따로 보여주고 싶으면 {@code list} 를 더하면 된다.
 */
class LedgerTotalsIT extends AbstractLedgerIT {

    /** 준비된 그 달의 지출 합계 — 일반 12000 + 할부 1회차 100000 + 고정 500000. */
    private static final long EXPENSE_TOTAL = 12000L + 100000L + 500000L;

    /** 준비된 그 달의 소득 합계. */
    private static final long INCOME_TOTAL = 3000000L;

    @Test
    @DisplayName("#50 expenseTotal 이 일반+할부+고정 합계다")
    void expenseTotalSumsThreeKinds() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture).get("data");

        assertThat(data.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
        assertThat(data.get("incomeTotal").asLong()).isEqualTo(INCOME_TOTAL);
    }

    @Test
    @DisplayName("#50 소득이 expenseTotal 에 섞이지 않는다")
    void incomeIsNotCountedAsExpense() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture).get("data");

        // 3000000 이 섞이면 612000 이 아니라 3612000 이 된다.
        assertThat(data.get("expenseTotal").asLong()).isLessThan(INCOME_TOTAL);
    }

    @Test
    @DisplayName("#50 type=INCOME 으로 걸러도 expenseTotal 은 그대로다")
    void totalsIgnoreTheTypeFilter() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture, "type=INCOME").get("data");

        assertThat(data.get("list")).hasSize(1);
        // 필터된 결과 기준이면 여기서 0 이 나온다.
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
        assertThat(data.get("incomeTotal").asLong()).isEqualTo(INCOME_TOTAL);
    }

    @Test
    @DisplayName("#50 type=EXPENSE 로 걸러도 incomeTotal 은 그대로다")
    void incomeTotalSurvivesAnExpenseOnlyFilter() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture, "type=EXPENSE").get("data");

        assertThat(data.get("list")).hasSize(1);
        assertThat(data.get("incomeTotal").asLong()).isEqualTo(INCOME_TOTAL);
    }

    @Test
    @DisplayName("#50 날짜·검색 필터도 합계를 바꾸지 않는다")
    void dateAndKeywordFiltersDoNotChangeTotals() throws Exception {
        Fixture fixture = prepare();

        JsonNode byDate = ledger(fixture, "dateFrom=2026-07-15&dateTo=2026-07-15").get("data");
        JsonNode byKeyword = ledger(fixture, "keyword=점심").get("data");

        assertThat(byDate.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
        assertThat(byKeyword.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
        assertThat(byKeyword.get("incomeTotal").asLong()).isEqualTo(INCOME_TOTAL);
    }

    @Test
    @DisplayName("#50 필터를 걸면 list 의 합과 expenseTotal 이 달라진다 — 의도한 결과다")
    void listSumMayDifferFromTotal() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture, "type=EXPENSE").get("data");

        long listSum = 0;
        for (JsonNode item : data.get("list")) {
            listSum += item.get("amount").asLong();
        }
        assertThat(listSum).isEqualTo(12000L);
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
        assertThat(listSum).isNotEqualTo(data.get("expenseTotal").asLong());
    }

    @Test
    @DisplayName("#50 할부는 그 달 회차만 센다 — 총액이 아니다")
    void installmentCountsOnlyThisMonthsSlice() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture).get("data");

        // 3개월 × 100000 = 300000 이 아니라 그 달 회차 100000 만 들어간다.
        // 총액을 세면 이번 달 지출이 세 배로 부푼다.
        assertThat(data.get("expenseTotal").asLong()).isEqualTo(EXPENSE_TOTAL);
    }

    @Test
    @DisplayName("#50 4.8 이 만든 고정지출 행이 합계에 즉시 반영된다")
    void lazilyCreatedFixedRowIsCountedInTheSameCall() throws Exception {
        Fixture fixture = prepare();
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isZero();

        // 첫 호출이 고정지출 행을 만들고, 그 500000 이 같은 응답의 합계에 들어가야 한다.
        assertThat(ledger(fixture).get("data").get("expenseTotal").asLong())
                .isEqualTo(EXPENSE_TOTAL);
    }
}

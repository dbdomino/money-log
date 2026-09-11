package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 필터와 정렬 — quickstart #45·#46·#47·#48 (FR-420).
 *
 * <p><b>#47 이 "출처마다 컬럼이 다르다"의 결과다.</b> {@code expendGroupId} 필터가 걸리면
 * {@code INCOME} 행이 <b>전부</b> 빠진다 — 소득에 지출유형이 없어 "그 유형의 거래"에 낄
 * 자리가 없기 때문이다. 값이 없어서가 아니라 컬럼이 아예 없어서다.
 */
class LedgerFilterSortIT extends AbstractLedgerIT {

    /** 응답 목록의 결제일들. 정렬 확인에 쓴다. */
    private List<String> datesOf(JsonNode response) {
        List<String> dates = new ArrayList<>();
        for (JsonNode item : response.get("data").get("list")) {
            dates.add(item.get("paymentDate").asString());
        }
        return dates;
    }

    /** 응답 목록의 금액들. */
    private List<Long> amountsOf(JsonNode response) {
        List<Long> amounts = new ArrayList<>();
        for (JsonNode item : response.get("data").get("list")) {
            amounts.add(item.get("amount").asLong());
        }
        return amounts;
    }

    @Test
    @DisplayName("#45 type=EXPENSE,INSTALLMENT 는 두 종류만 돌려준다 — 콤마 복수 지정")
    void commaSeparatedTypeFilter() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture, "type=EXPENSE,INSTALLMENT");

        assertThat(typesOf(response)).containsExactlyInAnyOrder("EXPENSE", "INSTALLMENT");
    }

    @Test
    @DisplayName("#45 type 하나만 지정할 수도 있다")
    void singleTypeFilter() throws Exception {
        Fixture fixture = prepare();

        assertThat(typesOf(ledger(fixture, "type=FIXED"))).containsExactly("FIXED");
        assertThat(typesOf(ledger(fixture, "type=INCOME"))).containsExactly("INCOME");
    }

    @Test
    @DisplayName("#45 type 을 생략하면 전부 나온다")
    void omittedTypeMeansEverything() throws Exception {
        Fixture fixture = prepare();

        assertThat(ledger(fixture).get("data").get("list")).hasSize(4);
    }

    @Test
    @DisplayName("#45 모르는 type 값은 9001 이다 — 조용히 무시하지 않는다")
    void unknownTypeIs9001() throws Exception {
        Fixture fixture = prepare();

        // 오타(EXPENES)를 무시하면 "전부"로 읽혀 사용자가 필터를 걸었다고 믿는 채
        // 전체를 보게 된다.
        assertThat(resCode(ledger(fixture, "type=EXPENES"))).isEqualTo(9001);
        assertThat(resCode(ledger(fixture, "type=EXPENSE,NOPE"))).isEqualTo(9001);
    }

    @Test
    @DisplayName("#46 dateFrom·dateTo 가 그 달 안에서 결제일로 좁힌다")
    void dateRangeNarrowsWithinTheMonth() throws Exception {
        Fixture fixture = prepare();
        // 준비된 거래: 지출 07-15 · 할부 1회차 07-01 · 소득 07-25 · 고정지출 07-25
        JsonNode response = ledger(fixture, "dateFrom=2026-07-10&dateTo=2026-07-20");

        assertThat(datesOf(response)).containsExactly("2026-07-15");
    }

    @Test
    @DisplayName("#46 dateFrom·dateTo 는 양 끝을 포함한다")
    void dateRangeIncludesBothEnds() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture, "dateFrom=2026-07-15&dateTo=2026-07-15");

        assertThat(datesOf(response)).containsExactly("2026-07-15");
    }

    @Test
    @DisplayName("#46 dateFrom 만 줘도 된다")
    void dateFromAloneWorks() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture, "dateFrom=2026-07-20");

        // 07-25 두 건(소득·고정지출)만 남는다.
        assertThat(response.get("data").get("list")).hasSize(2);
    }

    @Test
    @DisplayName("#47 expendGroupId 필터가 걸리면 INCOME 행이 전부 빠진다")
    void expendGroupFilterDropsEveryIncomeRow() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture, "expendGroupId=" + fixture.expendGroupId());

        // 소득에는 지출유형이 없다 — 값이 없어서가 아니라 컬럼이 아예 없다.
        assertThat(typesOf(response)).doesNotContain("INCOME");
        assertThat(typesOf(response)).containsExactlyInAnyOrder("EXPENSE", "INSTALLMENT");
    }

    @Test
    @DisplayName("#48 sort·order 를 생략하면 paymentDate desc 다")
    void defaultSortIsPaymentDateDesc() throws Exception {
        Fixture fixture = prepare();

        List<String> dates = datesOf(ledger(fixture));

        // 최근 것이 위다. 07-25 둘 · 07-15 · 07-01 순.
        assertThat(dates).isSortedAccordingTo((left, right) -> right.compareTo(left));
        assertThat(dates.get(0)).isEqualTo("2026-07-25");
        assertThat(dates.get(dates.size() - 1)).isEqualTo("2026-07-01");
    }

    @Test
    @DisplayName("#48 order=asc 면 오래된 것이 위다")
    void ascendingReversesTheOrder() throws Exception {
        Fixture fixture = prepare();

        List<String> dates = datesOf(ledger(fixture, "order=asc"));

        assertThat(dates.get(0)).isEqualTo("2026-07-01");
        assertThat(dates.get(dates.size() - 1)).isEqualTo("2026-07-25");
    }

    @Test
    @DisplayName("#48 sort=amount 로 금액순 정렬한다")
    void sortByAmount() throws Exception {
        Fixture fixture = prepare();

        List<Long> descending = amountsOf(ledger(fixture, "sort=amount"));
        List<Long> ascending = amountsOf(ledger(fixture, "sort=amount&order=asc"));

        // 준비된 금액: 지출 12000 · 할부 100000 · 소득 3000000 · 고정지출 500000
        assertThat(descending).containsExactly(3000000L, 500000L, 100000L, 12000L);
        assertThat(ascending).containsExactly(12000L, 100000L, 500000L, 3000000L);
    }

    @Test
    @DisplayName("#48 모르는 sort·order 값은 9001 이다")
    void unknownSortOrOrderIs9001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(ledger(fixture, "sort=place"))).isEqualTo(9001);
        assertThat(resCode(ledger(fixture, "order=up"))).isEqualTo(9001);
    }

    @Test
    @DisplayName("같은 날짜의 행들도 순서가 흔들리지 않는다")
    void tiesAreBrokenDeterministically() throws Exception {
        Fixture fixture = prepare();

        // 07-25 가 둘(소득·고정지출)이다. 매 호출 같은 순서여야 화면이 이유 없이
        // 다르게 보이지 않는다.
        List<String> first = itemIdsOf(ledger(fixture));
        List<String> second = itemIdsOf(ledger(fixture));

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("keyword 는 장소·내용을 보고 대소문자를 무시한다")
    void keywordSearchesPlaceAndContentIgnoringCase() throws Exception {
        Fixture fixture = prepare();

        // 준비된 지출: place="편의점", content="점심"
        assertThat(ledger(fixture, "keyword=편의점").get("data").get("list")).hasSize(1);
        assertThat(ledger(fixture, "keyword=점심").get("data").get("list")).hasSize(1);
        // 소득: content="급여". 소득에는 장소가 없어 내용만 본다.
        assertThat(typesOf(ledger(fixture, "keyword=급여"))).containsExactly("INCOME");
    }

    @Test
    @DisplayName("keyword 가 대소문자를 무시한다 — 자기가 적은 것을 자기가 못 찾으면 안 된다")
    void keywordIsCaseInsensitive() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":5500,
                 "paymentDate":"2026-07-18","place":"Starbucks","content":"커피"}
                """.formatted(fixture.expenseMethodId(), fixture.expendGroupId()))))
                .isEqualTo(200);

        assertThat(ledger(fixture, "keyword=starbucks").get("data").get("list")).hasSize(1);
        assertThat(ledger(fixture, "keyword=STARBUCKS").get("data").get("list")).hasSize(1);
    }

    @Test
    @DisplayName("걸리는 것이 없는 필터는 빈 배열이다 — 오류가 아니다")
    void filterWithNoMatchIsAnEmptyList() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture, "keyword=없는말");

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("list")).isEmpty();
    }
}

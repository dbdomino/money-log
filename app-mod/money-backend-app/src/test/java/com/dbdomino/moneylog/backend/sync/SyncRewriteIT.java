package com.dbdomino.moneylog.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.9 재작성의 네 처리 — quickstart #34·#36·#37 (FR-413·414·415).
 *
 * <p><b>#37 이 ④(삭제)를 검증한다.</b> 자동 반영(FR-412)은 값 갱신만 하고 삭제하지
 * 않으므로, 적용 기간을 줄이면 기간 밖이 된 행이 남는다. 그 정리를 재작성이 맡는 것이
 * 이 API 가 필요한 이유 중 하나다.
 *
 * <p><b>#34 는 지난 달을 대상으로 한다.</b> 자동 반영이 미래 달만 건드리는 것과 달리
 * 재작성은 <b>지정한 한 연·월</b>이면 지난 달도 맞춘다(FR-413) — "지난 달을 새 설정값으로
 * 맞추고 싶다"에 답하는 명시적 경로다.
 */
class SyncRewriteIT extends AbstractSyncIT {

    /** 4.9 호출. 연·월을 <b>Body</b> 로 보낸다. */
    private JsonNode sync(Fixture fixture, YearMonth yearMonth) throws Exception {
        return postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(yearMonth.getYear(), yearMonth.getMonthValue()));
    }

    /** 설정의 적용 기간 종료를 당긴다. 그 뒤의 달이 기간 밖이 된다. */
    private void shrinkPeriodTo(Fixture fixture, YearMonth end) throws Exception {
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"endYear":%d,"endMonth":%d}
                        """.formatted(end.getYear(), end.getMonthValue())))).isEqualTo(200);
    }

    @Test
    @DisplayName("#34 지난 달을 재작성하면 관리 값으로 갱신된다")
    void rewritingLastMonthAppliesTheSetting() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, lastMonth());
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":900000}
                        """))).isEqualTo(200);
        // 자동 반영은 지난 달을 건드리지 않았다.
        assertThat(amountOf(fixture, lastMonth())).isEqualTo(500000L);

        JsonNode response = sync(fixture, lastMonth());

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("updatedCount").asInt()).isEqualTo(1);
        assertThat(amountOf(fixture, lastMonth())).isEqualTo(900000L);
    }

    @Test
    @DisplayName("#34 아직 없는 달을 재작성하면 ①생성이 일어난다")
    void rewritingAnUnopenedMonthCreatesRows() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(3);
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isZero();

        JsonNode data = sync(fixture, target).get("data");

        assertThat(data.get("createdCount").asInt()).isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).isZero();
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(1);
    }

    @Test
    @DisplayName("#34 이미 연 달에 새로 등록한 고정지출이 재작성으로 들어온다")
    void newlyRegisteredSettingIsPickedUp() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());
        createFixedExpense(fixture.token(), "통신비", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "통신"), 60000L, 10, wideStart(), wideEnd());

        JsonNode data = sync(fixture, thisMonth()).get("data");

        // ①생성이 신규 등록분을 잡는다. 기존 1건은 modified=false 라 ②갱신이다.
        assertThat(data.get("createdCount").asInt()).isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).isEqualTo(1);
        assertThat(data.get("list")).hasSize(2);
    }

    @Test
    @DisplayName("#36 응답에 네 건수가 전부 온다")
    void responseCarriesAllFourCounts() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        JsonNode data = sync(fixture, thisMonth()).get("data");

        assertThat(data.has("createdCount")).isTrue();
        assertThat(data.has("updatedCount")).isTrue();
        assertThat(data.has("keptCount")).isTrue();
        assertThat(data.has("deletedCount")).isTrue();
    }

    @Test
    @DisplayName("#36 결과 목록이 함께 온다 — 재조회가 필요 없다")
    void responseCarriesTheResultingList() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        JsonNode data = sync(fixture, thisMonth()).get("data");

        assertThat(data.get("list")).hasSize(1);
        // 4.5 의 list[] 와 같은 구조라 프론트가 같은 렌더링 코드를 쓴다.
        JsonNode item = data.get("list").get(0);
        assertThat(item.get("fixedExpenseId").asLong()).isEqualTo(fixture.fixedExpenseId());
        assertThat(item.get("fixedExpenseName").asString()).isEqualTo("월세");
        assertThat(item.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(item.has("modified")).isTrue();
    }

    @Test
    @DisplayName("#36 year·month·total 도 함께 온다")
    void responseCarriesYearMonthAndTotal() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        JsonNode data = sync(fixture, thisMonth()).get("data");

        assertThat(data.get("year").asInt()).isEqualTo(thisMonth().getYear());
        assertThat(data.get("month").asInt()).isEqualTo(thisMonth().getMonthValue());
        assertThat(data.get("total").asLong()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#37 적용 기간을 줄인 뒤 기간 밖 달을 재작성하면 그 행이 삭제된다")
    void rowsOutsideTheShrunkPeriodAreDeleted() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);
        openMonth(fixture, target);
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(1);

        // 종료를 당겨 target 을 기간 밖으로 밀어낸다.
        shrinkPeriodTo(fixture, futureMonth(2));

        JsonNode data = sync(fixture, target).get("data");

        assertThat(data.get("deletedCount").asInt()).isEqualTo(1);
        assertThat(data.get("list")).isEmpty();
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isZero();
    }

    @Test
    @DisplayName("#37 자동 반영은 그 행을 지우지 않는다 — 그래서 4.9 가 필요하다")
    void propagationLeavesOutOfRangeRowsBehind() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);
        openMonth(fixture, target);

        // 이 PATCH 가 자동 반영을 일으키지만 삭제는 하지 않는다.
        shrinkPeriodTo(fixture, futureMonth(2));

        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(1);
    }

    @Test
    @DisplayName("#37 기간 밖 행의 값은 자동 반영이 건드리지도 않는다")
    void outOfRangeRowKeepsItsOldValue() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);
        openMonth(fixture, target);

        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":900000,"endYear":%d,"endMonth":%d}
                        """.formatted(futureMonth(2).getYear(),
                        futureMonth(2).getMonthValue())))).isEqualTo(200);

        // 이미 설정의 대상이 아닌 달이라 새 값을 씌울 근거가 없다.
        assertThat(amountOf(fixture, target)).isEqualTo(500000L);
    }

    @Test
    @DisplayName("재작성은 다른 달을 건드리지 않는다")
    void rewritingOneMonthLeavesOthersAlone() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":900000}
                        """))).isEqualTo(200);

        sync(fixture, lastMonth());

        assertThat(amountOf(fixture, lastMonth())).isEqualTo(900000L);
        // 이번 달은 자동 반영도 재작성도 건드리지 않았다.
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(500000L);
    }

    @Test
    @DisplayName("연·월이 범위 밖이면 3403 이다 — 4.8 의 3501 이 아니다")
    void invalidYearMonthIs3403() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(postJson(SYNC_URL, fixture.token(), """
                {"year":2026,"month":13}
                """))).isEqualTo(3403);
        assertThat(resCode(postJson(SYNC_URL, fixture.token(), """
                {"month":7}
                """))).isEqualTo(3403);
        assertThat(resCode(postJson(SYNC_URL, fixture.token(), "{}"))).isEqualTo(3403);
    }

    @Test
    @DisplayName("토큰 없이 부르면 1001 이고 아무것도 바뀌지 않는다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());

        assertThat(resCode(postJson(SYNC_URL, null, """
                {"year":%d,"month":%d}
                """.formatted(thisMonth().getYear(), thisMonth().getMonthValue()))))
                .isEqualTo(1001);
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(500000L);
    }

    @Test
    @DisplayName("남의 달을 재작성해도 내 행만 대상이다")
    void rewriteIsScopedToTheOwner() throws Exception {
        Fixture other = prepare();
        openMonth(other, thisMonth());
        Fixture fixture = prepare();

        JsonNode data = sync(fixture, thisMonth()).get("data");

        assertThat(data.get("createdCount").asInt()).isEqualTo(1);
        assertThat(data.get("list")).hasSize(1);
        // 남의 행은 그대로다.
        assertThat(amountOf(other, thisMonth())).isEqualTo(500000L);
    }
}

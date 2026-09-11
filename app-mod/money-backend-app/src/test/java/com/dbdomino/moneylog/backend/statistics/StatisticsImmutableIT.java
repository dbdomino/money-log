package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 저장본 불변 — quickstart #39·#43·#44 (SC-506 · SC-508 · FR-518).
 *
 * <h2>세 갈래를 다 걸어야 확인된다</h2>
 *
 * <table border="1">
 *   <caption>저장 뒤에 무엇이 바뀌어도 저장본은 그대로다</caption>
 *   <tr><th>#</th><th>바뀌는 것</th><th>저장본에서 지켜지는 것</th></tr>
 *   <tr><td>39</td><td>원본 <b>지출</b></td><td>합계</td></tr>
 *   <tr><td>43</td><td><b>지출유형</b> 삭제 표시</td><td>유형별 행과 저장 당시 이름</td></tr>
 *   <tr><td>44</td><td><b>목표금액</b></td><td>목표·사용률·상태</td></tr>
 * </table>
 *
 * <p>셋이 각각 다른 방식으로 깨진다 — 합계는 다시 계산해서, 이름은 연관을 타서, 목표는
 * 조회 시점에 다시 풀어서. 하나만 걸면 나머지 둘이 통과한다.
 *
 * <p><b>저장본을 최신화하는 유일한 방법은 5.6 을 다시 부르는 것</b>이고, 그 사이에
 * {@code view=live} 로 지금 값을 볼 수 있다.
 */
class StatisticsImmutableIT extends AbstractStatisticsIT {

    @Test
    @DisplayName("#39 저장 후 그 달 지출을 고쳐도 저장본은 그대로다")
    void survivesExpenseChange() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());
        save(fixture, year, month);

        addExpense(fixture, lastMonth().atDay(10).toString(), 500_000L, fixture.foodGroupId());

        JsonNode saved = statistics(fixture, year, month).get("data");
        assertThat(saved.get("source").asText()).isEqualTo("SAVED");
        assertThat(saved.get("expenseTotal").asLong()).isEqualTo(30_000L);
    }

    /**
     * #50 — 같은 달의 저장본과 {@code view=live} 가 <b>다른 값</b>을 낸다.
     *
     * <p>원본을 고쳤으므로 달라야 정상이다. 같으면 저장본이 지금 값을 다시 읽고 있다는
     * 뜻이라 불변이 성립하지 않는다.
     */
    @Test
    @DisplayName("#50 저장본과 view=live 가 나란히 다른 값을 낸다")
    void savedAndLiveDiffer() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());
        save(fixture, year, month);
        addExpense(fixture, lastMonth().atDay(10).toString(), 500_000L, fixture.foodGroupId());

        JsonNode saved = statistics(fixture, year, month).get("data");
        JsonNode live = statistics(fixture, year, month, "live").get("data");

        assertThat(saved.get("expenseTotal").asLong()).isEqualTo(30_000L);
        assertThat(live.get("expenseTotal").asLong()).isEqualTo(530_000L);
        // live 여도 저장 시각은 함께 실려 화면이 둘을 나란히 보여줄 수 있다.
        assertThat(live.get("savedAt").isNull()).isFalse();
    }

    /**
     * #43 — 지출유형을 삭제 표시해도 유형별 요약이 <b>그대로 남는다</b>(SC-508).
     *
     * <p>통계 상세에 지출유형 FK 가 <b>없어서</b> 가능한 일이다(FR-519). FK 를 "빠진
     * 실수"로 보고 추가하면 삭제가 막히거나(RESTRICT) 과거 통계가 함께 사라진다(CASCADE).
     */
    @Test
    @DisplayName("#43 저장 후 지출유형을 삭제 표시해도 유형별 요약이 남는다")
    void survivesExpendGroupDeletion() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        long groupId = createExpendGroup(fixture.token(), "구독");
        long expenseId = addExpense(fixture, lastMonth().atDay(3).toString(), 20_000L, groupId);
        save(fixture, year, month);

        // 그 유형을 쓴 지출을 지워야 삭제 표시가 가능하다(3106).
        assertThat(resCode(deleteJson("/api/v1/expenses/" + expenseId, fixture.token())))
                .isEqualTo(200);
        assertThat(resCode(deleteJson("/api/v1/expend-groups/" + groupId, fixture.token())))
                .isEqualTo(200);

        JsonNode row = groupSummaryOf(statistics(fixture, year, month), groupId);
        assertThat(row).isNotNull();
        // 저장 당시 이름으로 읽힌다 — 원본이 사라져도 화면을 복원할 수 있다.
        assertThat(row.get("expendGroupName").asText()).isEqualTo("구독");
        assertThat(row.get("amount").asLong()).isEqualTo(20_000L);
    }

    /**
     * 유형 이름을 <b>바꿔도</b> 저장본은 저장 당시 이름을 유지한다.
     *
     * <p>매퍼가 연관을 타고 현재 이름을 읽으면 여기서 걸린다 — 006 안에서 목표금액은
     * 현재 이름, 통계는 스냅샷이라 헷갈리기 쉬운 지점이다.
     */
    @Test
    @DisplayName("저장 후 유형 이름을 바꿔도 저장본은 저장 당시 이름이다")
    void keepsNameSnapshot() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        long groupId = createExpendGroup(fixture.token(), "구독");
        addExpense(fixture, lastMonth().atDay(3).toString(), 20_000L, groupId);
        save(fixture, year, month);

        assertThat(resCode(renameExpendGroup(fixture.token(), groupId, "정기결제")))
                .isEqualTo(200);

        assertThat(groupSummaryOf(statistics(fixture, year, month), groupId)
                .get("expendGroupName").asText()).isEqualTo("구독");
    }

    /**
     * #44 — 목표금액을 바꿔도 저장본의 목표·사용률·상태는 <b>저장 당시 값</b>이다.
     *
     * <p>조회 시점에 적용 금액을 다시 푸는 구현이면 셋이 함께 움직여 여기서 걸린다.
     */
    @Test
    @DisplayName("#44 저장 후 목표금액을 바꿔도 저장본의 목표·사용률·상태는 그대로다")
    void survivesTargetChange() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        addExpense(fixture, lastMonth().atDay(3).toString(), 320_000L, fixture.foodGroupId());
        save(fixture, year, month);

        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 100_000L);

        JsonNode row = groupSummaryOf(statistics(fixture, year, month), fixture.foodGroupId());
        assertThat(row.get("target").asLong()).isEqualTo(400_000L);
        assertThat(row.get("usageRate").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(row.get("status").asText()).isEqualTo("UNDER");
        // 지금 기준으로 다시 풀면 320,000 / 100,000 = 320% 라 OVER 가 된다.
        assertThat(statistics(fixture, year, month, "live").get("data")
                .get("expendGroupSummaries").get(0).get("status").asText()).isEqualTo("OVER");
    }
}

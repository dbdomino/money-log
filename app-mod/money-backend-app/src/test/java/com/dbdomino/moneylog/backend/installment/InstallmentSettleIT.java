package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.6 중도상환 — quickstart #29·#30·#31·#32 (FR-315·SC-304·SC-309).
 *
 * <p><b>#29 가 이 기능에서 가장 중요한 경계 시나리오다.</b> 과거 2·오늘 1·미래 9회차인
 * 할부를 중도상환하면 미래 9건만 사라지고 <b>오늘 회차는 남는다</b>. 경계를 {@code >=} 로
 * 잡은 구현은 다른 시험을 전부 통과하고 이 하나에서만 걸리는데, 그 실패는 조용하다 —
 * 오류가 나는 것이 아니라 <b>이번 달 합계가 소급해 줄어든다</b>.
 *
 * <p>시작 연월을 오늘 기준으로 계산해 <b>실행 날짜에 흔들리지 않게</b> 만든다. 고정 연월을
 * 쓰면 그 날짜가 지나는 순간 "미래 회차"가 과거가 되어 시험이 의미를 잃는다.
 */
class InstallmentSettleIT extends AbstractInstallmentIT {

    /** 과거 {@code past}·이번 달 1·미래 {@code future} 회차인 할부를 만든다. */
    private long createSpanningToday(Fixture fixture, int past, int future) throws Exception {
        // 이번 달이 (past+1) 회차가 되도록 시작 연월을 잡는다.
        String start = startYearMonthSoThatTodayIs(past + 1);
        return groupIdOf(createInstallment(fixture, 100000L, past + 1 + future, start));
    }

    @Test
    @DisplayName("#29 과거 2·오늘 1·미래 9 에서 중도상환하면 미래 9건만 사라진다")
    void onlyFutureRowsAreRemoved() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);
        assertThat(countRows(groupId)).isEqualTo(12);

        JsonNode response = settle(fixture, groupId);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("settledCount").asLong()).isEqualTo(9L);
        assertThat(response.get("data").get("installmentGroupId").asLong()).isEqualTo(groupId);
        assertThat(response.get("data").get("message").asString()).isNotBlank();

        // 과거 2 + 오늘 1 = 3건이 남는다.
        assertThat(countRows(groupId)).isEqualTo(3);
    }

    @Test
    @DisplayName("#29 오늘이 결제일인 회차는 남는다 — 경계가 > 이지 >= 가 아니다")
    void todaysRowSurvives() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);
        LocalDate thisMonthFirst = LocalDate.now().withDayOfMonth(1);

        assertThat(resCode(settle(fixture, groupId))).isEqualTo(200);

        List<Map<String, Object>> remaining = rowsOf(groupId);
        // >= 로 잡은 구현이면 이번 달 회차가 사라져 2건만 남는다.
        assertThat(remaining).hasSize(3);
        assertThat(remaining.stream().map(this::paymentDateOf))
                .as("이번 달 회차가 남아 있어야 한다")
                .contains(thisMonthFirst);
        assertThat(remaining.stream().map(this::paymentDateOf))
                .allSatisfy(date -> assertThat(date).isBeforeOrEqualTo(LocalDate.now()));
    }

    @Test
    @DisplayName("#29 남은 회차의 순번은 앞쪽 1..3 이다 — 뒤에서 지운다")
    void survivingRowsAreTheEarlyIndexes() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);

        assertThat(resCode(settle(fixture, groupId))).isEqualTo(200);

        assertThat(rowsOf(groupId).stream().map(row -> row.get("installment_index")))
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("#30 그룹 두 개 중 하나만 상환하면 다른 그룹은 영향받지 않는다")
    void otherGroupsAreUntouched() throws Exception {
        Fixture fixture = prepare();
        long target = createSpanningToday(fixture, 2, 9);
        long other = createSpanningToday(fixture, 1, 5);
        int otherBefore = countRows(other);

        assertThat(resCode(settle(fixture, target))).isEqualTo(200);

        assertThat(countRows(target)).isEqualTo(3);
        assertThat(countRows(other)).isEqualTo(otherBefore);
    }

    @Test
    @DisplayName("#31 미래 회차가 0건인 그룹을 다시 상환하면 3207 이다 — 멱등 성공이 아니다")
    void settlingTwiceIs3207() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);

        assertThat(resCode(settle(fixture, groupId))).isEqualTo(200);

        // "방금 정리했다"와 "이미 정리되어 있었다"를 화면이 구분해야 한다.
        assertThat(resCode(settle(fixture, groupId))).isEqualTo(3207);
        assertThat(countRows(groupId)).as("거절했으면 남은 회차도 그대로다").isEqualTo(3);
    }

    @Test
    @DisplayName("#31 처음부터 미래 회차가 없는 그룹도 3207 이다")
    void aFullyPastGroupIs3207() throws Exception {
        Fixture fixture = prepare();
        // 3회차 전부가 과거가 되도록 시작 연월을 넉넉히 당긴다.
        long groupId = groupIdOf(createInstallment(
                fixture, 100000L, 3, startYearMonthSoThatTodayIs(6)));

        assertThat(resCode(settle(fixture, groupId))).isEqualTo(3207);
        assertThat(countRows(groupId)).isEqualTo(3);
    }

    @Test
    @DisplayName("#32 남의 installmentGroupId 로 상환하면 3206 이고 회차도 남는다")
    void settlingOthersIs3206() throws Exception {
        Fixture owner = prepare();
        long groupId = createSpanningToday(owner, 2, 9);
        Fixture intruder = prepare();

        assertThat(resCode(settle(intruder, groupId))).isEqualTo(3206);

        assertThat(countRows(groupId)).as("거절했으면 한 건도 지워지지 않는다").isEqualTo(12);
    }

    @Test
    @DisplayName("#32 없는 그룹도 같은 3206 이다 — 코드가 갈리면 존재 여부가 새어 나간다")
    void missingGroupIsAlso3206() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(settle(fixture, 999999999L))).isEqualTo(3206);
    }

    @Test
    @DisplayName("소유자 판정이 3207 보다 먼저다 — 남의 그룹에 남은 회차가 없어도 3206 이다")
    void ownershipIsCheckedBeforeRemainingCount() throws Exception {
        Fixture owner = prepare();
        long groupId = groupIdOf(createInstallment(
                owner, 100000L, 3, startYearMonthSoThatTodayIs(6)));
        Fixture intruder = prepare();

        // 3207 이 나오면 "그 그룹은 이미 정리됐다"는 사실이 남에게 새어 나간다.
        assertThat(resCode(settle(intruder, groupId))).isEqualTo(3206);
    }

    @Test
    @DisplayName("상환 후에도 남은 회차는 3.2 로 읽힌다")
    void survivingRowsRemainReadable() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);
        assertThat(resCode(settle(fixture, groupId))).isEqualTo(200);

        long firstRowId = ((Number) rowsOf(groupId).get(0).get("idx")).longValue();

        JsonNode data = getJson(EXPENSE_URL + "/" + firstRowId, fixture.token()).get("data");
        assertThat(data.get("installmentGroupId").asLong()).isEqualTo(groupId);
        assertThat(data.get("installmentIndex").asInt()).isEqualTo(1);
        // 총 개월은 등록 당시 값 그대로다 — 상환이 그것을 고치지 않는다.
        assertThat(data.get("installmentTotal").asInt()).isEqualTo(12);
    }

    @Test
    @DisplayName("토큰 없이 상환하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();
        long groupId = createSpanningToday(fixture, 2, 9);

        assertThat(resCode(patchJson(
                INSTALLMENT_URL + "/" + groupId + "/remainder", null, "{}"))).isEqualTo(1001);
    }
}

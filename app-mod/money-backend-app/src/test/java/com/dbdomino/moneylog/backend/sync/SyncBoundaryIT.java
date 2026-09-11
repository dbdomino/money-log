package com.dbdomino.moneylog.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 재작성의 경계 — 구현 검토에서 시험이 비어 있던 자리를 채운다.
 *
 * <p>{@code SyncRewriteIT}·{@code SyncOverwriteIT} 는 네 처리를 <b>하나씩</b> 건다.
 * 여기서는 <b>여러 처리가 한 호출에 섞일 때</b>와 <b>아무 일도 없을 때</b>를 본다 —
 * 실제로 그 조합에서 {@code ObjectDeletedException} 이 나 {@code 9000} 이 나가는 결함이
 * 있었고, 조합을 만드는 시험이 없어 여덟 단계 내내 드러나지 않았다.
 */
class SyncBoundaryIT extends AbstractSyncIT {

    private JsonNode sync(Fixture fixture, YearMonth yearMonth) throws Exception {
        return postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(yearMonth.getYear(), yearMonth.getMonthValue()));
    }

    private JsonNode sync(Fixture fixture, YearMonth yearMonth, boolean overwrite)
            throws Exception {
        return postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d,"overwriteModified":%s}
                """.formatted(yearMonth.getYear(), yearMonth.getMonthValue(), overwrite));
    }

    @Test
    @DisplayName("네 처리가 한 호출에 전부 섞여도 안전하다")
    void allFourOperationsInOneCall() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);

        // ④ 가 될 것: 기간을 줄여 밀어낼 설정(prepare 가 만든 "월세")
        // ② 가 될 것: 손대지 않을 설정
        long plain = createFixedExpense(fixture.token(), "통신비", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "통신"), 60000L, 10, wideStart(), wideEnd());
        // ③ 이 될 것: 직접 고칠 설정
        long touched = createFixedExpense(fixture.token(), "구독료", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "문화"), 15000L, 15, wideStart(), wideEnd());
        openMonth(fixture, target);
        markMonthlyModified(fixture.member(), touched,
                target.getYear(), target.getMonthValue());

        // ① 이 될 것: 그 달을 연 뒤에 등록해 행이 없는 설정
        long fresh = createFixedExpense(fixture.token(), "보험료", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "의료"), 40000L, 20, wideStart(), wideEnd());
        // 월세를 기간 밖으로 밀어낸다.
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"endYear":%d,"endMonth":%d}
                        """.formatted(futureMonth(2).getYear(),
                        futureMonth(2).getMonthValue())))).isEqualTo(200);

        JsonNode response = sync(fixture, target);

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("createdCount").asInt()).as("① 보험료").isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).as("② 통신비").isEqualTo(1);
        assertThat(data.get("keptCount").asInt()).as("③ 구독료").isEqualTo(1);
        assertThat(data.get("deletedCount").asInt()).as("④ 월세").isEqualTo(1);

        // list 는 재작성 후 상태다 — ④는 없으므로 ①+②+③ 셋이다.
        assertThat(data.get("list")).hasSize(3);
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(3);
        assertThat(plain).isPositive();
        assertThat(fresh).isPositive();
    }

    @Test
    @DisplayName("삭제와 보존이 함께 일어나도 안전하다 — 갱신이 0건인 조합")
    void deleteAndKeepWithoutAnyUpdate() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);
        long touched = createFixedExpense(fixture.token(), "구독료", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "문화"), 15000L, 15, wideStart(), wideEnd());
        openMonth(fixture, target);
        markMonthlyModified(fixture.member(), touched,
                target.getYear(), target.getMonthValue());
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"endYear":%d,"endMonth":%d}
                        """.formatted(futureMonth(2).getYear(),
                        futureMonth(2).getMonthValue())))).isEqualTo(200);

        JsonNode data = sync(fixture, target).get("data");

        assertThat(data.get("deletedCount").asInt()).isEqualTo(1);
        assertThat(data.get("keptCount").asInt()).isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).isZero();
        assertThat(data.get("list")).hasSize(1);
    }

    @Test
    @DisplayName("삭제와 overwrite 가 함께 일어나도 안전하다")
    void deleteAndOverwriteTogether() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(6);
        long touched = createFixedExpense(fixture.token(), "구독료", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "문화"), 15000L, 15, wideStart(), wideEnd());
        openMonth(fixture, target);
        markMonthlyModified(fixture.member(), touched,
                target.getYear(), target.getMonthValue());
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"endYear":%d,"endMonth":%d}
                        """.formatted(futureMonth(2).getYear(),
                        futureMonth(2).getMonthValue())))).isEqualTo(200);

        // overwrite 면 ③이 ②로 넘어가 삭제와 갱신이 함께 일어난다.
        JsonNode data = sync(fixture, target, true).get("data");

        assertThat(data.get("deletedCount").asInt()).isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).isEqualTo(1);
        assertThat(data.get("keptCount").asInt()).isZero();
        assertThat(data.get("list")).hasSize(1);
    }

    @Test
    @DisplayName("아무 대상도 없는 달을 재작성하면 네 건수가 전부 0 이다")
    void rewritingAnEmptyMonthIsANoOp() throws Exception {
        Fixture fixture = prepare();
        // 적용 기간 밖이라 만들 것도 지울 것도 없다.
        YearMonth outside = futureMonth(24);

        JsonNode data = sync(fixture, outside).get("data");

        assertThat(data.get("createdCount").asInt()).isZero();
        assertThat(data.get("updatedCount").asInt()).isZero();
        assertThat(data.get("keptCount").asInt()).isZero();
        assertThat(data.get("deletedCount").asInt()).isZero();
        assertThat(data.get("list")).isEmpty();
        assertThat(data.get("total").asLong()).isZero();
    }

    @Test
    @DisplayName("같은 달을 두 번 재작성하면 두 번째는 갱신만 한다 — 멱등하다")
    void rewritingTwiceIsStable() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(3);

        JsonNode first = sync(fixture, target).get("data");
        JsonNode second = sync(fixture, target).get("data");

        assertThat(first.get("createdCount").asInt()).isEqualTo(1);
        // 두 번째는 이미 있으므로 ①이 아니라 ②다. 행 수는 그대로다.
        assertThat(second.get("createdCount").asInt()).isZero();
        assertThat(second.get("updatedCount").asInt()).isEqualTo(1);
        assertThat(countMonthly(fixture.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(1);
    }

    @Test
    @DisplayName("재작성이 그 달 결제일을 말일 보정해 다시 쓴다")
    void rewriteReappliesTheEndOfMonthClamp() throws Exception {
        Fixture fixture = prepare();
        YearMonth target = futureMonth(3);
        openMonth(fixture, target);
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"paymentDayOfMonth":31}
                        """))).isEqualTo(200);

        assertThat(resCode(sync(fixture, target))).isEqualTo(200);

        // 31 일이 없는 달이면 말일로 접혀야 한다. 설정값을 그대로 넣으면 여기서 터진다.
        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                target.getYear(), target.getMonthValue());
        assertThat(row.get("payment_date")).isNotNull();
    }

    @Test
    @DisplayName("남의 달을 재작성해도 남의 행은 지워지지 않는다")
    void rewriteNeverTouchesAnotherMembersRows() throws Exception {
        Fixture other = prepare();
        YearMonth target = futureMonth(6);
        openMonth(other, target);
        // 남의 설정 기간을 줄여 그 달에서 기간 밖이 되게 만든다.
        assertThat(resCode(patchJson(FIXED_URL + "/" + other.fixedExpenseId(),
                other.token(), """
                        {"endYear":%d,"endMonth":%d}
                        """.formatted(futureMonth(2).getYear(),
                        futureMonth(2).getMonthValue())))).isEqualTo(200);
        Fixture fixture = prepare();

        // 내가 같은 달을 재작성한다. 삭제 쿼리에 소유자 조건이 없으므로
        // 대상 목록이 내 것으로 좁혀져 있지 않으면 남의 행이 함께 지워진다.
        assertThat(resCode(sync(fixture, target))).isEqualTo(200);

        assertThat(countMonthly(other.member(), target.getYear(),
                target.getMonthValue())).isEqualTo(1);
    }
}

package com.dbdomino.moneylog.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * {@code overwriteModified} — quickstart #35 (SC-406 · FR-414 ③).
 *
 * <p><b>③보존이 ②갱신으로 넘어가는 갈래다.</b> 기본은 보존인데, 되돌리기가 파괴적이기
 * 때문이다 — 사용자가 일부러 넣은 값이 사라지므로 명시적으로 요청할 때만 한다.
 *
 * <p>SC-406 은 "직접 수정분이 <b>100%</b> 관리 값으로 되돌아간다"를 요구한다. 금액만이
 * 아니라 <b>결제일·내용·수단까지</b> 되돌아가야 하고 {@code modified} 표시도 내려가야
 * 한다 — 표시가 남으면 다음 설정 수정의 자동 반영이 이 달을 영구히 건너뛴다.
 */
class SyncOverwriteIT extends AbstractSyncIT {

    /** 4.9 호출. {@code overwriteModified} 를 명시한다. */
    private JsonNode sync(Fixture fixture, YearMonth yearMonth, boolean overwrite)
            throws Exception {
        return postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d,"overwriteModified":%s}
                """.formatted(yearMonth.getYear(), yearMonth.getMonthValue(), overwrite));
    }

    /** 그 달을 열고 4.6 으로 직접 고쳐 {@code modified=true} 로 만든다. */
    private void editThisMonthDirectly(Fixture fixture, YearMonth yearMonth) throws Exception {
        openMonth(fixture, yearMonth);
        assertThat(resCode(patchJson(MONTHLY_URL + "/" + yearMonth.getYear() + "/"
                + yearMonth.getMonthValue() + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":123456,"content":"내가 고친 값"}
                        """))).isEqualTo(200);
        assertThat(modifiedOf(fixture, yearMonth)).isTrue();
    }

    @Test
    @DisplayName("#35 overwriteModified=true 면 수정분이 관리 값으로 되돌아간다")
    void overwriteRestoresTheSettingValue() throws Exception {
        Fixture fixture = prepare();
        editThisMonthDirectly(fixture, thisMonth());

        JsonNode data = sync(fixture, thisMonth(), true).get("data");

        assertThat(data.get("updatedCount").asInt()).isEqualTo(1);
        assertThat(data.get("keptCount").asInt()).isZero();
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#35 modified 표시도 함께 내려간다")
    void overwriteClearsTheModifiedFlag() throws Exception {
        Fixture fixture = prepare();
        editThisMonthDirectly(fixture, thisMonth());

        sync(fixture, thisMonth(), true);

        // 표시가 남으면 다음 설정 수정의 자동 반영이 이 달을 영구히 건너뛴다.
        assertThat(modifiedOf(fixture, thisMonth())).isFalse();
    }

    @Test
    @DisplayName("#35 SC-406 — 금액만이 아니라 내용까지 100% 되돌아간다")
    void everyFieldIsRestored() throws Exception {
        Fixture fixture = prepare();
        editThisMonthDirectly(fixture, thisMonth());

        sync(fixture, thisMonth(), true);

        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                thisMonth().getYear(), thisMonth().getMonthValue());
        assertThat(row.get("amount")).isEqualTo(500000L);
        assertThat(row.get("content")).isEqualTo("월세");
    }

    @Test
    @DisplayName("#35 기본(생략)은 보존이다 — keptCount 로 잡힌다")
    void defaultIsPreserve() throws Exception {
        Fixture fixture = prepare();
        editThisMonthDirectly(fixture, thisMonth());

        // overwriteModified 를 아예 보내지 않는다.
        JsonNode data = postJson(SYNC_URL, fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(thisMonth().getYear(), thisMonth().getMonthValue())).get("data");

        assertThat(data.get("keptCount").asInt()).isEqualTo(1);
        assertThat(data.get("updatedCount").asInt()).isZero();
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(123456L);
        assertThat(modifiedOf(fixture, thisMonth())).isTrue();
    }

    @Test
    @DisplayName("#35 명시적 false 도 보존이다")
    void explicitFalseIsAlsoPreserve() throws Exception {
        Fixture fixture = prepare();
        editThisMonthDirectly(fixture, thisMonth());

        JsonNode data = sync(fixture, thisMonth(), false).get("data");

        assertThat(data.get("keptCount").asInt()).isEqualTo(1);
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(123456L);
    }

    @Test
    @DisplayName("#35 overwrite 는 modified=false 인 행에는 영향이 없다")
    void overwriteDoesNotChangeUnmodifiedRowsBehaviour() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, thisMonth());
        // 손대지 않은 행이다.
        assertThat(modifiedOf(fixture, thisMonth())).isFalse();

        JsonNode data = sync(fixture, thisMonth(), true).get("data");

        // overwrite 여부와 무관하게 ②갱신이다.
        assertThat(data.get("updatedCount").asInt()).isEqualTo(1);
        assertThat(data.get("keptCount").asInt()).isZero();
        assertThat(modifiedOf(fixture, thisMonth())).isFalse();
    }

    @Test
    @DisplayName("#35 여러 행이 섞여 있으면 건수가 갈린다")
    void mixedRowsSplitIntoKeptAndUpdated() throws Exception {
        Fixture fixture = prepare();
        long second = createFixedExpense(fixture.token(), "통신비", fixture.paymentMethodId(),
                defaultGroupId(fixture.member(), "통신"), 60000L, 10, wideStart(), wideEnd());
        openMonth(fixture, thisMonth());
        // 첫 번째만 직접 고친다.
        markMonthlyModified(fixture.member(), fixture.fixedExpenseId(),
                thisMonth().getYear(), thisMonth().getMonthValue());

        JsonNode preserved = sync(fixture, thisMonth(), false).get("data");
        assertThat(preserved.get("keptCount").asInt()).isEqualTo(1);
        assertThat(preserved.get("updatedCount").asInt()).isEqualTo(1);

        JsonNode overwritten = sync(fixture, thisMonth(), true).get("data");
        assertThat(overwritten.get("keptCount").asInt()).isZero();
        assertThat(overwritten.get("updatedCount").asInt()).isEqualTo(2);
        assertThat(second).isPositive();
    }

    @Test
    @DisplayName("#35 되돌린 뒤에는 설정 수정의 자동 반영이 다시 이 달에 닿는다")
    void afterOverwriteThePropagationReachesTheMonthAgain() throws Exception {
        Fixture fixture = prepare();
        YearMonth future = futureMonth(1);
        openMonth(fixture, future);
        assertThat(resCode(patchJson(MONTHLY_URL + "/" + future.getYear() + "/"
                + future.getMonthValue() + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":123456}
                        """))).isEqualTo(200);

        sync(fixture, future, true);

        // modified 가 내려갔으므로 이제 자동 반영 대상이다.
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":700000}
                        """))).isEqualTo(200);
        assertThat(amountOf(fixture, future)).isEqualTo(700000L);
    }
}

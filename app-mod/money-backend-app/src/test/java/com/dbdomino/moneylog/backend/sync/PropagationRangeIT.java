package com.dbdomino.moneylog.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 4.4 설정 수정의 자동 반영 범위 — quickstart #30·#31·#32·#33 (SC-405 · FR-412).
 *
 * <p><b>US1 이 만든 {@code FixedExpenseSyncService.propagate} 의 네 갈래를 전부 건다.</b>
 * 그쪽 시험(4.4)은 설정 행 자체가 갱신되는지만 봤고, 범위 판정은 월별 내역이 있어야
 * 성립해 여기로 미뤄 두었다.
 *
 * <table border="1">
 *   <caption>설정을 고쳤을 때 어느 달이 따라가나</caption>
 *   <tr><th>대상</th><th>갱신</th><th>이유</th></tr>
 *   <tr><td>지난 달</td><td>안 한다</td><td>이미 일어난 일이다</td></tr>
 *   <tr><td><b>이번 달</b></td><td><b>안 한다</b></td><td>진행 중이고 사용자가 이미 본 숫자다</td></tr>
 *   <tr><td>미래 달 + {@code modified=false}</td><td>한다</td><td>—</td></tr>
 *   <tr><td>미래 달 + {@code modified=true}</td><td>안 한다</td><td>사용자가 직접 손댄 달이다</td></tr>
 * </table>
 *
 * <p><b>#33 이 판단이 필요했던 지점이다.</b> 이번 달을 포함하면 월세를 올렸을 때 이번 달
 * 가계부 금액이 소급해 바뀐다 — 004 의 중도상환 경계({@code > today})와 같은 성격이며
 * "사용자가 이미 본 숫자를 바꾸지 않는다"가 그 규칙이다.
 */
class PropagationRangeIT extends AbstractSyncIT {

    private static final long NEW_AMOUNT = 900000L;

    /** 설정의 금액을 바꾼다. 이 호출이 자동 반영을 일으킨다. */
    private void changeSettingAmount(Fixture fixture) throws Exception {
        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":%d}
                        """.formatted(NEW_AMOUNT)))).isEqualTo(200);
    }

    @Test
    @DisplayName("#30 미래 달이면서 modified=false 면 새 값을 따라간다")
    void futureUnmodifiedFollowsTheNewValue() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);

        changeSettingAmount(fixture);

        assertThat(amountOf(fixture, futureMonth(1))).isEqualTo(NEW_AMOUNT);
        assertThat(amountOf(fixture, futureMonth(2))).isEqualTo(NEW_AMOUNT);
    }

    @Test
    @DisplayName("#31 미래 달이라도 modified=true 면 바뀌지 않는다")
    void futureModifiedIsPreserved() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);
        YearMonth touched = futureMonth(1);
        markMonthlyModified(fixture.member(), fixture.fixedExpenseId(),
                touched.getYear(), touched.getMonthValue());

        changeSettingAmount(fixture);

        // 사용자가 일부러 넣은 값을 설정 변경이 덮으면 안 된다.
        assertThat(amountOf(fixture, touched)).isEqualTo(500000L);
        // 손대지 않은 다른 미래 달은 따라간다.
        assertThat(amountOf(fixture, futureMonth(2))).isEqualTo(NEW_AMOUNT);
    }

    @Test
    @DisplayName("#32 지난 달은 바뀌지 않는다")
    void lastMonthIsUntouched() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);

        changeSettingAmount(fixture);

        // 이미 일어난 일이다. 맞추고 싶으면 4.9 를 명시적으로 부른다.
        assertThat(amountOf(fixture, lastMonth())).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#33 이번 달도 바뀌지 않는다 — 판단이 필요했던 경계다")
    void thisMonthIsUntouched() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);

        changeSettingAmount(fixture);

        // 포함하면 월세를 올렸을 때 사용자가 이미 본 이번 달 금액이 소급해 바뀐다.
        // 경계를 >= 로 잡은 구현은 여기서만 걸린다.
        assertThat(amountOf(fixture, thisMonth())).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#30 결제일·내용·수단도 함께 따라간다 — 금액만이 아니다")
    void everyCopiedValueFollows() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);
        long another = createExpensePaymentMethod(fixture.token(), "신한카드");

        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"paymentDayOfMonth":5,"content":"인상된 월세","paymentMethodId":%d}
                        """.formatted(another)))).isEqualTo(200);

        YearMonth future = futureMonth(1);
        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                future.getYear(), future.getMonthValue());
        assertThat(row.get("content")).isEqualTo("인상된 월세");
        assertThat(((Number) row.get("payment_method_idx")).longValue()).isEqualTo(another);
    }

    @Test
    @DisplayName("#30 결제일이 바뀌면 말일 보정도 다시 적용된다")
    void paymentDateIsRecomputedWithClamping() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);

        assertThat(resCode(patchJson(FIXED_URL + "/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"paymentDayOfMonth":31}
                        """))).isEqualTo(200);

        // 31 일이 없는 달이면 말일로 접힌다. 반영이 설정값을 그대로 복사하면
        // 존재하지 않는 날짜를 넣으려다 터진다.
        YearMonth future = futureMonth(1);
        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                future.getYear(), future.getMonthValue());
        assertThat(row.get("payment_date")).isNotNull();
    }

    @Test
    @DisplayName("#31 반영된 행의 modified 는 여전히 false 다")
    void propagationDoesNotSetModified() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);

        changeSettingAmount(fixture);

        // 자동 반영이 이 표시를 세우면 다음 반영이 이 행을 건너뛰게 된다.
        assertThat(modifiedOf(fixture, futureMonth(1))).isFalse();
    }

    @Test
    @DisplayName("#30 아직 만들어지지 않은 미래 달은 반영 대상이 아니다")
    void unopenedFutureMonthIsNotCreatedByPropagation() throws Exception {
        Fixture fixture = prepare();
        openMonth(fixture, futureMonth(1));
        YearMonth unopened = futureMonth(5);

        changeSettingAmount(fixture);

        // 자동 반영은 있는 행을 갱신할 뿐 새로 만들지 않는다. 그 달을 열면 그때
        // 새 값으로 생긴다.
        assertThat(countMonthly(fixture.member(), unopened.getYear(),
                unopened.getMonthValue())).isZero();
    }

    @Test
    @DisplayName("SC-405 설정을 고쳐도 지난 달·직접 수정한 달의 값이 변하지 않는다")
    void sc405InOneTest() throws Exception {
        Fixture fixture = prepare();
        openPastPresentFuture(fixture);
        YearMonth touched = futureMonth(2);
        markMonthlyModified(fixture.member(), fixture.fixedExpenseId(),
                touched.getYear(), touched.getMonthValue());

        changeSettingAmount(fixture);

        assertThat(amountOf(fixture, lastMonth())).isEqualTo(500000L);
        assertThat(amountOf(fixture, touched)).isEqualTo(500000L);
    }
}

package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 목표금액 범위 — quickstart #9 (SC-504 · FR-504).
 *
 * <p>범위는 {@code 0 ~ 100,000,000} 이고 <b>양 끝을 포함한다</b>. 벗어나면 {@code 3602} 다.
 *
 * <p><b>경계를 양쪽에서 건다.</b> 정확히 1억이 성공하고 1억+1 이 실패해야 한다 — 한쪽만
 * 보면 {@code <} 와 {@code <=} 를 잘못 쓴 구현이 통과한다.
 *
 * <p><b>애플리케이션이 먼저 막는 것이 핵심이다.</b> DB 에도 같은 범위가 CHECK 로 걸려
 * 있지만({@code ck_target_default_amount}·{@code ck_target_monthly_amount}) 거기까지 가면
 * {@code 3602} 가 아니라 <b>{@code 9000}</b> 이 나간다. 그래서 코드가 {@code 3602} 인지를
 * 본다 — "실패했다"만 보면 두 경로가 구분되지 않는다.
 */
class TargetRangeIT extends AbstractTargetIT {

    private static final long MAX = 100_000_000L;

    @Test
    @DisplayName("#9 1억 초과는 3602 다 — 기본")
    void rejectsOverMaxDefault() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(putDefault(fixture, fixture.foodGroupId(), MAX + 1))).isEqualTo(3602);
        assertThat(countDefaultTargets(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#9 1억 초과는 3602 다 — 월별")
    void rejectsOverMaxMonthly() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(putMonthly(fixture, fixture.foodGroupId(), MAX + 1))).isEqualTo(3602);
        assertThat(countMonthlyTargets(fixture.member())).isZero();
    }

    @Test
    @DisplayName("경계 — 정확히 1억은 성공이다")
    void acceptsExactlyMax() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(putDefault(fixture, fixture.foodGroupId(), MAX))).isEqualTo(200);
        assertThat(resCode(putMonthly(fixture, fixture.foodGroupId(), MAX))).isEqualTo(200);

        assertThat(getTarget(fixture, fixture.foodGroupId())
                .get("data").get("defaultTargetAmount").asLong()).isEqualTo(MAX);
    }

    @Test
    @DisplayName("음수는 3602 다")
    void rejectsNegative() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(putDefault(fixture, fixture.foodGroupId(), -1L))).isEqualTo(3602);
        assertThat(resCode(putMonthly(fixture, fixture.foodGroupId(), -1L))).isEqualTo(3602);
        assertThat(countDefaultTargets(fixture.member())).isZero();
        assertThat(countMonthlyTargets(fixture.member())).isZero();
    }

    /**
     * 금액을 빠뜨린 몸통도 {@code 3602} 다.
     *
     * <p>몸통 필드가 하나뿐이고 그 하나가 필수라 "빠뜨렸다"와 "값이 틀렸다"를 가르는 것이
     * 사용자에게 주는 정보가 없다. 005 의 금액 규칙과 같은 처방이다.
     */
    @Test
    @DisplayName("금액을 빠뜨리면 3602 다")
    void rejectsMissingAmount() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(patchJson(URL + "/default/" + fixture.foodGroupId(),
                fixture.token(), "{}"))).isEqualTo(3602);
        assertThat(resCode(patchJson(
                URL + "/monthly/" + YEAR + "/" + MONTH + "/" + fixture.foodGroupId(),
                fixture.token(), "{}"))).isEqualTo(3602);
    }

    /**
     * <b>금액 판정이 가장 나중이다.</b>
     *
     * <p>남의 유형에 1억 초과를 보내면 {@code 3602} 가 아니라 {@code 3103} 이어야 한다 —
     * 순서가 뒤집히면 그 ID 가 실재하는지가 코드 차이로 새어 나간다.
     */
    @Test
    @DisplayName("남의 유형에 범위 밖 금액을 보내면 3602 가 아니라 3103 이다")
    void ownershipBeatsRange() throws Exception {
        Fixture mine = prepare();
        Fixture other = prepare();

        assertThat(resCode(putDefault(mine, other.foodGroupId(), MAX + 1))).isEqualTo(3103);
    }
}

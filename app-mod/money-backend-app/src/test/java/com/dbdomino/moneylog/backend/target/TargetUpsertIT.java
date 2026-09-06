package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 5.3·5.4 upsert — quickstart #1·#2·#10.
 *
 * <p><b>"행이 늘지 않는다"를 DB 에서 직접 센다.</b> 응답만 보면 두 번째 저장이 갱신인지
 * 새 행인지 구분되지 않는다 — 둘 다 같은 금액을 돌려주기 때문이다. 새 행이 생기면 유니크
 * 제약이 막아 {@code 9000} 이 나겠지만, 제약이 없거나 조건이 어긋난 상태에서는 조용히
 * 쌓여 목록이 같은 유형을 두 번 보여준다.
 */
class TargetUpsertIT extends AbstractTargetIT {

    @Test
    @DisplayName("#1 목표가 없던 유형에 기본 목표를 저장하면 새로 만들어진다")
    void createsDefaultWhenAbsent() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = putDefault(fixture, fixture.foodGroupId(), 400_000L);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("defaultTargetAmount").asLong()).isEqualTo(400_000L);
        assertThat(response.get("data").get("expendGroupId").asLong())
                .isEqualTo(fixture.foodGroupId());
        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("#2 기본 목표를 다시 저장하면 같은 행이 갱신되고 행이 늘지 않는다")
    void updatesDefaultInPlace() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 400_000L);

        JsonNode response = putDefault(fixture, fixture.foodGroupId(), 450_000L);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("defaultTargetAmount").asLong()).isEqualTo(450_000L);
        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
        assertThat(getTarget(fixture, fixture.foodGroupId())
                .get("data").get("defaultTargetAmount").asLong()).isEqualTo(450_000L);
    }

    @Test
    @DisplayName("#1 월별 목표도 없던 연·월에 저장하면 새로 만들어진다")
    void createsMonthlyWhenAbsent() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = putMonthly(fixture, fixture.foodGroupId(), 500_000L);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("monthlyTargetAmount").asLong()).isEqualTo(500_000L);
        assertThat(response.get("data").get("year").asInt()).isEqualTo(YEAR);
        assertThat(response.get("data").get("month").asInt()).isEqualTo(MONTH);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
    }

    @Test
    @DisplayName("#2 월별 목표를 다시 저장하면 같은 행이 갱신되고 행이 늘지 않는다")
    void updatesMonthlyInPlace() throws Exception {
        Fixture fixture = prepare();
        putMonthly(fixture, fixture.foodGroupId(), 500_000L);

        JsonNode response = putMonthly(fixture, fixture.foodGroupId(), 600_000L);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("monthlyTargetAmount").asLong()).isEqualTo(600_000L);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
    }

    /**
     * 같은 연·월이라도 유형이 다르면 <b>다른 행</b>이다.
     *
     * <p>유니크 제약이 {@code (id_key, year, month, expend_group_idx)} 인데 upsert 의 충돌
     * 대상을 좁게 잡으면(예: 유형을 빼고) 두 번째 유형의 저장이 첫 번째를 덮어쓴다.
     */
    @Test
    @DisplayName("같은 달의 다른 유형은 각각 저장된다")
    void keepsMonthlyRowsPerGroup() throws Exception {
        Fixture fixture = prepare();

        putMonthly(fixture, fixture.foodGroupId(), 500_000L);
        putMonthly(fixture, fixture.transportGroupId(), 100_000L);

        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(2);
        assertThat(getTarget(fixture, fixture.foodGroupId())
                .get("data").get("monthlyTargetAmount").asLong()).isEqualTo(500_000L);
        assertThat(getTarget(fixture, fixture.transportGroupId())
                .get("data").get("monthlyTargetAmount").asLong()).isEqualTo(100_000L);
    }

    @Test
    @DisplayName("#10 0원 저장은 성공이다 — 유효한 값이다")
    void acceptsZero() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(putDefault(fixture, fixture.foodGroupId(), 0L))).isEqualTo(200);
        assertThat(resCode(putMonthly(fixture, fixture.foodGroupId(), 0L))).isEqualTo(200);

        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
    }
}

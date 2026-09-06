package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 두 층의 독립 — quickstart #3·#4 (SC-502 · FR-505).
 *
 * <p><b>요점은 "복사하지 않는다"이다.</b> 기본을 정할 때 월별로 값을 복사해 두면 그
 * 순간에는 시험이 통과한다 — 조회하면 두 값이 다 보이기 때문이다. 깨지는 것은 <b>그
 * 다음</b>이다: 기본을 고쳐도 복사본은 그대로라 화면이 "이 달은 따로 정했다"고 표시하고,
 * 사용자는 정한 적 없는 월별 값을 지울 방법이 없다.
 *
 * <p>그래서 이 시험은 <b>한쪽을 저장한 뒤 반대쪽이 어떤 상태인지</b>를 본다. 특히
 * "기본만 저장한 뒤 월별이 여전히 {@code null}"이 복사 여부를 직접 가른다.
 */
class TargetTwoLayerIT extends AbstractTargetIT {

    @Test
    @DisplayName("#3 기본을 바꿔도 저장된 월별 값은 그대로다")
    void changingDefaultKeepsMonthly() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 400_000L);
        putMonthly(fixture, fixture.foodGroupId(), 600_000L);

        putDefault(fixture, fixture.foodGroupId(), 100_000L);

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");
        assertThat(data.get("defaultTargetAmount").asLong()).isEqualTo(100_000L);
        assertThat(data.get("monthlyTargetAmount").asLong()).isEqualTo(600_000L);
    }

    @Test
    @DisplayName("#4 월별을 저장해도 기본은 바뀌지 않는다")
    void savingMonthlyKeepsDefault() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 400_000L);

        putMonthly(fixture, fixture.foodGroupId(), 600_000L);

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");
        assertThat(data.get("defaultTargetAmount").asLong()).isEqualTo(400_000L);
        assertThat(data.get("monthlyTargetAmount").asLong()).isEqualTo(600_000L);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
    }

    /**
     * 복사 여부를 직접 가르는 시험이다.
     *
     * <p>기본만 저장했는데 월별 행이 생겼다면 복사한 것이다.
     */
    @Test
    @DisplayName("FR-505 기본을 정해도 월별로 복사되지 않는다")
    void neverCopiesDefaultIntoMonthly() throws Exception {
        Fixture fixture = prepare();

        putDefault(fixture, fixture.foodGroupId(), 400_000L);

        assertThat(countMonthlyTargets(fixture.member())).isZero();
        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");
        assertThat(data.get("defaultTargetAmount").asLong()).isEqualTo(400_000L);
        assertThat(data.get("monthlyTargetAmount").isNull()).isTrue();
    }

    /** 반대 방향도 같다 — 월별만 저장했다고 기본 행이 생기지 않는다. */
    @Test
    @DisplayName("FR-505 월별을 정해도 기본 행이 생기지 않는다")
    void neverCreatesDefaultFromMonthly() throws Exception {
        Fixture fixture = prepare();

        putMonthly(fixture, fixture.foodGroupId(), 600_000L);

        assertThat(countDefaultTargets(fixture.member())).isZero();
        assertThat(getTarget(fixture, fixture.foodGroupId())
                .get("data").get("defaultTargetAmount").asLong()).isZero();
    }
}

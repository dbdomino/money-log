package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * {@code null} 과 {@code 0} 의 비대칭 — quickstart #5·#6·#7·#8 (SC-503 · FR-506·507).
 *
 * <h2>네 개가 한 묶음이다</h2>
 *
 * <table border="1">
 *   <caption>행이 없을 때 두 필드의 값</caption>
 *   <tr><th>필드</th><th>값</th><th>뜻</th></tr>
 *   <tr><td>{@code monthlyTargetAmount}</td><td>{@code null}</td><td>기본값을 쓰겠다</td></tr>
 *   <tr><td>{@code defaultTargetAmount}</td><td>{@code 0}</td><td>한도를 정한 적 없다</td></tr>
 * </table>
 *
 * <p>월별에만 두 상태가 필요한 것은 {@code 0} 이 <b>"그 달엔 쓰지 않겠다"</b>라는 다른
 * 결과를 내기 때문이다. 기본에는 그 구분이 필요 없다 — 미설정과 0 둘 다 한도 없음이다.
 *
 * <p><b>#8 이 가장 놓치기 쉽다.</b> {@code monthlyTargetAmount} 가 {@code null} 일 때
 * 필드가 <b>생략되지 않고 {@code null} 로 와야</b> 한다. Jackson 이 {@code null} 필드를
 * 빼면 프론트가 {@code 'monthlyTargetAmount' in obj} 로 "없음"과 "필드 자체가 없음"을
 * 구분할 수 없다. 그래서 {@code isNull()} 만 보지 않고 {@code has(...)} 를 함께 본다 —
 * 없는 필드도 {@code get(...)} 이 {@code null} 을 돌려주므로 {@code isNull()} 만으로는
 * 두 경우가 같아 보인다.
 */
class TargetNullVsZeroIT extends AbstractTargetIT {

    @Test
    @DisplayName("#5 월별을 저장한 적 없으면 monthlyTargetAmount 는 null 이다")
    void monthlyIsNullWhenAbsent() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 400_000L);

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");

        assertThat(data.get("monthlyTargetAmount").isNull()).isTrue();
        assertThat(data.get("defaultTargetAmount").asLong()).isEqualTo(400_000L);
    }

    @Test
    @DisplayName("#6 월별을 0원으로 저장했으면 monthlyTargetAmount 는 0 이다 (null 이 아니다)")
    void monthlyIsZeroWhenSavedZero() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 400_000L);
        putMonthly(fixture, fixture.foodGroupId(), 0L);

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");

        assertThat(data.get("monthlyTargetAmount").isNull()).isFalse();
        assertThat(data.get("monthlyTargetAmount").asLong()).isZero();
    }

    @Test
    @DisplayName("#7 기본을 저장한 적 없으면 defaultTargetAmount 는 0 이다 (null 이 아니다)")
    void defaultIsZeroWhenAbsent() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");

        assertThat(data.get("defaultTargetAmount").isNull()).isFalse();
        assertThat(data.get("defaultTargetAmount").asLong()).isZero();
    }

    @Test
    @DisplayName("#8 monthlyTargetAmount 필드가 생략되지 않고 null 로 온다 — 단건")
    void keepsNullFieldInDetail() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = getTarget(fixture, fixture.foodGroupId()).get("data");

        assertThat(data.has("monthlyTargetAmount")).isTrue();
        assertThat(data.get("monthlyTargetAmount").isNull()).isTrue();
    }

    @Test
    @DisplayName("#8 monthlyTargetAmount 필드가 생략되지 않고 null 로 온다 — 목록")
    void keepsNullFieldInList() throws Exception {
        Fixture fixture = prepare();

        JsonNode row = rowOf(listTargets(fixture, 0, 10), fixture.foodGroupId());

        assertThat(row).isNotNull();
        assertThat(row.has("monthlyTargetAmount")).isTrue();
        assertThat(row.get("monthlyTargetAmount").isNull()).isTrue();
        assertThat(row.get("defaultTargetAmount").asLong()).isZero();
    }

    /**
     * 두 상태가 <b>같은 응답 안에서</b> 갈리는지 본다.
     *
     * <p>한 유형은 0원으로 정하고 다른 유형은 정하지 않았을 때 목록이 {@code 0} 과
     * {@code null} 을 각각 내야 한다. 어느 한쪽으로 접는 구현은 두 줄을 같게 만든다.
     */
    @Test
    @DisplayName("FR-506 같은 목록에서 0원 저장과 미저장이 갈린다")
    void distinguishesZeroFromAbsentInOneList() throws Exception {
        Fixture fixture = prepare();
        putMonthly(fixture, fixture.foodGroupId(), 0L);

        JsonNode response = listTargets(fixture, 0, 10);

        assertThat(rowOf(response, fixture.foodGroupId()).get("monthlyTargetAmount").asLong())
                .isZero();
        assertThat(rowOf(response, fixture.transportGroupId()).get("monthlyTargetAmount").isNull())
                .isTrue();
    }
}

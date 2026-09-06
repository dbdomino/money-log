package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * upsert 의 동시 요청 — target-amount.md §4.
 *
 * <p><b>"조회해서 없으면 INSERT" 로는 통과할 수 없다.</b> 두 요청이 같은 순간 "없음"을
 * 보는 창이 열리고, 하나가 {@code ux_target_default}·{@code ux_target_monthly} 위반으로
 * 실패해 <b>{@code 9000}</b> 을 낸다. 사용자에게는 둘 다 성공으로 보여야 하므로 충돌을
 * <b>정상 흐름으로</b> 흡수해야 한다 — {@code INSERT ... ON CONFLICT DO UPDATE} 다.
 *
 * <p>005 의 lazy 생성({@code DO NOTHING})과 다른 점은 <b>뒤에 온 값이 이겨야 한다</b>는
 * 것이다. 목표금액은 사용자가 방금 입력한 값이라 "이미 있으니 건너뛴다"로 끝내면 저장한
 * 값이 반영되지 않은 채 200 이 나간다.
 *
 * <p><b>{@link CyclicBarrier} 없이 순차로 부르면 경합 자체가 일어나지 않는다</b> —
 * 두 번째가 이미 커밋된 행을 보게 되어 시험이 통과해도 아무것도 검증하지 못한다.
 */
class TargetConcurrencyIT extends AbstractTargetIT {

    private static final int TIMEOUT_SECONDS = 30;

    /** 두 호출을 같은 지점에서 푼다. 두 응답을 그대로 돌려준다. */
    private List<JsonNode> together(Callable<JsonNode> left, Callable<JsonNode> right)
            throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<JsonNode> a = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return left.call();
            });
            Future<JsonNode> b = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return right.call();
            });
            return List.of(a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("기본 목표를 동시에 처음 저장해도 행은 1건이고 둘 다 200 이다")
    void concurrentFirstDefaultSaves() throws Exception {
        Fixture fixture = prepare();
        assertThat(countDefaultTargets(fixture.member())).isZero();

        List<JsonNode> responses = together(
                () -> putDefault(fixture, fixture.foodGroupId(), 400_000L),
                () -> putDefault(fixture, fixture.foodGroupId(), 450_000L));

        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
        for (JsonNode response : responses) {
            // 한쪽이 유니크 위반으로 9000 을 내면 여기서 걸린다. 그 상황에서도 행은
            // 1건이라 위 단언만으로는 통과해 버린다 — 그래서 이 단언이 따로 필요하다.
            assertThat(resCode(response)).as("동시 저장 응답: %s", response).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("월별 목표를 동시에 처음 저장해도 행은 1건이고 둘 다 200 이다")
    void concurrentFirstMonthlySaves() throws Exception {
        Fixture fixture = prepare();
        assertThat(countMonthlyTargets(fixture.member())).isZero();

        List<JsonNode> responses = together(
                () -> putMonthly(fixture, fixture.foodGroupId(), 500_000L),
                () -> putMonthly(fixture, fixture.foodGroupId(), 600_000L));

        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
        for (JsonNode response : responses) {
            assertThat(resCode(response)).as("동시 저장 응답: %s", response).isEqualTo(200);
        }
    }

    /**
     * 충돌을 흡수하되 <b>덮어쓴다</b>.
     *
     * <p>{@code DO NOTHING} 으로 흡수하면 이 시험이 걸린다 — 이미 값이 있는 상태에서
     * 다시 저장한 요청이 200 을 받는데 값은 옛것 그대로다.
     */
    @Test
    @DisplayName("이미 있는 목표에 동시 저장을 걸면 둘 중 하나의 값이 남는다")
    void lastWriteWins() throws Exception {
        Fixture fixture = prepare();
        putDefault(fixture, fixture.foodGroupId(), 100_000L);

        together(() -> putDefault(fixture, fixture.foodGroupId(), 400_000L),
                () -> putDefault(fixture, fixture.foodGroupId(), 450_000L));

        long saved = getTarget(fixture, fixture.foodGroupId())
                .get("data").get("defaultTargetAmount").asLong();
        assertThat(saved).isIn(400_000L, 450_000L);
        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
    }

    /**
     * 두 층을 동시에 건드려도 서로를 밀어내지 않는다.
     *
     * <p>독립이므로 각각 1건씩 남아야 한다(FR-505).
     */
    @Test
    @DisplayName("기본과 월별을 동시에 저장하면 각각 1건씩 남는다")
    void concurrentSavesAcrossLayers() throws Exception {
        Fixture fixture = prepare();

        List<JsonNode> responses = together(
                () -> putDefault(fixture, fixture.foodGroupId(), 400_000L),
                () -> putMonthly(fixture, fixture.foodGroupId(), 600_000L));

        for (JsonNode response : responses) {
            assertThat(resCode(response)).isEqualTo(200);
        }
        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);
    }
}

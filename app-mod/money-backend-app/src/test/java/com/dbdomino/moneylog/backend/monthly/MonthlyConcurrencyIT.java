package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 동시 생성 경합 — quickstart #15 (SC-403 · FR-407).
 *
 * <p><b>이 기능에서 가장 중요한 시험이다.</b> lazy 생성 모델의 대가가 여기서 드러난다 —
 * GET 이 쓰기를 하므로 같은 달을 두 화면이 동시에 처음 여는 상황이 <b>정상 경로</b>다.
 *
 * <h2>SC-403 의 뒷부분까지 확인한다</h2>
 *
 * <p>"행은 1건"만 보면 부족하다. 유니크 제약이 두 번째 삽입을 막긴 하지만 그것이
 * <b>예외로 끝나면 한쪽 사용자 화면이 실패한다.</b> SC-403 은 "어느 쪽도 오류로 끝나지
 * 않는다"까지 요구하므로 <b>두 응답이 모두 200</b>인지 함께 단언한다.
 *
 * <p>애플리케이션의 "있으면 건너뛴다" 검사만으로는 통과할 수 없다 — 두 트랜잭션이 같은
 * 순간 "없음"을 보는 창이 열리기 때문이다. {@code INSERT ... ON CONFLICT DO NOTHING} 이
 * 그 충돌을 예외 없이 흡수해야 한다.
 *
 * <h2>두 스레드를 같은 지점에서 푼다</h2>
 *
 * <p>{@link CyclicBarrier} 없이 순차로 부르면 두 번째가 이미 커밋된 행을 보게 되어
 * <b>경합 자체가 일어나지 않는다</b> — 시험이 통과해도 아무것도 검증하지 못한다.
 * {@code data-mod} 의 {@code FixedExpenseMonthlyConcurrencyIT} 와 같은 처방이다.
 */
class MonthlyConcurrencyIT extends AbstractMonthlyIT {

    private static final int TIMEOUT_SECONDS = 30;

    /** 두 스레드가 같은 순간 같은 달을 연다. 두 응답을 그대로 돌려준다. */
    private List<JsonNode> openSameMonthConcurrently(Fixture fixture, int year, int month)
            throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<JsonNode> left = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return listMonthly(fixture, year, month);
            });
            Future<JsonNode> right = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return listMonthly(fixture, year, month);
            });
            return List.of(left.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    right.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("#15 같은 달을 동시에 두 요청이 처음 열어도 행은 1건이다")
    void concurrentFirstOpensLeaveExactlyOneRow() throws Exception {
        Fixture fixture = prepare();
        assertThat(countMonthly(fixture.member(), 2026, 11)).isZero();

        openSameMonthConcurrently(fixture, 2026, 11);

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#15 어느 쪽도 오류로 끝나지 않는다 — SC-403 의 뒷부분")
    void neitherRequestFails() throws Exception {
        Fixture fixture = prepare();

        List<JsonNode> responses = openSameMonthConcurrently(fixture, 2026, 11);

        // 한쪽이 유니크 위반으로 9000 을 내면 여기서 걸린다. 그 상황에서도 DB 의 행은
        // 1건이라 위 시험만으로는 통과해 버린다 — 그래서 이 단언이 따로 필요하다.
        for (JsonNode response : responses) {
            assertThat(resCode(response)).as("동시 조회 응답: %s", response).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("#15 두 응답이 같은 내용을 본다")
    void bothRequestsSeeTheSameRow() throws Exception {
        Fixture fixture = prepare();

        List<JsonNode> responses = openSameMonthConcurrently(fixture, 2026, 11);

        for (JsonNode response : responses) {
            JsonNode list = response.get("data").get("list");
            // 삽입 직후 다시 조회하므로 삽입에 진 쪽도 이긴 쪽의 행을 본다.
            // 빈 목록이 돌아오면 "충돌했으니 아무것도 없다"로 끝낸 것이다.
            assertThat(list).hasSize(1);
            assertThat(list.get(0).get("amount").asLong()).isEqualTo(500000L);
        }
    }

    @Test
    @DisplayName("#15 고정지출이 여러 건이어도 각각 1행씩만 남는다")
    void concurrentOpensWithMultipleSettings() throws Exception {
        Fixture fixture = prepare();
        addFixedExpense(fixture, "통신비", fixture.paymentMethodId(), 60000L);
        addFixedExpense(fixture, "구독료", fixture.paymentMethodId(), 15000L);

        List<JsonNode> responses = openSameMonthConcurrently(fixture, 2026, 11);

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(3);
        for (JsonNode response : responses) {
            assertThat(resCode(response)).isEqualTo(200);
            assertThat(response.get("data").get("list")).hasSize(3);
        }
    }

    @Test
    @DisplayName("이미 만들어진 달을 동시에 열어도 그대로다")
    void concurrentReadsOnAnExistingMonthAreSafe() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        List<JsonNode> responses = openSameMonthConcurrently(fixture, 2026, 11);

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
        for (JsonNode response : responses) {
            assertThat(resCode(response)).isEqualTo(200);
        }
    }
}

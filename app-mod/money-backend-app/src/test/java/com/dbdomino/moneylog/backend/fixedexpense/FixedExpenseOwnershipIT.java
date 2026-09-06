package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 소유자 판정 — quickstart #10 (FR-401).
 *
 * <p><b>"없음"과 "타인 소유"를 같은 {@code 3402} 로 낸다.</b> 나누면 ID 를 훑는 것만으로
 * 남의 고정지출이 존재한다는 사실이 새어 나간다 — 003·004 의 {@code 3003}·{@code 3103}·
 * {@code 3202}·{@code 3302} 와 같은 규칙이다.
 *
 * <p><b>조회 조건에 소유자를 함께 건다.</b> 먼저 꺼내 놓고 소유자를 비교하는 방식은
 * 비교를 빠뜨린 자리가 곧 구멍이 된다.
 */
class FixedExpenseOwnershipIT extends AbstractFixedExpenseIT {

    @Test
    @DisplayName("#10 남의 설정을 조회하면 3402 다")
    void gettingOthersSettingIs3402() throws Exception {
        Fixture owner = prepare();
        long id = createDefaultFixedExpense(owner);
        Fixture stranger = prepare();

        assertThat(resCode(getJson(URL + "/" + id, stranger.token()))).isEqualTo(3402);
    }

    @Test
    @DisplayName("#10 없는 ID 도 같은 3402 다 — 존재 여부가 새지 않는다")
    void missingIdIsTheSameCode() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL + "/999999999", fixture.token()))).isEqualTo(3402);
    }

    @Test
    @DisplayName("#10 남의 설정을 수정하면 3402 이고 값이 바뀌지 않는다")
    void updatingOthersSettingIs3402() throws Exception {
        Fixture owner = prepare();
        long id = createDefaultFixedExpense(owner);
        Fixture stranger = prepare();

        assertThat(resCode(patchJson(URL + "/" + id, stranger.token(), """
                {"amount":999999}
                """))).isEqualTo(3402);

        // 거절만으로는 부족하다 — 실제로 안 바뀌었는지 본인 토큰으로 확인한다.
        assertThat(getJson(URL + "/" + id, owner.token())
                .get("data").get("amount").asLong()).isEqualTo(500000L);
    }

    @Test
    @DisplayName("#10 남의 설정을 삭제하면 3402 이고 행이 남는다")
    void deletingOthersSettingIs3402() throws Exception {
        Fixture owner = prepare();
        long id = createDefaultFixedExpense(owner);
        Fixture stranger = prepare();

        assertThat(resCode(deleteJson(URL + "/" + id, stranger.token()))).isEqualTo(3402);

        assertThat(resCode(getJson(URL + "/" + id, owner.token()))).isEqualTo(200);
    }

    @Test
    @DisplayName("토큰 없이 부르면 1001 이다 — 소유자 판정보다 먼저다")
    void withoutTokenIs1001() throws Exception {
        Fixture owner = prepare();
        long id = createDefaultFixedExpense(owner);

        assertThat(resCode(getJson(URL + "/" + id, null))).isEqualTo(1001);
        assertThat(resCode(deleteJson(URL + "/" + id, null))).isEqualTo(1001);
    }
}

package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 4.7 고정지출 삭제 — quickstart #12 (SC-407 · FR-416).
 *
 * <p><b>물리 삭제다.</b> 수단·지출유형은 삭제 표시(행 보존)인데 고정지출은 다르다 —
 * "이 항목 자체를 없앤다"는 뜻이므로 그 고정지출의 월별 내역이 <b>지난 달 것까지 전부</b>
 * 함께 사라진다({@code ON DELETE CASCADE}).
 *
 * <p><b>막는 조건이 없다.</b> 003 의 지출유형 삭제는 그 유형을 쓴 지출이 있으면
 * {@code 3106} 으로 막았지만, 고정지출을 참조하는 것은 자기 월별 내역뿐이라 함께 지운다.
 *
 * <p>이 시험은 <b>삭제 전에 월별 내역을 몇 달치 만들어 둔다.</b> 빈 상태에서 지우면
 * CASCADE 가 걸렸는지 아닌지가 드러나지 않는다.
 */
class FixedExpenseDeleteIT extends AbstractFixedExpenseIT {

    /**
     * 그 달의 월별 내역을 만든다.
     *
     * <p><b>4.5(lazy 생성)가 US2 에 있어 여기서는 JDBC 로 넣는다.</b> US1 이 독립적으로
     * 검증되어야 하는데(Independent Test) SC-407 은 지울 자식 행을 요구하기 때문이다.
     * US2 가 서면 이 자리는 실제 경로인 "그 달을 연다"로 바뀌어도 된다.
     */
    private void makeMonthly(Fixture fixture, long fixedExpenseId, String yearMonth) {
        insertMonthlyRow(fixture.member(), fixedExpenseId,
                yearOf(yearMonth), monthOf(yearMonth), 500000L);
    }

    @Test
    @DisplayName("#12 삭제하면 관리 행과 월별 내역이 전부 사라진다 — 지난 달 포함")
    void deleteCascadesToEveryMonthlyRow() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);
        // 적용 기간 2026-11 ~ 2027-02 중 세 달을 열어 둔다.
        makeMonthly(fixture, id, "2026-11");
        makeMonthly(fixture, id, "2026-12");
        makeMonthly(fixture, id, "2027-01");
        assertThat(countMonthlyAll(fixture.member())).isEqualTo(3);

        assertThat(resCode(deleteJson(URL + "/" + id, fixture.token()))).isEqualTo(200);

        // SC-407. 여기서 3이 나오면 CASCADE 가 걸리지 않은 것이다.
        assertThat(countMonthlyAll(fixture.member())).isZero();
        assertThat(resCode(getJson(URL + "/" + id, fixture.token()))).isEqualTo(3402);
    }

    @Test
    @DisplayName("#12 삭제 표시가 아니라 물리 삭제다 — 목록에서도 사라진다")
    void deleteIsPhysicalNotAFlag() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(deleteJson(URL + "/" + id, fixture.token()))).isEqualTo(200);

        // 수단·지출유형은 삭제 표시라 관리 목록에 deleted=true 로 남는다. 고정지출은 다르다.
        assertThat(getJson(URL + "?offset=0&limit=10", fixture.token())
                .get("data").get("totalCount").asLong()).isZero();
    }

    @Test
    @DisplayName("#12 사용 이력이 있어도 막지 않는다 — 003 의 지출유형 삭제와 다르다")
    void deleteIsNotBlockedByUsage() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);
        makeMonthly(fixture, id, "2026-11");

        // 003 이라면 여기서 3106 이 났을 상황이다. 고정지출에는 그런 조건이 없다.
        assertThat(resCode(deleteJson(URL + "/" + id, fixture.token()))).isEqualTo(200);
    }

    @Test
    @DisplayName("#12 다른 고정지출의 월별 내역은 남는다 — 지운 것만 사라진다")
    void deleteOnlyRemovesItsOwnRows() throws Exception {
        Fixture fixture = prepare();
        long doomed = createDefaultFixedExpense(fixture);
        long survivor = createFixedExpense(fixture.token(), "통신비", fixture.paymentMethodId(),
                fixture.expendGroupId(), 60000L, 10, START, END);
        makeMonthly(fixture, doomed, "2026-11");
        makeMonthly(fixture, survivor, "2026-11");
        assertThat(countMonthlyAll(fixture.member())).isEqualTo(2);

        assertThat(resCode(deleteJson(URL + "/" + doomed, fixture.token()))).isEqualTo(200);

        assertThat(countMonthlyAll(fixture.member())).isEqualTo(1);
        assertThat(resCode(getJson(URL + "/" + survivor, fixture.token()))).isEqualTo(200);
    }

    @Test
    @DisplayName("같은 설정을 두 번 지우면 두 번째는 3402 다")
    void deletingTwiceIs3402() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(deleteJson(URL + "/" + id, fixture.token()))).isEqualTo(200);
        // 행이 사라졌으므로 "없음"이고, 없음은 3402 다. 003 처럼 "이미 삭제됨" 코드를
        // 따로 두지 않는다 — 삭제 표시가 아니라 물리 삭제라 그런 상태가 존재하지 않는다.
        assertThat(resCode(deleteJson(URL + "/" + id, fixture.token()))).isEqualTo(3402);
    }
}

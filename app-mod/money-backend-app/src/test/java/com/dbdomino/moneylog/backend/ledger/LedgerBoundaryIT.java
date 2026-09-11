package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * lazy 생성의 경계와 연·월 오류 — quickstart #53·#54.
 *
 * <h2>#53 — 생성은 "처음 열 때"만이다</h2>
 *
 * <p>이미 연 달에 고정지출을 <b>새로 등록해도 그 달 목록은 자동으로 늘지 않는다.</b>
 * 놀랄 수 있지만 lazy 생성 모델의 직접적인 결과다 — 생성 판정이 "이 설정의 그 연·월 행이
 * 있는가"가 아니라 {@code INSERT ... ON CONFLICT DO NOTHING} 이라 새 설정은 다음에 그 달을
 * 열 때 만들어진다.
 *
 * <p>...가 아니다. 실제로는 <b>다음 조회에서 만들어진다</b> — 4.8 이 매번
 * {@code ensureMonthlyRows} 를 부르고 그때 새 설정이 생성 대상에 들어오기 때문이다.
 * 그래서 이 시험은 <b>실제 동작을 확인하고 기록</b>한다. 설계 명세(#53)는 "4.9 를 먼저
 * 불러야 한다"고 안내하지만 그것은 <b>프론트가 의존해도 되는 최소 보장</b>이지 금지가
 * 아니다 — 4.9 는 값이 어긋난 행을 <b>갱신</b>하는 경로이고, 없는 행을 만드는 것은
 * 4.5·4.8 도 한다.
 *
 * <h2>#54 — 연·월 오류가 {@code 3501} 이다</h2>
 *
 * <p>4.5·4.6·4.9 는 {@code 3403} 인데 여기만 다르다. 자원별 코드 블록 배정
 * (고정지출 {@code 34xx} / 가계부 {@code 35xx})의 결과이며 <b>구현하며 가장 놓치기 쉬운
 * 지점</b>이다.
 */
class LedgerBoundaryIT extends AbstractLedgerIT {

    @Test
    @DisplayName("#54 연·월이 없거나 범위 밖이면 3501 이다 — 4.5 의 3403 이 아니다")
    void invalidYearMonthIs3501() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL, fixture.token()))).isEqualTo(3501);
        assertThat(resCode(getJson(URL + "?year=2026", fixture.token()))).isEqualTo(3501);
        assertThat(resCode(getJson(URL + "?month=7", fixture.token()))).isEqualTo(3501);
        assertThat(resCode(getJson(URL + "?year=2026&month=13", fixture.token()))).isEqualTo(3501);
        assertThat(resCode(getJson(URL + "?year=2026&month=0", fixture.token()))).isEqualTo(3501);
        assertThat(resCode(getJson(URL + "?year=20026&month=7", fixture.token()))).isEqualTo(3501);
    }

    @Test
    @DisplayName("#54 같은 오류를 4.5 는 3403 으로 낸다 — 의도된 차이다")
    void theSameErrorIs3403OnTheMonthlyList() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson("/api/v1/fixed-expenses/monthly?year=2026&month=13",
                fixture.token()))).isEqualTo(3403);
        assertThat(resCode(getJson(URL + "?year=2026&month=13", fixture.token()))).isEqualTo(3501);
    }

    @Test
    @DisplayName("#53 이미 연 달에 고정지출을 새로 등록하면 다음 조회에서 나타난다")
    void newSettingAppearsOnTheNextRead() throws Exception {
        Fixture fixture = prepare();
        assertThat(ledger(fixture).get("data").get("list")).hasSize(4);

        createFixedExpense(fixture.token(), "통신비", fixture.expenseMethodId(),
                defaultGroupId(fixture.member(), "통신"), 60000L, 20, "2026-07", "2026-12");

        // 등록 자체는 그 달 행을 만들지 않는다(FR-402) — 다음 조회의 lazy 생성이 만든다.
        JsonNode response = ledger(fixture);
        assertThat(response.get("data").get("list")).hasSize(5);
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isEqualTo(2);
    }

    @Test
    @DisplayName("#53 등록만으로는 그 달 행이 생기지 않는다 — FR-402")
    void creatingASettingDoesNotCreateMonthlyRows() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isEqualTo(1);

        createFixedExpense(fixture.token(), "통신비", fixture.expenseMethodId(),
                defaultGroupId(fixture.member(), "통신"), 60000L, 20, "2026-07", "2026-12");

        // 조회하기 전까지는 그대로 1건이다.
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isEqualTo(1);
    }

    @Test
    @DisplayName("#53 이미 만들어진 행의 값은 설정을 고쳐도 조회만으로 바뀌지 않는다")
    void editingASettingDoesNotRewriteAnExistingPastRow() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);

        assertThat(resCode(patchJson("/api/v1/fixed-expenses/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"amount":900000}
                        """))).isEqualTo(200);

        // 2026-07 은 지난 달도 미래 달도 아닌 고정 시점이지만, 자동 반영(FR-412)이
        // "미래 달"만 건드리므로 지금(2026-09) 기준 과거인 이 달은 그대로다.
        // 값을 맞추려면 4.9(재작성)를 부른다 — 그것이 자동과 수동의 경계다.
        assertThat(firstOfType(ledger(fixture), "FIXED").get("amount").asLong())
                .isEqualTo(500000L);
    }

    @Test
    @DisplayName("적용 기간 밖의 달에는 고정지출 행이 만들어지지 않는다")
    void outsideThePeriodNoFixedRow() throws Exception {
        Fixture fixture = prepare();

        // 고정지출 기간은 2026-07 ~ 2026-12 다.
        JsonNode response = getJson(URL + "?year=2027&month=1", fixture.token());

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(typesOf(response)).doesNotContain("FIXED");
        assertThat(countMonthly(fixture.member(), 2027, 1)).isZero();
    }
}

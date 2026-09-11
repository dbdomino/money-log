package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.8 통합 목록의 조립 — quickstart #39·#40·#49·#52 (SC-408 · FR-417·418·422·423).
 *
 * <p><b>#39 가 이 스토리의 핵심이다.</b> 저장은 세 테이블로 나뉘어 있지만 사람은 한
 * 목록으로 본다 — 네 종류가 각 1건인 달을 조회하면 4건이 한 응답에 나와야 한다.
 *
 * <p><b>#40 이 lazy 생성을 건다.</b> 고정지출 설정만 있고 그 달 내역이 없을 때, 4.8 이
 * 4.5 와 <b>같은 규칙으로</b> 만들어 저장한 뒤 목록에 넣어야 한다. 만들지 않으면 그 달을
 * 처음 여는 사용자에게 고정지출이 아예 보이지 않는다.
 */
class LedgerAssembleIT extends AbstractLedgerIT {

    @Test
    @DisplayName("#39 네 종류가 각 1건인 달을 조회하면 4건이 한 목록에 나온다")
    void fourKindsInOneList() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("list")).hasSize(4);
        assertThat(typesOf(response)).containsExactlyInAnyOrder(
                "EXPENSE", "INSTALLMENT", "INCOME", "FIXED");
    }

    @Test
    @DisplayName("#39 각 행의 type 이 정확하다 — 일반 지출과 할부가 갈린다")
    void expenseAndInstallmentAreDistinguished() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture);

        JsonNode plain = firstOfType(response, "EXPENSE");
        JsonNode installment = firstOfType(response, "INSTALLMENT");
        // 둘은 같은 테이블의 같은 형태 행이고 차이는 할부 3컬럼뿐이다.
        assertThat(plain.get("installmentGroupId").isNull()).isTrue();
        assertThat(installment.get("installmentGroupId").isNull()).isFalse();
    }

    @Test
    @DisplayName("#40 그 달 고정지출 내역이 없어도 조회가 만들어 저장한 뒤 목록에 넣는다")
    void lazyCreationHappensOnLedgerRead() throws Exception {
        Fixture fixture = prepare();
        // 4.8 을 부르기 전에는 월별 내역이 없다.
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isZero();

        JsonNode response = ledger(fixture);

        assertThat(firstOfType(response, "FIXED")).isNotNull();
        // 응답에만 넣고 저장하지 않으면 여기서 0 이 나온다.
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isEqualTo(1);
    }

    @Test
    @DisplayName("#40 4.8 이 만든 행을 4.5 가 그대로 본다 — 같은 규칙이다")
    void rowsCreatedByLedgerAreVisibleToMonthlyList() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);

        JsonNode monthly = getJson("/api/v1/fixed-expenses/monthly?year=" + YEAR
                + "&month=" + MONTH, fixture.token());

        assertThat(monthly.get("data").get("list")).hasSize(1);
        // 두 API 가 각자 만들면 중복 행이 생기거나 값이 갈린다.
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isEqualTo(1);
    }

    @Test
    @DisplayName("#49 year·month·expenseTotal·incomeTotal 이 list 와 같은 레벨이다")
    void additionalFieldsAreSiblingsOfList() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture).get("data");

        assertThat(data.get("year").asInt()).isEqualTo(YEAR);
        assertThat(data.get("month").asInt()).isEqualTo(MONTH);
        assertThat(data.has("expenseTotal")).isTrue();
        assertThat(data.has("incomeTotal")).isTrue();
        assertThat(data.get("list").isArray()).isTrue();
    }

    @Test
    @DisplayName("#52 응답에 페이징 필드가 하나도 없다")
    void noPagingFieldsAtAll() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = ledger(fixture).get("data");

        assertThat(data.has("offset")).isFalse();
        assertThat(data.has("limit")).isFalse();
        assertThat(data.has("totalCount")).isFalse();
        // 최상위는 resCode·data 둘뿐이다.
        assertThat(ledger(fixture).size()).isEqualTo(2);
    }

    @Test
    @DisplayName("거래가 하나도 없는 달은 빈 배열이고 합계가 0 이다")
    void emptyMonthIsAnEmptyArray() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = getJson(URL + "?year=2030&month=3", fixture.token()).get("data");

        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.get("list")).isEmpty();
        assertThat(data.get("expenseTotal").asLong()).isZero();
        assertThat(data.get("incomeTotal").asLong()).isZero();
    }

    @Test
    @DisplayName("남의 거래가 섞이지 않는다")
    void listIsScopedToTheOwner() throws Exception {
        Fixture other = prepare();
        ledger(other);
        Fixture fixture = prepare();

        JsonNode response = ledger(fixture);

        assertThat(response.get("data").get("list")).hasSize(4);
    }

    @Test
    @DisplayName("별도의 가계부 테이블을 만들지 않는다 — 조회 시점에 합칠 뿐이다")
    void noSeparateLedgerRowsAreCreated() throws Exception {
        Fixture fixture = prepare();

        ledger(fixture);
        ledger(fixture);
        ledger(fixture);

        // 세 번 불러도 고정지출 월별 내역 1건 외에는 아무 행도 생기지 않는다.
        assertThat(countMonthlyAll(fixture.member())).isEqualTo(1);
        assertThat(ledger(fixture).get("data").get("list")).hasSize(4);
    }

    @Test
    @DisplayName("토큰 없이 조회하면 1001 이고 아무것도 만들어지지 않는다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL + "?year=" + YEAR + "&month=" + MONTH, null)))
                .isEqualTo(1001);
        assertThat(countMonthly(fixture.member(), YEAR, MONTH)).isZero();
    }
}

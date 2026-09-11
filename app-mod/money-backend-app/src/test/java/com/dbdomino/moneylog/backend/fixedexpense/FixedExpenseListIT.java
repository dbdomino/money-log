package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.2 고정지출 설정 목록 — quickstart #6·#7.
 *
 * <p><b>005 의 목록 넷 중 페이징이 있는 것은 여기뿐이다.</b> 4.5·4.8·4.9 는 한 달치를
 * 전부 돌려주고 합계·건수를 대신 싣는다. 그래서 이 시험이 페이징 규칙을 지키는 유일한
 * 자리다.
 */
class FixedExpenseListIT extends AbstractFixedExpenseIT {

    /** 설정 {@code count} 건을 만든다. 이름만 다르다. */
    private void createMany(Fixture fixture, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            createFixedExpense(fixture.token(), "고정지출" + i, fixture.paymentMethodId(),
                    fixture.expendGroupId(), 10000L * (i + 1), 25, START, END);
        }
    }

    @Test
    @DisplayName("#6 data.list 가 object 배열이고 offset·limit·totalCount 가 형제 필드다")
    void listCarriesPagingFieldsAsSiblings() throws Exception {
        Fixture fixture = prepare();
        createMany(fixture, 3);

        JsonNode data = getJson(URL + "?offset=0&limit=10", fixture.token()).get("data");

        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.get("list")).hasSize(3);
        assertThat(data.get("list").get(0).isObject()).isTrue();
        // list 안이 아니라 같은 레벨이다(FR-423). data.list 와 data.totalCount 가 형제다.
        assertThat(data.get("offset").asInt()).isZero();
        assertThat(data.get("limit").asInt()).isEqualTo(10);
        assertThat(data.get("totalCount").asLong()).isEqualTo(3);
    }

    @Test
    @DisplayName("#6 totalCount 는 전체 건수다 — 현재 페이지 건수가 아니다")
    void totalCountIsTheWholeMatchNotThePage() throws Exception {
        Fixture fixture = prepare();
        createMany(fixture, 5);

        JsonNode data = getJson(URL + "?offset=0&limit=2", fixture.token()).get("data");

        assertThat(data.get("list")).hasSize(2);
        // 2 가 나오면 화면이 마지막 페이지를 계산할 수 없다.
        assertThat(data.get("totalCount").asLong()).isEqualTo(5);
    }

    @Test
    @DisplayName("#6 두 번째 페이지가 첫 페이지와 겹치지 않는다")
    void secondPageDoesNotOverlap() throws Exception {
        Fixture fixture = prepare();
        createMany(fixture, 5);

        JsonNode first = getJson(URL + "?offset=0&limit=2", fixture.token()).get("data").get("list");
        JsonNode second = getJson(URL + "?offset=2&limit=2", fixture.token()).get("data").get("list");

        assertThat(first.get(0).get("fixedExpenseId").asLong())
                .isNotEqualTo(second.get(0).get("fixedExpenseId").asLong());
        assertThat(second).hasSize(2);
    }

    @Test
    @DisplayName("#7 offset 이 limit 의 배수가 아니면 9001 이다")
    void offsetNotMultipleOfLimitIs9001() throws Exception {
        Fixture fixture = prepare();
        createMany(fixture, 3);

        // 경계에 맞지 않는 offset 은 목록을 겹쳐 보이게 한다.
        assertThat(resCode(getJson(URL + "?offset=3&limit=2", fixture.token()))).isEqualTo(9001);
    }

    @Test
    @DisplayName("#7 offset·limit 이 빠지면 9001 이다 — 기본값이 없다")
    void missingPagingParamsIs9001() throws Exception {
        Fixture fixture = prepare();

        // 기본값을 채워 조용히 통과시키면 프론트가 페이징을 빠뜨린 채 동작해 버린다.
        assertThat(resCode(getJson(URL, fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(URL + "?limit=10", fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(URL + "?offset=0", fixture.token()))).isEqualTo(9001);
    }

    @Test
    @DisplayName("#7 limit 이 0 이하이거나 offset 이 음수면 9001 이다")
    void outOfRangePagingIs9001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL + "?offset=0&limit=0", fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(URL + "?offset=-1&limit=10", fixture.token()))).isEqualTo(9001);
    }

    @Test
    @DisplayName("목록에 남의 설정이 섞이지 않는다")
    void listIsScopedToTheOwner() throws Exception {
        Fixture other = prepare();
        createMany(other, 3);
        Fixture fixture = prepare();
        createMany(fixture, 2);

        JsonNode data = getJson(URL + "?offset=0&limit=10", fixture.token()).get("data");

        assertThat(data.get("totalCount").asLong()).isEqualTo(2);
    }

    @Test
    @DisplayName("설정이 하나도 없으면 빈 배열이고 totalCount 는 0 이다")
    void emptyListIsAnArrayNotNull() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = getJson(URL + "?offset=0&limit=10", fixture.token()).get("data");

        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.get("list")).isEmpty();
        assertThat(data.get("totalCount").asLong()).isZero();
    }

    @Test
    @DisplayName("목록의 각 요소도 현재 이름을 담는다")
    void itemsCarryCurrentNames() throws Exception {
        Fixture fixture = prepare();
        createDefaultFixedExpense(fixture);

        JsonNode item = getJson(URL + "?offset=0&limit=10", fixture.token())
                .get("data").get("list").get(0);

        assertThat(item.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(item.get("expendGroupName").asString()).isEqualTo("주거");
    }
}

package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.5 lazy 생성 — quickstart #13·#16·#17·#20 (FR-406·FR-408).
 *
 * <p><b>조회가 쓰기를 일으킨다.</b> 그 연·월을 처음 열면 설정에서 복사해 만들어 저장하고
 * 함께 돌려준다. 그래서 "만들어졌는가"는 응답이 아니라 <b>DB 를 봐야</b> 안다 —
 * 응답 목록의 길이는 필터가 걸리면 달라진다.
 */
class MonthlyLazyCreateIT extends AbstractMonthlyIT {

    @Test
    @DisplayName("#13 적용 기간 안의 달을 처음 조회하면 만들어져 저장되고 함께 돌아온다")
    void firstOpenCreatesAndReturns() throws Exception {
        Fixture fixture = prepare();
        assertThat(countMonthly(fixture.member(), 2026, 11)).isZero();

        JsonNode data = listMonthly(fixture, 2026, 11).get("data");

        assertThat(data.get("list")).hasSize(1);
        // 응답만 보면 "계산해서 돌려줬을" 수도 있다. 저장을 확인한다.
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#13 응답에 year·month·total 이 list 와 같은 레벨로 온다")
    void additionalFieldsAreSiblingsOfList() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = listMonthly(fixture, 2026, 11).get("data");

        assertThat(data.get("year").asInt()).isEqualTo(2026);
        assertThat(data.get("month").asInt()).isEqualTo(11);
        assertThat(data.get("total").asLong()).isEqualTo(500000L);
        assertThat(data.get("list").isArray()).isTrue();
    }

    @Test
    @DisplayName("#13 응답에 페이징 필드가 없다 — 한 달치를 전부 돌려준다")
    void noPagingFields() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = listMonthly(fixture, 2026, 11).get("data");

        assertThat(data.has("offset")).isFalse();
        assertThat(data.has("limit")).isFalse();
        assertThat(data.has("totalCount")).isFalse();
    }

    @Test
    @DisplayName("#16 적용 기간 밖의 달은 만들어지지 않는다")
    void outsideThePeriodNothingIsCreated() throws Exception {
        Fixture fixture = prepare();

        // 적용 기간은 2026-11 ~ 2027-02 다. 앞뒤로 하나씩 벗어난 달을 연다.
        assertThat(listMonthly(fixture, 2026, 10).get("data").get("list")).isEmpty();
        assertThat(listMonthly(fixture, 2027, 3).get("data").get("list")).isEmpty();

        assertThat(countMonthly(fixture.member(), 2026, 10)).isZero();
        assertThat(countMonthly(fixture.member(), 2027, 3)).isZero();
    }

    @Test
    @DisplayName("#16 기간 경계의 두 달은 만들어진다 — 양 끝을 포함한다")
    void bothEndsOfThePeriodAreIncluded() throws Exception {
        Fixture fixture = prepare();

        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list")).hasSize(1);
        assertThat(listMonthly(fixture, 2027, 2).get("data").get("list")).hasSize(1);
    }

    @Test
    @DisplayName("#17 걸리는 고정지출이 하나도 없는 달은 list 가 빈 배열이다")
    void noApplicableSettingGivesAnEmptyArray() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = listMonthly(fixture, 2030, 6).get("data");

        // null 이 아니라 빈 배열이어야 한다 — 프론트가 길이만 보면 되게.
        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.get("list")).isEmpty();
        assertThat(data.get("total").asLong()).isZero();
    }

    @Test
    @DisplayName("#17 설정이 하나도 없는 회원도 빈 배열을 받는다")
    void memberWithoutAnySettingGetsAnEmptyArray() throws Exception {
        Member member = signupAndLogin();

        JsonNode data = getJson(LIST_URL + "?year=2026&month=11", member.token()).get("data");

        assertThat(data.get("list")).isEmpty();
    }

    @Test
    @DisplayName("#20 적용 기간 2026-11~2027-02 의 2027-01 이 만들어진다 — 해를 넘겨도 기간 안")
    void januaryAcrossTheYearBoundaryIsInside() throws Exception {
        Fixture fixture = prepare();

        // 연·월을 따로 비교하면 1월(1)이 시작 월(11)보다 작아 빠져 버린다.
        // 합성 비교가 아니면 여기서 걸린다.
        assertThat(listMonthly(fixture, 2027, 1).get("data").get("list")).hasSize(1);
        assertThat(countMonthly(fixture.member(), 2027, 1)).isEqualTo(1);
    }

    @Test
    @DisplayName("#13 만들어진 행이 설정값을 그대로 복사한다")
    void createdRowCopiesTheSetting() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        var row = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11);

        assertThat(row.get("amount")).isEqualTo(500000L);
        assertThat(row.get("content")).isEqualTo("월세");
        // 새로 만든 행은 사용자가 손대지 않았다.
        assertThat(row.get("modified")).isEqualTo(false);
    }

    @Test
    @DisplayName("여러 고정지출이 걸리면 전부 만들어진다")
    void everyApplicableSettingIsCreated() throws Exception {
        Fixture fixture = prepare();
        addFixedExpense(fixture, "통신비", fixture.paymentMethodId(), 60000L);
        addFixedExpense(fixture, "구독료", fixture.paymentMethodId(), 15000L);

        JsonNode data = listMonthly(fixture, 2026, 11).get("data");

        assertThat(data.get("list")).hasSize(3);
        assertThat(data.get("total").asLong()).isEqualTo(500000L + 60000L + 15000L);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(3);
    }

    @Test
    @DisplayName("남의 달을 열어도 내 내역만 만들어진다")
    void creationIsScopedToTheOwner() throws Exception {
        Fixture other = prepare();
        Fixture fixture = prepare();

        listMonthly(fixture, 2026, 11);

        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
        // 남의 설정은 그 사람이 열 때 만들어진다.
        assertThat(countMonthly(other.member(), 2026, 11)).isZero();
    }

    @Test
    @DisplayName("연·월이 없거나 범위 밖이면 3403 이다 — 4.8 의 3501 과 다르다")
    void invalidYearMonthIs3403() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(LIST_URL, fixture.token()))).isEqualTo(3403);
        assertThat(resCode(getJson(LIST_URL + "?year=2026", fixture.token()))).isEqualTo(3403);
        assertThat(resCode(getJson(LIST_URL + "?year=2026&month=13", fixture.token())))
                .isEqualTo(3403);
        assertThat(resCode(getJson(LIST_URL + "?year=2026&month=0", fixture.token())))
                .isEqualTo(3403);
        // 연 오타. 애플리케이션이 막지 않으면 빈 목록이 돌아와 "그 달엔 없다"로 읽힌다.
        assertThat(resCode(getJson(LIST_URL + "?year=20026&month=11", fixture.token())))
                .isEqualTo(3403);
    }

    @Test
    @DisplayName("토큰 없이 조회하면 1001 이고 아무것도 만들어지지 않는다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(LIST_URL + "?year=2026&month=11", null))).isEqualTo(1001);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isZero();
    }
}

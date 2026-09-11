package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 5.1 목록 — quickstart #15·#16 (FR-526).
 *
 * <h2>두 실패 코드가 다르다</h2>
 *
 * <p>연·월은 {@code 3603}, 페이징은 {@code 9001} 이다. 같은 요청의 두 검증인데 코드가
 * 갈리는 것은 연·월이 <b>자원의 좌표</b>이고 페이징은 <b>요청 형식</b>이기 때문이다.
 *
 * <h2>{@code totalCount} 는 목록과 같은 모집단에서 센다</h2>
 *
 * <p>목록이 사용 중 유형만 돌려주는데 건수만 전부 세면 화면이 있지도 않은 페이지를
 * 그린다. 그래서 유형 하나를 {@code in_use=false} 로 돌린 뒤 <b>건수가 함께 줄어드는지</b>
 * 를 본다 — 목록 길이만 보는 시험은 이 실수를 잡지 못한다.
 *
 * <h2>연·월이 필수인 이유</h2>
 *
 * <p>기본 목표만 보는 화면도 조회 연·월을 넘긴다. 모드 분기를 두지 않아 응답 형태가
 * 하나로 유지된다 — {@code monthlyTargetAmount} 가 항상 있으려면 어느 달인지 알아야 한다.
 */
class TargetListIT extends AbstractTargetIT {

    @Test
    @DisplayName("#15 offset 이 limit 의 배수가 아니면 9001 이다")
    void rejectsUnalignedOffset() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(listTargets(fixture, 3, 10))).isEqualTo(9001);
    }

    @Test
    @DisplayName("페이징이 빠지거나 범위를 벗어나면 9001 이다")
    void rejectsBadPaging() throws Exception {
        Fixture fixture = prepare();
        String base = URL + "?year=" + YEAR + "&month=" + MONTH;

        assertThat(resCode(getJson(base, fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(base + "&offset=0", fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(base + "&limit=10", fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(base + "&offset=0&limit=0", fixture.token()))).isEqualTo(9001);
        assertThat(resCode(getJson(base + "&offset=-10&limit=10", fixture.token())))
                .isEqualTo(9001);
    }

    @Test
    @DisplayName("연·월이 빠지거나 범위를 벗어나면 3603 이다 — 9001 이 아니다")
    void rejectsBadYearMonth() throws Exception {
        Fixture fixture = prepare();
        String paging = "&offset=0&limit=10";

        assertThat(resCode(getJson(URL + "?month=" + MONTH + paging, fixture.token())))
                .isEqualTo(3603);
        assertThat(resCode(getJson(URL + "?year=" + YEAR + paging, fixture.token())))
                .isEqualTo(3603);
        assertThat(resCode(getJson(URL + "?year=" + YEAR + "&month=13" + paging, fixture.token())))
                .isEqualTo(3603);
        assertThat(resCode(getJson(URL + "?year=1999&month=" + MONTH + paging, fixture.token())))
                .isEqualTo(3603);
        assertThat(resCode(getJson(URL + "?year=2101&month=" + MONTH + paging, fixture.token())))
                .isEqualTo(3603);
    }

    /**
     * 연·월 검증이 페이징보다 <b>먼저</b>다.
     *
     * <p>둘 다 어긋난 요청에서 자원 좌표가 성립하지 않는 쪽을 먼저 알린다.
     */
    @Test
    @DisplayName("연·월과 페이징이 둘 다 어긋나면 3603 이다")
    void yearMonthBeatsPaging() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL + "?year=1999&month=13&offset=3&limit=10",
                fixture.token()))).isEqualTo(3603);
    }

    @Test
    @DisplayName("#16 totalCount 는 사용 중 유형만 센다")
    void countsOnlyInUseGroups() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");

        long before = listTargets(fixture, 0, 20).get("data").get("totalCount").asLong();

        setExpendGroupInUse(fixture.token(), groupId, false);

        JsonNode after = listTargets(fixture, 0, 20);
        assertThat(after.get("data").get("totalCount").asLong()).isEqualTo(before - 1);
        assertThat(after.get("data").get("list").size())
                .isEqualTo((int) after.get("data").get("totalCount").asLong());
    }

    /**
     * 목표 행이 없는 유형도 <b>줄이 나온다</b>.
     *
     * <p>모집단이 목표 행이 아니라 사용 중 지출유형이기 때문이다(FR-509). 목표 행에서
     * 출발하는 구현은 아직 목표를 정하지 않은 유형을 통째로 빠뜨려, 사용자가 값을 넣을
     * 자리 자체가 화면에서 사라진다.
     */
    @Test
    @DisplayName("목표를 한 번도 정하지 않은 유형도 목록에 나온다")
    void listsGroupsWithoutTargets() throws Exception {
        Fixture fixture = prepare();

        JsonNode row = rowOf(listTargets(fixture, 0, 20), fixture.foodGroupId());

        assertThat(row).isNotNull();
        assertThat(row.get("defaultTargetAmount").asLong()).isZero();
        assertThat(row.get("monthlyTargetAmount").isNull()).isTrue();
    }

    @Test
    @DisplayName("응답이 요청 연·월과 페이징을 그대로 되돌려준다")
    void echoesQuery() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = listTargets(fixture, 0, 2).get("data");

        assertThat(data.get("year").asInt()).isEqualTo(YEAR);
        assertThat(data.get("month").asInt()).isEqualTo(MONTH);
        assertThat(data.get("offset").asInt()).isZero();
        assertThat(data.get("limit").asInt()).isEqualTo(2);
        assertThat(data.get("list").size()).isLessThanOrEqualTo(2);
    }

    /** 페이지를 넘겨도 같은 줄이 겹쳐 나오지 않는다. */
    @Test
    @DisplayName("offset 이 다음 페이지를 가리킨다")
    void paginates() throws Exception {
        Fixture fixture = prepare();

        long total = listTargets(fixture, 0, 100).get("data").get("totalCount").asLong();
        assertThat(total).isGreaterThan(2);

        var first = groupIdsOf(listTargets(fixture, 0, 2));
        var second = groupIdsOf(listTargets(fixture, 2, 2));

        assertThat(first).hasSize(2);
        assertThat(second).doesNotContainAnyElementsOf(first);
    }

    @Test
    @DisplayName("expendGroupName 은 부분 일치로 걸린다")
    void filtersByPartialName() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = getJson(URL + "?year=" + YEAR + "&month=" + MONTH
                + "&offset=0&limit=20&expendGroupName=식", fixture.token());

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(groupIdsOf(response)).containsExactly(fixture.foodGroupId());
        assertThat(response.get("data").get("totalCount").asLong()).isEqualTo(1);
    }
}

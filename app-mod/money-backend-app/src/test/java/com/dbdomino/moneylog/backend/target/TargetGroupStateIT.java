package com.dbdomino.moneylog.backend.target;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 지출유형의 상태와 목표금액 — quickstart #11·#12·#13·#14 (FR-509·510·511).
 *
 * <h2>{@code in_use} 와 {@code deleted} 는 다른 조건이다</h2>
 *
 * <table border="1">
 *   <caption>유형 상태별 목표금액 API 의 반응</caption>
 *   <tr><th>상태</th><th>5.1 목록</th><th>5.2 단건 · 5.3 · 5.4</th></tr>
 *   <tr><td>{@code in_use=false}</td><td><b>제외</b></td><td><b>{@code 3601}</b></td></tr>
 *   <tr><td>{@code deleted=true} (사용 중인 채)</td><td>나온다</td><td>정상</td></tr>
 * </table>
 *
 * <p><b>목표금액이 보는 것은 {@code in_use} 뿐이다</b>(target-amount.md §5). 삭제 표시는
 * 003 이 과거 기록을 지우지 않으려고 고른 방식이고, 그걸 여기서 조건으로 삼으면 FR-511
 * ("삭제 표시돼도 목표금액 행과 참조는 유지된다")이 성립할 수 없다 — 행은 남는데 읽을
 * 방법이 없어진다.
 *
 * <p><b>#14 가 판정 순서를 검증한다.</b> 남의 유형 ID 는 그것이 {@code in_use=false} 여도
 * {@code 3103} 이다. {@code 3601} 이 나오면 그 ID 가 실재한다는 사실이 새어 나간다.
 */
class TargetGroupStateIT extends AbstractTargetIT {

    @Test
    @DisplayName("#11 in_use=false 유형의 단건 조회는 3601 이다")
    void rejectsGetOnUnusedGroup() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");
        setExpendGroupInUse(fixture.token(), groupId, false);

        assertThat(resCode(getJson(URL + "/" + YEAR + "/" + MONTH + "/" + groupId,
                fixture.token()))).isEqualTo(3601);
    }

    @Test
    @DisplayName("#11 in_use=false 유형의 목표 변경은 3601 이다 — 기본·월별 둘 다")
    void rejectsUpsertOnUnusedGroup() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");
        setExpendGroupInUse(fixture.token(), groupId, false);

        assertThat(resCode(putDefault(fixture, groupId, 50_000L))).isEqualTo(3601);
        assertThat(resCode(putMonthly(fixture, groupId, 50_000L))).isEqualTo(3601);
        assertThat(countDefaultTargets(fixture.member())).isZero();
        assertThat(countMonthlyTargets(fixture.member())).isZero();
    }

    @Test
    @DisplayName("#12 in_use=false 유형은 5.1 목록에서 제외된다")
    void excludesUnusedGroupFromList() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");

        assertThat(groupIdsOf(listTargets(fixture, 0, 20))).contains(groupId);

        setExpendGroupInUse(fixture.token(), groupId, false);

        assertThat(groupIdsOf(listTargets(fixture, 0, 20))).doesNotContain(groupId);
    }

    /**
     * #13 — 목표를 정해 둔 유형을 삭제 표시해도 <b>행과 참조가 남는다</b>(FR-511).
     *
     * <p>DB 행 수로 직접 센다. 응답만 보면 "읽히지 않는다"와 "지워졌다"가 구분되지 않는다.
     */
    @Test
    @DisplayName("#13 삭제 표시된 유형의 목표금액 행은 유지된다")
    void keepsTargetRowsAfterGroupDeleted() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");
        putDefault(fixture, groupId, 50_000L);
        putMonthly(fixture, groupId, 30_000L);

        assertThat(resCode(deleteJson("/api/v1/expend-groups/" + groupId, fixture.token())))
                .isEqualTo(200);

        assertThat(countDefaultTargets(fixture.member())).isEqualTo(1);
        assertThat(countMonthlyTargets(fixture.member())).isEqualTo(1);

        JsonNode data = getJson(URL + "/" + YEAR + "/" + MONTH + "/" + groupId,
                fixture.token()).get("data");
        assertThat(data.get("defaultTargetAmount").asLong()).isEqualTo(50_000L);
        assertThat(data.get("monthlyTargetAmount").asLong()).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("#14 남의 유형 ID 는 3103 이다 — 그것이 in_use=false 여도")
    void hidesOtherMembersGroupEvenWhenUnused() throws Exception {
        Fixture mine = prepare();
        Fixture other = prepare();
        long otherGroupId = createExpendGroup(other.token(), "구독");
        setExpendGroupInUse(other.token(), otherGroupId, false);

        assertThat(resCode(getJson(URL + "/" + YEAR + "/" + MONTH + "/" + otherGroupId,
                mine.token()))).isEqualTo(3103);
        assertThat(resCode(putDefault(mine, otherGroupId, 50_000L))).isEqualTo(3103);
        assertThat(resCode(putMonthly(mine, otherGroupId, 50_000L))).isEqualTo(3103);
    }

    @Test
    @DisplayName("없는 유형 ID 도 3103 이다")
    void rejectsUnknownGroup() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(getJson(URL + "/" + YEAR + "/" + MONTH + "/999999",
                fixture.token()))).isEqualTo(3103);
    }

    /**
     * 삭제 표시된 유형이 <b>사용 중인 채</b>라면 목록에 남는다(target-amount.md §5).
     *
     * <p>놀라워 보이는 규칙이라 명시적으로 걸어 둔다. 003 의 "사용 중 목록"(2.13)이
     * {@code deleted} 까지 보는 것과 달라, 그쪽 리포지토리 메서드를 재사용하면 이 줄이
     * 사라진다 — 그러면 FR-511 이 지키려던 목표 행을 화면에서 읽을 방법이 없어진다.
     */
    @Test
    @DisplayName("FR-509 삭제 표시만 된 유형은 사용 중인 채라 목록에 남는다")
    void keepsDeletedButInUseGroupInList() throws Exception {
        Fixture fixture = prepare();
        long groupId = createExpendGroup(fixture.token(), "구독");
        putDefault(fixture, groupId, 50_000L);

        assertThat(resCode(deleteJson("/api/v1/expend-groups/" + groupId, fixture.token())))
                .isEqualTo(200);

        JsonNode row = rowOf(listTargets(fixture, 0, 20), groupId);
        assertThat(row).isNotNull();
        assertThat(row.get("defaultTargetAmount").asLong()).isEqualTo(50_000L);
    }
}

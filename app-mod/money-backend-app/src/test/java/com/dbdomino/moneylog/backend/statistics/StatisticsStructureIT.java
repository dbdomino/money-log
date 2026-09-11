package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 통계 저장 구조 — quickstart #51·#52·#54 (SC-509 · FR-519·526).
 *
 * <h2>FK 가 없는 것이 결정이다</h2>
 *
 * <p>{@code tbl_statistics_expend_group}·{@code tbl_statistics_payment_method} 의
 * 지출유형·수단 참조에 <b>FK 가 없다</b>. API 로는 확인할 수 없어 {@code jdbc} 로
 * {@code information_schema} 를 직접 읽는다.
 *
 * <p><b>구현자가 "FK 가 빠진 실수"로 오해해 추가하면 이 시험이 잡는다.</b> 추가하면 둘 중
 * 하나가 일어난다 — {@code RESTRICT} 면 원본 삭제가 막히고, {@code CASCADE} 면 과거 통계가
 * 원본과 함께 사라진다. 어느 쪽이든 "저장본은 불변"(FR-518)이 깨진다.
 *
 * <p>그 대신 이름 두 컬럼이 <b>NOT NULL</b> 이고, 그것이 원본이 사라진 뒤 화면을 복원한다.
 *
 * <h2>#54 — 배열 3종은 목록 응답이 아니다</h2>
 *
 * <p>{@code data.list} 규칙의 적용 대상이 아니므로 통계 응답에 {@code list} 도
 * {@code totalCount} 도 없어야 한다. 셋이 한 통계 객체의 <b>구성 요소</b>이지 각각이
 * 목록 API 의 결과가 아니기 때문이다.
 */
class StatisticsStructureIT extends AbstractStatisticsIT {

    /** 그 테이블에서 나가는 FK 의 참조 대상 테이블 이름들. */
    private List<String> foreignKeyTargetsOf(String table) {
        return jdbc.queryForList("""
                select ccu.table_name
                  from information_schema.table_constraints tc
                  join information_schema.constraint_column_usage ccu
                    on ccu.constraint_name = tc.constraint_name
                   and ccu.constraint_schema = tc.constraint_schema
                 where tc.constraint_type = 'FOREIGN KEY'
                   and tc.table_schema = 'moneylog'
                   and tc.table_name = ?
                """, String.class, table);
    }

    @Test
    @DisplayName("#51 통계 상세 2종에 지출유형·수단으로 나가는 FK 가 0건이다")
    void detailTablesHaveNoReferenceForeignKeys() {
        assertThat(foreignKeyTargetsOf("tbl_statistics_expend_group"))
                .as("유형별 상세의 FK 대상")
                .doesNotContain("tbl_user_expend_group");
        assertThat(foreignKeyTargetsOf("tbl_statistics_payment_method"))
                .as("수단별 상세의 FK 대상")
                .doesNotContain("tbl_user_payment_method");
    }

    /**
     * 회원·통계로 나가는 FK 는 <b>있어야</b> 한다.
     *
     * <p>"FK 가 없다"가 상세 테이블 전체에 걸린 규칙으로 읽히면 이 둘까지 빠질 수 있다.
     * 없어지는 것은 <b>스냅샷 대상</b>으로 나가는 참조뿐이다.
     */
    @Test
    @DisplayName("#51 회원·통계로 나가는 FK 는 그대로 있다")
    void ownerAndParentForeignKeysRemain() {
        for (String table : List.of("tbl_statistics_expend_group",
                "tbl_statistics_payment_method", "tbl_statistics_weekly")) {
            assertThat(foreignKeyTargetsOf(table)).as("%s", table)
                    .contains("tbl_user", "tbl_statistics");
        }
    }

    @Test
    @DisplayName("#52 상세의 이름 컬럼이 NOT NULL 이다 — 화면 복원을 이 값이 맡는다")
    void nameColumnsAreNotNull() {
        assertThat(isNullable("tbl_statistics_expend_group", "expend_group_name"))
                .isEqualTo("NO");
        assertThat(isNullable("tbl_statistics_payment_method", "payment_method_name"))
                .isEqualTo("NO");
    }

    private String isNullable(String table, String column) {
        return jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                 where table_schema = 'moneylog' and table_name = ? and column_name = ?
                """, String.class, table, column);
    }

    @Test
    @DisplayName("#52 저장된 상세의 이름이 실제로 채워진다")
    void savedNamesAreFilled() throws Exception {
        Fixture fixture = prepare();
        int year = lastMonth().getYear();
        int month = lastMonth().getMonthValue();
        addExpense(fixture, lastMonth().atDay(3).toString(), 30_000L, fixture.foodGroupId());

        save(fixture, year, month);

        Map<String, Object> row = jdbc.queryForMap("""
                select g.expend_group_name, g.status
                  from moneylog.tbl_statistics_expend_group g
                  join moneylog.tbl_user u on u.id_key = g.id_key
                 where u.user_id = ?
                """, fixture.member().memberId());
        assertThat(row.get("expend_group_name")).isEqualTo("식비");
        assertThat(row.get("status")).isIn("UNDER", "OK", "OVER");
    }

    @Test
    @DisplayName("#54 통계 응답에 list 도 totalCount 도 없다 — 목록 API 가 아니다")
    void statisticsResponseIsNotAListResponse() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = statistics(fixture, FIXED_YEAR, FIXED_MONTH).get("data");

        assertThat(data.has("list")).isFalse();
        assertThat(data.has("totalCount")).isFalse();
        assertThat(data.has("offset")).isFalse();
        assertThat(data.has("limit")).isFalse();
        // 배열 3종은 각자 자기 이름으로 있다.
        assertThat(data.get("weeklyExpenses").isArray()).isTrue();
        assertThat(data.get("expendGroupSummaries").isArray()).isTrue();
        assertThat(data.get("paymentMethodSummaries").isArray()).isTrue();
    }

    /**
     * 반대로 <b>5.1 은 목록 API 라 {@code list} 규칙을 지킨다.</b>
     *
     * <p>006 안에서 두 규칙이 공존하는 것을 함께 못박는다 — 5.1 을 통계처럼 고치거나
     * 통계를 5.1 처럼 고치는 방향 모두 여기서 걸린다.
     */
    @Test
    @DisplayName("#54 5.1 목록은 반대로 data.list 규칙을 지킨다")
    void targetListFollowsTheListRule() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = getJson("/api/v1/expend-targets?year=" + FIXED_YEAR
                + "&month=" + FIXED_MONTH + "&offset=0&limit=10", fixture.token()).get("data");

        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.has("totalCount")).isTrue();
        assertThat(data.has("offset")).isTrue();
        assertThat(data.has("limit")).isTrue();
    }
}

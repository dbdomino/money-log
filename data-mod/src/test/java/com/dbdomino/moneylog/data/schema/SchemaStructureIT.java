package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스키마 구조 검증 — quickstart.md §2-1·§2-2·§2-3·§2-5·§2-6.
 *
 * <p>quickstart가 손으로 돌리라고 적어 둔 SQL 검증을 테스트로 옮긴 것이다. 손으로만
 * 두면 실행되지 않는 검증이 된다 — 이 절의 항목들은 "누군가 규칙을 어겼을 때"
 * 드러나야 하는데, 그 시점은 대체로 사람이 quickstart를 다시 펼치지 않는 때다.
 *
 * <p>회원 데이터를 만들지 않는다. 카탈로그만 읽으므로 상위 클래스의 정리 코드가
 * 지울 것도 없다.
 *
 * <p>§2-6(통계 상세의 FK 부재)은 {@link StatisticsBrokenRefIT}에도 있다. 보는 각도가
 * 다르다 — 저쪽은 <b>FK가 어디를 가리키는지</b>를 보고(유형·수단이 끼면 실패),
 * 이쪽은 <b>FK가 몇 개인지</b>를 본다. 둘 중 하나만 두면 "FK를 하나 더 붙였는데
 * 대상은 허용 목록 안"이거나 "개수는 맞는데 대상이 바뀐" 변경을 놓친다.
 */
class SchemaStructureIT extends AbstractSchemaIT {

    /** contracts/table-inventory.md가 정한 테이블 수. */
    private static final int EXPECTED_TABLE_COUNT = 15;

    /**
     * 레거시 {@code money-app}이 쓰는 테이블 이름. 이 기능은 하나도 만들지 않는다.
     *
     * <p>PostgreSQL은 따옴표 없는 식별자를 소문자로 접으므로 대소문자 차이는 충돌
     * 회피 근거가 되지 않는다 — 아래는 접힌 뒤의 이름이다.
     */
    private static final List<String> LEGACY_TABLE_NAMES = List.of(
            "tbl_member", "tbl_login_history", "tbl_payment_method", "tbl_card",
            "tbl_expend", "tbl_expend_group", "tbl_expend_fix", "tbl_ammount",
            "tbl_system_stat");

    @Test
    @DisplayName("§2-1 tbl_user% 테이블이 정확히 15개다")
    void schemaHasExactlyFifteenTables() {
        List<String> tables = inTx(() -> jdbc.queryForList("""
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'moneylog' AND tablename LIKE 'tbl_user%'
                ORDER BY tablename
                """, String.class));

        assertThat(tables).hasSize(EXPECTED_TABLE_COUNT);
        assertThat(tables).containsExactly(
                "tbl_user",
                "tbl_user_expend_group",
                "tbl_user_expend_target_default",
                "tbl_user_expend_target_monthly",
                "tbl_user_expense",
                "tbl_user_fixed_expense",
                "tbl_user_fixed_expense_monthly",
                "tbl_user_income",
                "tbl_user_login_history",
                "tbl_user_payment_method",
                "tbl_user_session",
                "tbl_user_statistics",
                "tbl_user_statistics_expend_group",
                "tbl_user_statistics_payment_method",
                "tbl_user_statistics_weekly");
    }

    @Test
    @DisplayName("§2-2 레거시 money-app 테이블 이름을 하나도 만들지 않는다")
    void doesNotCreateAnyLegacyTableName() {
        // 이름 개수만큼 자리표시자를 만든다. 자바 배열을 그대로 = ANY(?) 에 넘기면
        // 드라이버가 배열이 아니라 인자 여러 개로 펼쳐 자리표시자 수가 어긋난다.
        String placeholders = String.join(", ", java.util.Collections.nCopies(
                LEGACY_TABLE_NAMES.size(), "?"));

        List<String> collisions = inTx(() -> jdbc.queryForList(
                "SELECT tablename FROM pg_tables"
                        + " WHERE schemaname = 'moneylog' AND tablename IN (" + placeholders + ")"
                        + " ORDER BY tablename",
                String.class, LEGACY_TABLE_NAMES.toArray()));

        assertThat(collisions).isEmpty();
    }

    @Test
    @DisplayName("§2-3 기본키는 tbl_user 만 id_key 이고 나머지 14개는 idx 다")
    void onlyUserTableUsesIdKeyAsPrimaryKey() {
        List<Map<String, Object>> primaryKeys = inTx(() -> jdbc.queryForList("""
                SELECT c.relname AS table_name, a.attname AS pk_column
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
                WHERE i.indisprimary AND n.nspname = 'moneylog' AND c.relname LIKE 'tbl_user%'
                ORDER BY 1
                """));

        assertThat(primaryKeys).hasSize(EXPECTED_TABLE_COUNT);
        assertThat(primaryKeys)
                .allSatisfy(row -> {
                    String table = (String) row.get("table_name");
                    String expected = "tbl_user".equals(table) ? "id_key" : "idx";
                    assertThat(row)
                            .as("테이블 %s의 기본키 이름", table)
                            .containsEntry("pk_column", expected);
                });
    }

    @Test
    @DisplayName("§2-5 부분 유니크 인덱스 2건이 존재한다")
    void partialUniqueIndexesExist() {
        List<Map<String, Object>> partials = inTx(() -> jdbc.queryForList("""
                SELECT indexname, indexdef FROM pg_indexes
                WHERE schemaname = 'moneylog' AND indexdef LIKE '%WHERE%'
                ORDER BY indexname
                """));

        // Hibernate 가 만들지 못하는 것들이라, 보조 DDL 을 돌리지 않으면 여기서 빈다
        assertThat(partials).extracting(row -> row.get("indexname"))
                .containsExactly("ux_user_email", "ux_user_session_active");

        assertThat(indexDefOf(partials, "ux_user_email"))
                .contains("tbl_user")
                .contains("email IS NOT NULL");
        assertThat(indexDefOf(partials, "ux_user_session_active"))
                .contains("tbl_user_session")
                .contains("revoked = false");
    }

    @Test
    @DisplayName("§2-6 통계 상세 2종의 FK 는 회원과 소속 스냅샷 둘뿐이다")
    void statisticsDetailTablesCarryOnlyTwoForeignKeys() {
        List<Map<String, Object>> foreignKeys = inTx(() -> jdbc.queryForList("""
                SELECT conrelid::regclass::text AS table_name, conname
                FROM pg_constraint
                WHERE contype = 'f'
                  AND connamespace = 'moneylog'::regnamespace
                  AND conrelid::regclass::text IN (
                      'tbl_user_statistics_expend_group',
                      'tbl_user_statistics_payment_method')
                ORDER BY 1, 2
                """));

        // 유형·수단으로 나가는 FK 가 하나라도 늘면 개수가 2를 넘는다(FR-078a)
        assertThat(foreignKeys).extracting(row -> row.get("conname"))
                .containsExactlyInAnyOrder(
                        "fk_user_stat_group_user",
                        "fk_user_stat_group_statistics",
                        "fk_user_stat_method_user",
                        "fk_user_stat_method_statistics");
    }

    @Test
    @DisplayName("할부 그룹 시퀀스가 존재한다")
    void installmentGroupSequenceExists() {
        List<String> sequences = inTx(() -> jdbc.queryForList("""
                SELECT sequencename FROM pg_sequences
                WHERE schemaname = 'moneylog' AND sequencename = 'seq_installment_group'
                """, String.class));

        assertThat(sequences).containsExactly("seq_installment_group");
    }

    private String indexDefOf(List<Map<String, Object>> rows, String indexName) {
        return rows.stream()
                .filter(row -> indexName.equals(row.get("indexname")))
                .map(row -> (String) row.get("indexdef"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("인덱스를 찾지 못했다: " + indexName));
    }
}

package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 감사 컬럼의 전 테이블 적용 — quickstart.md §2-4, §3 시나리오 #20.
 *
 * <p>확인하려는 것: <b>15개 테이블 전부</b>가 {@code created_at}·{@code updated_at}·
 * {@code created_by}·{@code updated_by} 4종을 갖는가, 그리고 그 값 없이는 행이
 * 들어가지 않는가다(FR-004).
 *
 * <p>테이블을 하나씩 나열해 확인하지 않고 {@code information_schema}에 <b>질의</b>한다.
 * 나열하면 새 테이블이 생겼을 때 목록에 넣는 것을 잊어도 초록으로 통과한다 — 이
 * 검사의 목적이 바로 "빠뜨린 테이블이 없는가"라 그 방식으로는 목적을 이루지 못한다.
 *
 * <p>예외는 {@code tbl_user} 하나다. 회원가입은 자기 자신을 만드는 행위라 INSERT
 * 시점에 자기 {@code id_key}가 없고, 이는 {@code created_by}·{@code updated_by}
 * <b>둘 다</b>에 해당한다. 그래서 그 두 컬럼만 NULL 허용이다.
 */
class AuditColumnIT extends AbstractSchemaIT {

    /** 이 기능이 만드는 테이블 수. contracts/table-inventory.md가 정한 값이다. */
    private static final int EXPECTED_TABLE_COUNT = 15;

    private static final List<String> AUDIT_COLUMNS =
            List.of("created_at", "updated_at", "created_by", "updated_by");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Test
    @DisplayName("§2-4 15개 테이블이 모두 감사 컬럼 4종을 갖는다")
    void everyTableCarriesAllFourAuditColumns() {
        List<Map<String, Object>> rows = inTx(() -> jdbc.queryForList("""
                SELECT table_name,
                       count(*) FILTER (
                           WHERE column_name IN ('created_at','updated_at','created_by','updated_by')
                       ) AS audit_cols
                FROM information_schema.columns
                WHERE table_schema = 'moneylog' AND table_name LIKE 'tbl_user%'
                GROUP BY table_name
                ORDER BY table_name
                """));

        assertThat(rows).hasSize(EXPECTED_TABLE_COUNT);
        // 4종을 다 갖추지 못한 테이블이 하나라도 있으면 그 이름이 그대로 드러난다
        assertThat(rows)
                .allSatisfy(row -> assertThat(row)
                        .as("테이블 %s의 감사 컬럼 수", row.get("table_name"))
                        .containsEntry("audit_cols", (long) AUDIT_COLUMNS.size()));
    }

    @Test
    @DisplayName("§2-4 tbl_user 외의 모든 테이블에서 감사 4종이 NOT NULL 이다")
    void auditColumnsAreNotNullExceptOnUserTable() {
        List<Map<String, Object>> nullable = inTx(() -> jdbc.queryForList("""
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'moneylog'
                  AND table_name LIKE 'tbl_user%'
                  AND table_name <> 'tbl_user'
                  AND column_name IN ('created_at','updated_at','created_by','updated_by')
                  AND is_nullable = 'YES'
                ORDER BY table_name, column_name
                """));

        assertThat(nullable).isEmpty();
    }

    @Test
    @DisplayName("§2-4 tbl_user 만 created_by·updated_by 가 NULL 허용이다")
    void userTableAllowsNullAuthorColumnsOnly() {
        List<String> nullableColumns = inTx(() -> jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'moneylog' AND table_name = 'tbl_user'
                  AND column_name IN ('created_at','updated_at','created_by','updated_by')
                  AND is_nullable = 'YES'
                ORDER BY column_name
                """, String.class));

        // 시각 2종은 여기서도 NOT NULL 이다 — 없어도 되는 것은 "누가"이지 "언제"가 아니다
        assertThat(nullableColumns).containsExactly("created_by", "updated_by");
    }

    @Test
    @DisplayName("#20 created_by 없이 자식 테이블에 INSERT 하면 DB 가 NOT NULL 로 막는다")
    void childRowWithoutAuthorIsRejectedByDatabase() {
        User user = inTx(() -> userRepository.save(newUser()));

        // Entity 가 아니라 JdbcTemplate 으로 넣는다. Entity 경로는 Hibernate 가
        // @Column(nullable = false) 를 보고 DB 에 닿기 전에 PropertyValueException 을
        // 던져 버려서, 정작 확인하려는 DB NOT NULL 제약이 한 번도 실행되지 않는다.
        // 이 시험의 목적은 "DB 가 막는가"이므로 영속 계층을 건너뛴다.
        assertThatThrownBy(() -> inTx(() -> jdbc.update("""
                INSERT INTO tbl_user_expend_group
                    (id_key, name, created_at, updated_at, updated_by)
                VALUES (?, ?, now(), now(), ?)
                """, user.getIdKey(), "감사누락", user.getIdKey())))
                .isInstanceOf(DataIntegrityViolationException.class)
                // NOT NULL 위반 메시지는 제약 이름이 아니라 컬럼 이름을 담는다
                .hasMessageContaining("created_by");
    }

    @Test
    @DisplayName("#20 Entity 경로에서는 Hibernate 가 DB 보다 먼저 막는다")
    void entityPathRejectsMissingAuthorBeforeReachingDatabase() {
        User user = inTx(() -> userRepository.save(newUser()));

        // stampAudit 을 부르지 않는다. AuditorAware 가 아직 빈 Optional 을 돌려주는
        // 임시 구현이라 createdBy 가 채워지지 않는다. 두 겹으로 막히는 것이 정상이며,
        // 위 시험이 DB 쪽 겹을, 이 시험이 매핑 쪽 겹을 각각 확인한다.
        assertThatThrownBy(() -> inTx(() -> {
            UserExpendGroup group = new UserExpendGroup();
            group.setUser(user);
            group.setName("감사누락");
            expendGroupRepository.saveAndFlush(group);
        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("createdBy");
    }

    @Test
    @DisplayName("#20 tbl_user 는 created_by 없이도 저장된다 — 가입은 자기 자신을 만드는 행위다")
    void userRowWithoutAuthorIsAccepted() {
        User saved = inTx(() -> {
            User user = new User();
            user.setUserId(TEST_USER_PREFIX + "noauthor");
            user.setPw("$2a$12$0123456789012345678901234567890123456789012345678901");
            user.setNickname("가입직후");
            // createdBy·updatedBy 를 채우지 않는다
            return userRepository.saveAndFlush(user);
        });

        User found = inTx(() -> userRepository.findById(saved.getIdKey()).orElseThrow());

        assertThat(found.getCreatedBy()).isNull();
        assertThat(found.getUpdatedBy()).isNull();
        // 시각은 JPA Auditing 이 채운다 — NULL 이 허용되는 것은 "누가"뿐이다
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }
}

package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Foundational 단계(T006~T011)가 실제로 성립하는지 확인한다.
 *
 * <p>quickstart.md §3의 20개 시나리오에 속하지 않는다. 그것들은 각 User Story의
 * 테스트가 맡는다. 여기서 보는 것은 <b>기반이 쓸 수 있는 상태인가</b>다 —
 * 컨텍스트가 뜨는가, {@code tbl_user}가 실제로 만들어졌는가, 대리키·감사 컬럼
 * 규칙이 DB에 반영됐는가.
 */
class SchemaFoundationIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        cleanUpUsers();
    }

    @Test
    @DisplayName("tbl_user 가 생성되고 기본키 이름이 id_key 다")
    void userTableUsesIdKeyAsPrimaryKey() {
        Map<String, Object> pk = jdbc.queryForMap("""
                SELECT c.relname AS table_name, a.attname AS pk_column
                  FROM pg_index i
                  JOIN pg_class c ON c.oid = i.indrelid
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                  JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
                 WHERE i.indisprimary AND n.nspname = 'moneylog' AND c.relname = 'tbl_user'
                """);

        assertThat(pk.get("pk_column")).isEqualTo("id_key");
    }

    @Test
    @DisplayName("tbl_user 가 감사 컬럼 4종을 갖고, created_by·updated_by 는 NULL 을 허용한다")
    void userTableHasNullableAuditorColumns() {
        Map<String, String> nullability = jdbc.query("""
                SELECT column_name, is_nullable
                  FROM information_schema.columns
                 WHERE table_schema = 'moneylog' AND table_name = 'tbl_user'
                   AND column_name IN ('created_at', 'updated_at', 'created_by', 'updated_by')
                """, rs -> {
            Map<String, String> result = new java.util.HashMap<>();
            while (rs.next()) {
                result.put(rs.getString("column_name"), rs.getString("is_nullable"));
            }
            return result;
        });

        assertThat(nullability).hasSize(4);
        assertThat(nullability.get("created_at")).isEqualTo("NO");
        assertThat(nullability.get("updated_at")).isEqualTo("NO");
        // 회원가입은 자기 자신을 만드는 행위라 INSERT 시점에 id_key 가 없다.
        assertThat(nullability.get("created_by")).isEqualTo("YES");
        assertThat(nullability.get("updated_by")).isEqualTo("YES");
    }

    @Test
    @DisplayName("회원 1건이 저장되고 user_id 로 다시 읽힌다")
    void savesAndReadsBackAUser() {
        User saved = inTx(() -> userRepository.save(newUser()));

        assertThat(saved.getIdKey()).isNotNull();

        User found = inTx(() -> userRepository.findByUserId(saved.getUserId()).orElseThrow());

        assertThat(found.getIdKey()).isEqualTo(saved.getIdKey());
        assertThat(found.getNickname()).isEqualTo("테스트회원");
        // 가입 기본값
        assertThat(found.getRole()).isEqualTo((short) 3);
        assertThat(found.getActive()).isTrue();
        // 감사 시각은 JPA Auditing 이 채운다
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 아이디는 중복 검사에 걸리지 않는다")
    void existsByUserIdIsFalseForUnknownId() {
        assertThat(inTx(() -> userRepository.existsByUserId(TEST_USER_PREFIX + "no_such_user")))
                .isFalse();
    }
}

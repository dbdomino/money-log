package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dbdomino.moneylog.data.entity.BaseAuditEntity;
import com.dbdomino.moneylog.data.entity.User;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 스키마 검증 통합 테스트 기반.
 *
 * <p>여기 있는 테스트들은 <b>애플리케이션 로직이 아니라 DB 제약</b>을 검사한다.
 * "유일 제약이 실제로 두 번째 INSERT를 막는가", "FK CASCADE가 자식을 지우는가"처럼
 * 저장 구조 자체가 요구사항을 강제하는지 확인하는 것이 목적이다.
 *
 * <p>실제 PostgreSQL에 붙는다. 인메모리 DB로 바꾸면 부분 유니크 인덱스·CHECK 같은
 * PostgreSQL 고유 동작을 검증할 수 없어 시험의 의미가 사라진다.
 *
 * <p><b>트랜잭션을 클래스에 걸지 않는다.</b> 제약 위반은 flush 시점에 터지고,
 * 위반 이후 트랜잭션은 롤백만 가능해진다. 각 검사를 독립된 트랜잭션으로 돌려야
 * 한 테스트 안에서 여러 시나리오를 이어 볼 수 있다. 대신 각 테스트가 만든 데이터를
 * 직접 지운다({@link #cleanUpUsers()}).
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/quickstart.md">quickstart.md §3</a>
 */
@SpringBootTest
@ActiveProfiles("postgresql")
public abstract class AbstractSchemaIT {

    /**
     * 테스트가 만든 회원을 골라내기 위한 접두사. 정리할 때 이 접두사로 지운다.
     *
     * <p>밑줄을 쓰지 않는다 — SQL {@code LIKE}에서 {@code _}는 한 글자 와일드카드라
     * {@code "it_%"}로 지우면 의도치 않은 아이디까지 걸린다.
     */
    protected static final String TEST_USER_PREFIX = "ittest";

    @Autowired
    protected TransactionTemplate tx;

    @Autowired
    protected JdbcTemplate jdbc;

    /**
     * 저장 가능한 회원 1건을 만들어 돌려준다(아직 저장하지 않는다).
     *
     * <p>아이디는 접두사 + 무작위 문자열이라 테스트끼리 부딪히지 않는다.
     */
    protected User newUser() {
        return newUser(TEST_USER_PREFIX + UUID.randomUUID().toString().substring(0, 8));
    }

    /** 아이디를 지정해 회원 1건을 만든다. 유일 제약 검사에 쓴다. */
    protected User newUser(String userId) {
        User user = new User();
        user.setUserId(userId);
        user.setPw("$2a$12$0123456789012345678901234567890123456789012345678901");
        user.setNickname("테스트회원");
        stampAudit(user);
        return user;
    }

    /**
     * 감사 컬럼을 직접 채운다.
     *
     * <p>{@code AuditorAware}가 아직 임시 구현(빈 {@code Optional})이라
     * {@code created_by}/{@code updated_by}가 자동으로 채워지지 않는다. 회원 외의
     * 저장 단위는 두 컬럼이 NOT NULL이므로 테스트가 값을 넣어야 저장된다.
     *
     * <p>백엔드 Phase 1에서 {@code AuditorAware}가 실제 {@code id_key}를 공급하게
     * 되면 이 헬퍼의 {@code createdBy}/{@code updatedBy} 부분은 필요 없어진다.
     */
    protected void stampAudit(BaseAuditEntity entity) {
        stampAudit(entity, PLACEHOLDER_AUDITOR_ID_KEY);
    }

    /**
     * 임시 감사자 {@code id_key}. 실재하지 않는 값이며, 감사 컬럼에 FK를 걸지 않기로 한
     * 덕에 통과한다. {@code AuditorAware}가 실제 값을 공급하게 되는 Phase 1에서
     * {@code stampAudit}과 함께 사라진다.
     */
    protected static final Long PLACEHOLDER_AUDITOR_ID_KEY = 0L;

    /** 감사 컬럼을 지정한 {@code id_key}로 채운다. */
    protected void stampAudit(BaseAuditEntity entity, Long auditorIdKey) {
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(auditorIdKey);
        entity.setUpdatedBy(auditorIdKey);
    }

    /** 한 트랜잭션에서 실행하고 결과를 돌려준다. 예외는 그대로 올라온다. */
    protected <T> T inTx(Supplier<T> work) {
        return tx.execute(status -> work.get());
    }

    /** 한 트랜잭션에서 실행한다. */
    protected void inTx(Runnable work) {
        tx.executeWithoutResult(status -> work.run());
    }

    /**
     * 주어진 작업이 DB 제약에 막히는지 확인한다.
     *
     * <p>유일 제약·CHECK·NOT NULL·FK 위반은 모두
     * {@link DataIntegrityViolationException}으로 올라온다.
     */
    protected void assertViolatesConstraint(Runnable work) {
        assertThatThrownBy(() -> inTx(work))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * <b>지정한 이름의</b> 제약에 막히는지까지 확인한다.
     *
     * <p>이름 없는 버전만 쓰면 검증력이 샌다 — 확인하려던 제약이 아예 만들어지지
     * 않았어도 다른 제약(예: {@code created_by} NOT NULL)이 먼저 터지면 테스트가
     * 초록으로 통과한다. 이 테스트들의 존재 이유가 "그 제약이 실제로 강제되는가"이므로,
     * 이름이 정해진 검사는 이 쪽을 쓴다.
     */
    protected void assertViolatesConstraint(Runnable work, String constraintName) {
        assertThatThrownBy(() -> inTx(work))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(constraintName);
    }

    /**
     * 테스트가 만든 데이터를 정리한다.
     *
     * <p>하위 클래스에서도 자동 실행된다 — JUnit 5가 상위 클래스의
     * {@code @AfterEach}를 함께 돌리므로, 새 IT 클래스가 정리를 빠뜨리는 일 자체가
     * 불가능해진다. 빠뜨리면 남은 데이터가 다음 실행의 유일 제약 검사를 오염시키는데
     * 그 사실이 바로 드러나지 않아 원인을 찾기 어렵다.
     */
    @AfterEach
    protected void tearDownTestData() {
        cleanUpUsers();
    }

    /**
     * 테스트가 만든 회원과 그에 딸린 데이터를 지운다.
     *
     * <p>자식 테이블이 늘어나면 {@link #deleteChildRows()}의 삭제 순서를 함께 늘린다
     * — FK RESTRICT라 자식을 먼저 지워야 한다.
     */
    protected void cleanUpUsers() {
        // 반드시 트랜잭션 안에서 지운다. datasource 가 auto-commit=false 라
        // 트랜잭션 밖의 JdbcTemplate 갱신은 커밋되지 않고 조용히 사라진다.
        inTx(() -> {
            deleteChildRows();
            jdbc.update("DELETE FROM tbl_user WHERE user_id LIKE ?", TEST_USER_PREFIX + "%");
        });
    }

    /**
     * 회원에 딸린 자식 행을 지운다.
     *
     * <p>자식 저장 단위가 생길 때마다 여기 삭제문을 늘린다. FK가 RESTRICT라
     * <b>자식을 먼저</b> 지워야 회원 삭제가 통과한다. 삭제 순서는 참조 방향의
     * 역순이다.
     *
     * <p>US2~US5의 수단·지출유형·지출·소득·고정지출·목표금액·통계가 생기면 여기에
     * 이어 붙인다.
     */
    protected void deleteChildRows() {
        deleteByOwner("tbl_user_session");
        deleteByOwner("tbl_user_login_history");
        // 기준 데이터는 거래·고정지출·목표·통계가 참조하므로 그것들보다 나중에 지운다.
        deleteByOwner("tbl_user_payment_method");
        deleteByOwner("tbl_user_expend_group");
    }

    /**
     * 소유자 기준으로 지울 수 있는 자식 테이블. 여기 없는 이름은 받지 않는다.
     *
     * <p>테이블명은 바인딩할 수 없어 SQL에 문자열로 이어 붙일 수밖에 없다. 허용 목록으로
     * 좁혀 두면 오타·오용이 들어올 자리가 없어진다. 자식 저장 단위가 늘어날 때
     * {@link #deleteChildRows()}와 함께 이 목록도 갱신한다.
     */
    private static final Set<String> OWNED_TABLES = Set.of(
            "tbl_user_session",
            "tbl_user_login_history",
            "tbl_user_payment_method",
            "tbl_user_expend_group");

    /** 테스트 회원이 소유한 행을 그 테이블에서 지운다. */
    protected void deleteByOwner(String tableName) {
        if (!OWNED_TABLES.contains(tableName)) {
            throw new IllegalArgumentException("허용되지 않은 테이블: " + tableName);
        }
        jdbc.update("DELETE FROM " + tableName + " WHERE id_key IN "
                + "(SELECT id_key FROM tbl_user WHERE user_id LIKE ?)", TEST_USER_PREFIX + "%");
    }
}

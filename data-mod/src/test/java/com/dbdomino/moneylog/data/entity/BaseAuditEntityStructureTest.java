package com.dbdomino.moneylog.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 감사 컬럼을 밖에서 쓸 수 없는가.
 *
 * <p>quickstart.md §3 의 시나리오 #40 에 대응한다.
 *
 * <p>Spring 컨텍스트를 띄우지 않는다 — 검사 대상이 <b>클래스의 모양</b>이라 DB 도 빈도
 * 필요 없다.
 *
 * <p>이 검사가 있는 이유: {@code @Setter} 를 지운 것은 되돌리기 쉬운 변경이다. Lombok
 * 애너테이션 한 줄이면 네 개의 공개 세터가 다시 생기고, 그 뒤로는 아무 코드나
 * {@code created_by} 를 위조할 수 있다. 그때 이 테스트가 깨져서 알려 준다.
 */
class BaseAuditEntityStructureTest {

    private static final List<String> AUDIT_SETTERS =
            List.of("setCreatedAt", "setUpdatedAt", "setCreatedBy", "setUpdatedBy");

    @Test
    @DisplayName("#40 BaseAuditEntity 에 감사 값 세터가 없다 — AuditingEntityListener 만 채운다")
    void auditFieldsHaveNoPublicSetters() {
        List<String> found = Arrays.stream(BaseAuditEntity.class.getMethods())
                .map(Method::getName)
                .filter(AUDIT_SETTERS::contains)
                .toList();

        assertThat(found)
                .as("감사 값은 리스너만 채운다. 세터가 열리면 created_by 를 위조할 수 있다")
                .isEmpty();
    }

    @Test
    @DisplayName("#41 AbstractSchemaIT 에 stampAudit 이 없다 — 테스트도 감사 값을 손으로 채우지 않는다")
    void schemaTestBaseNoLongerStampsAuditColumns() {
        List<String> stampers = Arrays.stream(
                        com.dbdomino.moneylog.data.schema.AbstractSchemaIT.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("stampAudit"))
                .toList();

        // 테스트가 감사 값을 직접 채우면 "리스너가 실제로 채우는가"를 영영 검증하지 못한다.
        assertThat(stampers).isEmpty();
    }

    @Test
    @DisplayName("#40 감사 값 게터는 열려 있다 — 검증·조회에는 필요하다")
    void auditFieldsKeepTheirGetters() {
        List<String> getters = Arrays.stream(BaseAuditEntity.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("getCreated") || name.startsWith("getUpdated"))
                .toList();

        assertThat(getters).contains("getCreatedAt", "getUpdatedAt", "getCreatedBy", "getUpdatedBy");
    }
}

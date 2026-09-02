package com.dbdomino.moneylog.data.config;

import java.util.Set;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Entity 애너테이션으로 표현할 수 없는 스키마 객체를 Hibernate 가 만들게 한다.
 *
 * <p>대상은 셋이다 — <b>부분 유니크 인덱스 2건</b>과 <b>할부 그룹 시퀀스 1건</b>.
 * 나머지(테이블·컬럼·FK·일반 인덱스·조건 없는 UNIQUE·CHECK)는 전부 Entity 애너테이션이
 * 표현하므로 여기 오지 않는다.
 *
 * <h2>왜 애너테이션으로 안 되는가</h2>
 *
 * <p>{@code @Table(uniqueConstraints = ...)}·{@code @Index}에는 <b>{@code WHERE} 절을
 * 붙일 자리가 없다.</b> JPA에도 Hibernate 애너테이션에도 부분 인덱스 개념이 없다.
 * 그런데 이 프로젝트의 유일성 두 건은 조건부다:
 *
 * <ul>
 *   <li>{@code ux_user_email} — 이메일은 선택 항목이라 비어 있는 회원이 여럿일 수
 *       있다. 조건 없는 UNIQUE 를 걸면 두 번째 "이메일 없는 가입"이 막힌다(FR-012).</li>
 *   <li>{@code ux_user_session_active} — 폐기된 세션은 행으로 남으므로(FR-016) 조건
 *       없는 UNIQUE 를 걸면 재로그인 자체가 막힌다(FR-017).</li>
 * </ul>
 *
 * <p>시퀀스는 사정이 다르다. {@code @SequenceGenerator} 는 <b>어떤 Entity 의 식별자
 * 생성기로 쓰일 때만</b> DDL 에 나온다. {@code seq_installment_group} 은 한 행이 아니라
 * <b>한 할부의 N개 행이 공유하는</b> 값이라 어느 Entity 의 PK 생성기도 아니다(FR-044).
 * 그래서 모델에 직접 등록한다.
 *
 * <h2>등록 방식</h2>
 *
 * <p>{@code META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor} 로
 * {@link java.util.ServiceLoader} 가 찾는다. Spring 빈이 아니다 — Hibernate 가
 * {@code Metadata} 를 만드는 시점은 애플리케이션 컨텍스트보다 이르다.
 *
 * <p>스키마 이름을 하드코딩하지 않고 {@code default_schema} 설정에서 읽는다. 박아 두면
 * 다른 스키마를 가리켜 띄웠을 때 이 세 객체만 엉뚱한 곳에 생긴다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/contracts/naming-and-constraints.md">contracts §4·§7</a>
 */
public class MoneylogSchemaContributor implements AdditionalMappingContributor {

    /** 할부 그룹 식별자 시퀀스. {@code UserExpense.INSTALLMENT_GROUP_SEQUENCE} 와 같은 이름이다. */
    private static final String INSTALLMENT_GROUP_SEQUENCE = "seq_installment_group";

    /** 모든 방언에 적용한다 — 방언별로 갈라 쓸 이유가 없다. */
    private static final Set<String> ALL_DIALECTS = Set.of();

    /**
     * 스키마 자리표시자. Hibernate 가 <b>DDL 을 내보내는 시점에</b> 실제 스키마 이름으로
     * 바꾼다({@code SimpleAuxiliaryDatabaseObject.injectCatalogAndSchema}).
     *
     * <p>여기서 직접 스키마 이름을 읽어 문자열에 박으면 안 된다 — 이 메서드가 도는
     * 시점에는 기본 네임스페이스가 아직 확정되지 않아 이름이 비고, 그러면 SQL 이
     * 스키마 없이 나가 <b>접속의 {@code search_path} 를 타고 엉뚱한 스키마에</b>
     * 만들어진다. 실제로 그렇게 새는 것을 확인했다(2026-09-02).
     */
    private static final String SCHEMA = "${schema}.";

    @Override
    public String getContributorName() {
        return "moneylog";
    }

    @Override
    public void contribute(AdditionalMappingContributions contributions,
                           InFlightMetadataCollector metadata,
                           ResourceStreamLocator resourceStreamLocator,
                           MetadataBuildingContext buildingContext) {

        // 할부 그룹 식별자 발급용 시퀀스 (FR-044).
        contributions.contributeAuxiliaryDatabaseObject(databaseObject(
                "CREATE SEQUENCE IF NOT EXISTS " + SCHEMA + INSTALLMENT_GROUP_SEQUENCE,
                "DROP SEQUENCE IF EXISTS " + SCHEMA + INSTALLMENT_GROUP_SEQUENCE));

        // 이메일은 "값이 있을 때만" 유일하다 (FR-012).
        contributions.contributeAuxiliaryDatabaseObject(partialUniqueIndex(
                "ux_user_email",
                "tbl_user",
                "(email) WHERE email IS NOT NULL"));

        // 회원당 폐기되지 않은 세션은 동시에 1건뿐이다 (FR-017).
        contributions.contributeAuxiliaryDatabaseObject(partialUniqueIndex(
                "ux_user_session_active",
                "tbl_user_session",
                "(id_key) WHERE revoked = false"));
    }

    /**
     * 부분 유니크 인덱스 하나를 만든다.
     *
     * <p>{@code IF NOT EXISTS} 를 쓴다. {@code ddl-auto: update} 는 기동할 때마다 보조
     * 객체의 생성문을 <b>다시 낸다</b> — 테이블과 달리 존재 여부를 대조하지 않는다.
     * 이게 없으면 두 번째 기동부터 "이미 있음" 오류가 로그를 채운다.
     */
    private SimpleAuxiliaryDatabaseObject partialUniqueIndex(String indexName,
                                                             String table,
                                                             String columnsAndPredicate) {
        return databaseObject(
                "CREATE UNIQUE INDEX IF NOT EXISTS " + indexName
                        + " ON " + SCHEMA + table + " " + columnsAndPredicate,
                "DROP INDEX IF EXISTS " + SCHEMA + indexName);
    }

    private SimpleAuxiliaryDatabaseObject databaseObject(String create, String drop) {
        return new SimpleAuxiliaryDatabaseObject(ALL_DIALECTS, null, null,
                new String[] {create}, new String[] {drop});
    }
}

package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * 지출·소득 수단 — {@code tbl_user_payment_method}.
 *
 * <p>지출용과 소득용을 한 테이블에 담고 {@code purpose}로 가른다. 한 수단은
 * {@code EXPENSE} 또는 {@code INCOME} 중 <b>한쪽만</b> 갖는다(FR-033). 등록 후
 * 용도 변경은 그 수단을 참조하는 지출·소득·고정지출이 한 건도 없을 때만 허용하는데,
 * 이 검사는 DB가 아니라 애플리케이션의 몫이다.
 *
 * <p><b>삭제해도 행을 지우지 않는다</b>(FR-031). 과거 지출·소득 내역이 등록 당시
 * 수단 이름을 보존하고 있긴 하지만, 수단 자체를 지우면 그 내역의 참조가 끊긴다.
 *
 * <p>{@code inUse}와 {@code deleted}는 다른 항목이다 — 앞은 입력 화면에 노출할지,
 * 뒤는 관리 목록에서 삭제 상태인지를 뜻한다. "사용 중인 수단 목록"(2.6)은
 * 용도가 일치하고 두 조건을 모두 만족하는 것만 고른다(FR-032).
 *
 * <p>{@code type}·{@code purpose}를 자바 열거형이 아니라 문자열로 둔다. Hibernate 6은
 * {@code @Enumerated(STRING)} 컬럼에 CHECK 제약을 자동 생성하는데, 이 프로젝트는
 * CHECK을 {@code sql/04_constraints.sql}이 이름까지 정해 관리하기로 했다
 * (contracts/naming-and-constraints.md §7). 두 곳이 같은 제약을 중복해 만들지 않도록
 * 값 검증은 스크립트 한 곳에 둔다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md §4</a>
 */
@Entity
@Table(
        name = "tbl_user_payment_method",
        indexes = @Index(
                name = "ix_user_payment_method_active",
                columnList = "id_key, purpose, in_use, deleted"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserPaymentMethod extends BaseAuditEntity {

    /** {@code type} 허용 값 — 카드. */
    public static final String TYPE_CARD = "CARD";
    /** {@code type} 허용 값 — 계좌. */
    public static final String TYPE_ACCOUNT = "ACCOUNT";

    /** {@code purpose} 허용 값 — 지출용. */
    public static final String PURPOSE_EXPENSE = "EXPENSE";
    /** {@code purpose} 허용 값 — 소득용. */
    public static final String PURPOSE_INCOME = "INCOME";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    /** 소유 회원. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_key",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_payment_method_user")
    )
    private User user;

    /** 수단 이름 (예: 국민카드, 월급통장). 회원 안에서 중복을 막지 않는다. */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 종류 — {@code CARD} 또는 {@code ACCOUNT}. */
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    /** 용도 — {@code EXPENSE} 또는 {@code INCOME}. 한 수단은 한쪽만 갖는다. */
    @Column(name = "purpose", nullable = false, length = 10)
    private String purpose;

    /** 사용 여부. 지출·소득 입력 화면에 이 수단을 띄울지 여부다. */
    @Column(name = "in_use", nullable = false)
    @ColumnDefault("true")
    private Boolean inUse = true;

    /**
     * 카드 유효기간 {@code YYYY-MM}. {@code type=ACCOUNT}면 비어 있다.
     *
     * <p>타입이 {@code CHAR(7)}인 것은 data-model.md §4가 정한 값이다. 고정 길이라
     * <b>7자보다 짧은 값을 넣으면 읽을 때 공백이 붙어 돌아온다.</b> {@code YYYY-MM}은
     * 항상 정확히 7자라 현재는 문제가 없지만, 형식이 어긋난 값이 들어오면 비교가
     * 조용히 실패한다. 형식 검증을 붙일 때는 다른 CHECK와 같이
     * {@code sql/04_constraints.sql}에 이름을 붙여 관리한다.
     */
    @Column(name = "card_expiry", columnDefinition = "char(7)")
    private String cardExpiry;

    /** 삭제 표시. 삭제해도 행은 남는다. */
    @Column(name = "deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean deleted = false;
}

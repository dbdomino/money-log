package com.dbdomino.moneylog.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 저장 단위가 공유하는 감사 항목 4종.
 *
 * <p>{@code created_by}/{@code updated_by}는 그 행을 만들거나 고친 회원의
 * {@code id_key}이며, 소유자 {@code id_key}와 다를 수 있다 — 관리자가 다른 회원을
 * 추가·수정하는 경로가 그렇다.
 *
 * <p>두 컬럼에 FK를 걸지 않는다. 감사 기록은 대상 회원의 존재와 무관하게 남아야
 * 하고, {@code tbl_user}가 자기 컬럼으로 자신을 참조하는 순환을 피한다.
 *
 * <p>기본키는 여기에 두지 않는다. {@code tbl_user}만 기본키 이름이 {@code id_key}이고
 * 나머지는 {@code idx}라서, PK는 각 Entity가 직접 선언한다.
 *
 * <p>{@code created_by}가 NOT NULL이면 곤란한 곳은 {@code tbl_user} 하나뿐이다
 * (회원가입은 자기 자신을 만드는 행위라 INSERT 시점에 {@code id_key}가 없다).
 * 그 Entity가 {@code @AttributeOverride}로 nullable하게 재정의한다.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/spec.md">spec.md FR-004</a>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseAuditEntity {

    /** 행 생성 시각. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 행 최종 수정 시각. 생성 시에도 채워진다. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 이 행을 만든 회원의 {@code id_key}. */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    /** 이 행을 마지막으로 고친 회원의 {@code id_key}. */
    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;
}

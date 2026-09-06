package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpendTargetDefault;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 기본 목표금액 조회 — {@code tbl_expend_target_default}.
 *
 * <p>저장(5.2)이 "있으면 고치고 없으면 만든다"라서 단건 조회가 주 용도다.
 */
public interface UserExpendTargetDefaultRepository
        extends JpaRepository<UserExpendTargetDefault, Long> {

    /** 목표금액 목록(5.1) — 본인 기본 목표 전체. */
    List<UserExpendTargetDefault> findByUserIdKeyOrderByIdxAsc(Long idKey);

    /**
     * upsert 판정용 단건 조회. 유일 제약 {@code ux_target_default}와 같은 조합이다.
     *
     * <p>비어 있으면 INSERT, 있으면 그 행을 UPDATE한다. 조회 조건이 유일 제약과
     * 어긋나면 "없다"고 판단한 뒤 INSERT가 제약에 막힌다.
     */
    Optional<UserExpendTargetDefault> findByUserIdKeyAndExpendGroupIdx(
            Long idKey, Long expendGroupIdx);

    /**
     * 기본 목표 upsert — <b>있으면 갱신하고 없으면 만든다</b>(FR-512).
     *
     * <p>네이티브 쿼리인 이유는 {@code ON CONFLICT DO UPDATE}가 JPQL에 없기 때문이다.
     * "조회해서 없으면 INSERT"로는 부족하다 — 두 요청이 같은 순간에 "없음"을 보면
     * 하나가 {@code ux_target_default} 위반으로 실패하고 사용자 화면이 죽는다. 충돌을
     * <b>정상 흐름으로</b> 흡수하려면 DB 쪽 구문이 필요하다. 005의
     * {@code insertIfAbsent}와 같은 처방이며, 그쪽이 {@code DO NOTHING}인 것과 달리
     * 여기는 {@code DO UPDATE}다 — 목표금액은 뒤에 온 값이 이겨야 한다.
     *
     * <p><b>충돌 대상을 제약 이름으로 적는다.</b> 제약 이름이
     * {@code ux_target_default}로 테이블 이름({@code tbl_expend_target_default})과
     * 달라 컬럼 목록으로 적으면 찾기 어렵다.
     *
     * <p>감사 컬럼을 직접 채운다. {@code AuditingEntityListener}는 Entity를 거칠 때만
     * 동작하는데 이 경로는 Entity를 만들지 않는다. 갱신에서는 {@code created_*}를
     * 건드리지 않는다 — 처음 정한 사람과 시각이 남아야 한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tbl_expend_target_default
                (id_key, expend_group_idx, target_amount,
                 created_at, updated_at, created_by, updated_by)
            VALUES
                (:idKey, :expendGroupIdx, :targetAmount,
                 now(), now(), :auditorIdKey, :auditorIdKey)
            ON CONFLICT ON CONSTRAINT ux_target_default DO UPDATE
               SET target_amount = EXCLUDED.target_amount,
                   updated_at = now(),
                   updated_by = EXCLUDED.updated_by
            """, nativeQuery = true)
    int upsert(@Param("idKey") Long idKey,
               @Param("expendGroupIdx") Long expendGroupIdx,
               @Param("targetAmount") Long targetAmount,
               @Param("auditorIdKey") Long auditorIdKey);
}

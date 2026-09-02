package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpendTargetDefault;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 기본 목표금액 조회 — {@code tbl_user_expend_target_default}.
 *
 * <p>저장(5.2)이 "있으면 고치고 없으면 만든다"라서 단건 조회가 주 용도다.
 */
public interface UserExpendTargetDefaultRepository
        extends JpaRepository<UserExpendTargetDefault, Long> {

    /** 목표금액 목록(5.1) — 본인 기본 목표 전체. */
    List<UserExpendTargetDefault> findByUserIdKeyOrderByIdxAsc(Long idKey);

    /**
     * upsert 판정용 단건 조회. 유일 제약 {@code ux_user_target_default}와 같은 조합이다.
     *
     * <p>비어 있으면 INSERT, 있으면 그 행을 UPDATE한다. 조회 조건이 유일 제약과
     * 어긋나면 "없다"고 판단한 뒤 INSERT가 제약에 막힌다.
     */
    Optional<UserExpendTargetDefault> findByUserIdKeyAndExpendGroupIdx(
            Long idKey, Long expendGroupIdx);
}

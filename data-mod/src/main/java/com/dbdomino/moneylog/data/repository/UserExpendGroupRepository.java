package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지출유형 조회 — {@code tbl_user_expend_group}.
 *
 * <p>이름 중복 검사({@code 3101})와 사용 중 목록(2.13)이 주 용도다.
 */
public interface UserExpendGroupRepository extends JpaRepository<UserExpendGroup, Long> {

    /** 관리 목록(2.8) — 본인 유형 전체. 삭제 표시된 것도 포함한다. */
    List<UserExpendGroup> findByUserIdKeyOrderByIdxAsc(Long idKey);

    /** 사용 중 목록(2.13) — 사용 중이고 삭제되지 않은 것만. */
    List<UserExpendGroup> findByUserIdKeyAndInUseTrueAndDeletedFalse(Long idKey);

    /**
     * 이름 중복 검사({@code 3101}).
     *
     * <p>삭제 표시된 유형의 이름도 여전히 점유 상태다 — 유일 제약이 삭제분을 포함하기
     * 때문이다. 이름을 재사용하면 아이콘 파일이 충돌한다.
     */
    boolean existsByUserIdKeyAndName(Long idKey, String name);

    /** 소유자 확인을 겸한 단건 조회. */
    Optional<UserExpendGroup> findByIdxAndUserIdKey(Long idx, Long idKey);
}

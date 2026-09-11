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
    List<UserExpendGroup> findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(Long idKey);

    /**
     * 목표금액 목록(006 의 5.1) — <b>{@code in_use} 만 본다</b>.
     *
     * <p>바로 위의 2.13 용 메서드와 달리 {@code deleted} 를 조건에 넣지 <b>않는다</b>.
     * FR-509 가 정한 모집단이 "사용 중 유형"뿐이고, 삭제 표시는 목표금액에서 조건이
     * 아니기 때문이다(target-amount.md §5) — 삭제 표시된 유형의 목표 행은 유지되며
     * (FR-511) 그 유형이 {@code in_use=true} 인 채로 삭제 표시만 됐다면 목록에 나온다.
     *
     * <p>둘을 하나로 합치지 않는 것은 <b>2.13 의 모집단을 바꾸면 003 의 계약이 움직이기</b>
     * 때문이다.
     */
    List<UserExpendGroup> findByUserIdKeyAndInUseTrueOrderByIdxAsc(Long idKey);

    /**
     * 이름 중복 검사({@code 3101}).
     *
     * <p>삭제 표시된 유형의 이름도 여전히 점유 상태다 — 유일 제약이 삭제분을 포함하기
     * 때문이다. 이름을 재사용하면 아이콘 파일이 충돌한다.
     */
    boolean existsByUserIdKeyAndName(Long idKey, String name);

    /**
     * 엑셀 업로드(004 의 3.12)가 <b>이름으로</b> 지출유형을 찾는다.
     *
     * <p>양식이 드롭다운으로 이름을 넣게 하므로 파일에 ID 가 없다. 못 찾으면 그 행을
     * 오류로 본다({@code 3502}).
     *
     * <p><b>{@code Optional} 이다.</b> {@code ux_user_expend_group_name (id_key, name)} 이
     * 회원 안에서 이름을 하나로 강제한다 — 수단 쪽이 {@code List} 인 것과 다른 이유가
     * 그 제약의 유무다.
     *
     * <p>위 {@link #existsByUserIdKeyAndName} 로는 대신할 수 없다. {@code boolean} 이라
     * <b>Entity 를 가져올 수 없어</b> FK 와 이름 스냅샷을 채우지 못한다.
     *
     * <p>사용 중인 것만 돌려준다(004 FR-325). 그래서 이름 유일 제약이 삭제분을 포함해도
     * 결과는 최대 1건이다.
     */
    Optional<UserExpendGroup> findByUserIdKeyAndNameAndInUseTrueAndDeletedFalse(
            Long idKey, String name);

    /** 소유자 확인을 겸한 단건 조회. */
    Optional<UserExpendGroup> findByIdxAndUserIdKey(Long idx, Long idKey);
}

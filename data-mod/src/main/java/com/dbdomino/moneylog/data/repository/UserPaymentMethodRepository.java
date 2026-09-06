package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 지출·소득 수단 조회 — {@code tbl_user_payment_method}.
 *
 * <p>조회 축이 둘이다. 관리 목록(2.2)은 삭제 표시된 것까지 전부 보여주고, 입력
 * 화면용 목록(2.6)은 용도가 맞고 사용 중이며 삭제되지 않은 것만 고른다.
 */
public interface UserPaymentMethodRepository extends JpaRepository<UserPaymentMethod, Long> {

    /** 관리 목록(2.2) — 본인 수단 전체. 삭제 표시된 것도 포함한다. */
    List<UserPaymentMethod> findByUserIdKeyOrderByIdxAsc(Long idKey);

    /**
     * 입력 화면용 목록(2.6) — 용도가 맞고 사용 중이며 삭제되지 않은 것만.
     *
     * <p>세 조건을 모두 걸어야 한다(FR-032). {@code ix_user_payment_method_active}가
     * 이 조합을 그대로 덮는다.
     */
    List<UserPaymentMethod> findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalseOrderByIdxAsc(
            Long idKey, String purpose);

    /**
     * 엑셀 양식(004 의 3.11)의 수단 드롭다운 — <b>용도 구분 없이</b> 사용 중 전부.
     *
     * <p>위 두 메서드로는 이 목록을 만들 수 없다. 사용 중 목록은 {@code purpose} 를
     * <b>필수로</b> 받는데 양식의 A열이 지출·소득을 모두 담아 한쪽만 골라서는 안 되고,
     * 관리 목록은 <b>삭제 표시된 수단까지</b> 돌려줘 "사용 중만"(004 FR-317)을 어긴다.
     *
     * <p>지출유형 쪽은 {@code UserExpendGroupRepository} 의 사용 중 목록이 이미 같은
     * 조건이라 그대로 쓴다 — 그쪽에는 용도 구분이 없기 때문이다.
     */
    List<UserPaymentMethod> findByUserIdKeyAndInUseTrueAndDeletedFalseOrderByIdxAsc(Long idKey);

    /**
     * 엑셀 업로드(004 의 3.12)가 <b>이름으로</b> 수단을 찾는다.
     *
     * <p>양식이 드롭다운으로 이름을 넣게 하므로 파일에 ID 가 없다. 못 찾으면 그 행을
     * 오류로 본다({@code 3502}).
     *
     * <p><b>여러 건이 나올 수 있어 {@code List} 다.</b> 이 테이블에는 지출유형의
     * {@code ux_user_expend_group_name} 같은 <b>이름 유니크 제약이 없어</b> 한 회원이
     * "국민카드"를 두 개 만들 수 있다(덤프 확인). 어느 쪽을 고를지는 애플리케이션이
     * 정한다 — 첫 매치로 가든 중복을 오류로 보든 이 시그니처는 바뀌지 않는다.
     *
     * <p>사용 중인 것만 돌려준다. 죽은 수단에 새 지출을 걸 수 없다(004 FR-325).
     */
    List<UserPaymentMethod> findByUserIdKeyAndNameAndInUseTrueAndDeletedFalseOrderByIdxAsc(
            Long idKey, String name);

    /** 소유자 확인을 겸한 단건 조회. 남의 수단을 집어오지 않도록 회원까지 함께 건다. */
    Optional<UserPaymentMethod> findByIdxAndUserIdKey(Long idx, Long idKey);
}

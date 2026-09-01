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
    List<UserPaymentMethod> findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalse(
            Long idKey, String purpose);

    /** 소유자 확인을 겸한 단건 조회. 남의 수단을 집어오지 않도록 회원까지 함께 건다. */
    Optional<UserPaymentMethod> findByIdxAndUserIdKey(Long idx, Long idKey);
}

package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserIncome;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 수입 내역 조회 — {@code tbl_user_income}.
 *
 * <p>할부·지출유형이 없어 조회 축이 기간 하나다. 지출과 구조가 달라 테이블을 나눈
 * 결과가 여기서도 그대로 드러난다(FR-046).
 */
public interface UserIncomeRepository extends JpaRepository<UserIncome, Long> {

    /**
     * 월별 가계부 목록(4.8)·통계 집계(5.5)가 쓰는 기간 조회. 양 끝을 포함한다.
     *
     * <p>지출과 같은 이유로 연·월이 아니라 날짜 범위로 받는다.
     * {@code ix_user_income_date}가 이 조합을 덮는다.
     */
    List<UserIncome> findByUserIdKeyAndPaymentDateBetween(Long idKey, LocalDate from, LocalDate to);

    /** 소유자 확인을 겸한 단건 조회. */
    Optional<UserIncome> findByIdxAndUserIdKey(Long idx, Long idKey);
}

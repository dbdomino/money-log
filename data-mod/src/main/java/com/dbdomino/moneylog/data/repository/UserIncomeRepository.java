package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserIncome;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 수입 내역 조회 — {@code tbl_income}.
 *
 * <p>할부·지출유형이 없어 조회 축이 기간 하나다. 지출과 구조가 달라 테이블을 나눈
 * 결과가 여기서도 그대로 드러난다(FR-046).
 */
public interface UserIncomeRepository extends JpaRepository<UserIncome, Long> {

    /**
     * 월별 가계부 목록(4.8)·통계 집계(5.5)가 쓰는 기간 조회. 양 끝을 포함한다.
     *
     * <p>지출과 같은 이유로 연·월이 아니라 날짜 범위로 받는다.
     * {@code ix_income_date}가 이 조합을 덮는다.
     */
    List<UserIncome> findByUserIdKeyAndPaymentDateBetween(Long idKey, LocalDate from, LocalDate to);

    /** 소유자 확인을 겸한 단건 조회. */
    Optional<UserIncome> findByIdxAndUserIdKey(Long idx, Long idKey);

    /**
     * 이 수단을 쓴 소득이 하나라도 있는가. 수단의 {@code purpose} 변경 판정({@code 3005})이 쓴다.
     *
     * <p><b>{@code count} 가 아니라 {@code exists} 다.</b> 필요한 답이 "있느냐"인데 세면 전
     * 행을 훑는다. 참조 검사는 네 테이블 전부를 봐야 하므로(FR-205) 네 곳에 같은 메서드가 있다.
     */
    boolean existsByPaymentMethodIdx(Long paymentMethodIdx);
}

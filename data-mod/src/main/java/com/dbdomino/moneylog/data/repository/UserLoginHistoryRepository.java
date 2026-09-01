package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserLoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 로그인 이력 조회 — {@code tbl_user_login_history}.
 *
 * <p>이력은 무기한 쌓이므로(FR-019a) 전체를 한 번에 읽는 메서드를 두지 않는다.
 */
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {

    /**
     * 한 회원의 로그인 이력을 최근 순으로 읽는다. {@code ix_user_login_history_at}을 탄다.
     *
     * <p>정리 배치 없이 무한 누적되는 테이블이라 <b>반드시 페이징으로 끊는다</b>.
     * 오래 쓴 회원 한 명의 조회가 힙과 응답 시간을 그대로 끌어올리기 때문이다.
     * {@link Page#getTotalElements()}가 응답 규격의 {@code totalCount}가 된다.
     */
    Page<UserLoginHistory> findByUserIdKeyOrderByLoginAtDesc(Long idKey, Pageable pageable);
}

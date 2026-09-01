package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserLoginHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 로그인 이력 조회 — {@code tbl_user_login_history}.
 *
 * <p>이력은 무기한 쌓이므로(FR-019a) 전체를 한 번에 읽는 메서드를 두지 않는다.
 * 최근 순 조회만 제공하고, 화면이 필요로 하면 페이징을 얹는다.
 */
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {

    /** 한 회원의 로그인 이력을 최근 순으로 읽는다. {@code ix_user_login_history_at}을 탄다. */
    List<UserLoginHistory> findByUserIdKeyOrderByLoginAtDesc(Long idKey);
}

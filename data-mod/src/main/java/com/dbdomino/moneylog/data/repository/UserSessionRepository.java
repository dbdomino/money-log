package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 세션 조회 — {@code tbl_user_session}.
 *
 * <p>조회 축이 둘이다. 인증 필터는 토큰의 {@code sid}로 세션을 찾고, 로그인은
 * 회원의 기존 활성 세션을 찾아 폐기한다.
 *
 * <p>소유자가 {@code User} 연관으로 매핑되어 있어 파생 쿼리 이름이
 * {@code ...UserIdKey...} 형태가 된다 — 회원의 {@code id_key}를 가리킨다.
 */
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /** 토큰의 {@code sid}로 세션을 찾는다. 인증 필터의 DB 대조 진입점. */
    Optional<UserSession> findBySessionId(UUID sessionId);

    /**
     * 회원의 현재 활성 세션을 찾는다.
     *
     * <p>부분 유니크 인덱스({@code ux_user_session_active})가 회원당 1건을 보장하므로
     * 결과는 항상 0건 또는 1건이다. 로그인 시 이 세션을 폐기하고 새로 발급한다.
     */
    Optional<UserSession> findByUserIdKeyAndRevokedFalse(Long idKey);

    /** 회원에게 활성 세션이 있는지만 확인한다. */
    boolean existsByUserIdKeyAndRevokedFalse(Long idKey);
}

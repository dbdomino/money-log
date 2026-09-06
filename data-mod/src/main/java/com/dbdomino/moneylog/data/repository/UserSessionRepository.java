package com.dbdomino.moneylog.data.repository;

import com.dbdomino.moneylog.data.entity.UserSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 토큰의 {@code sid}로 세션을 찾되 <b>회원까지 함께</b> 읽는다.
     *
     * <p>인증 필터가 쓰는 조회다. 소유자가 {@code LAZY}라 필터처럼 트랜잭션 밖에서 도는
     * 코드가 {@code session.getUser()}를 건드리면 {@code LazyInitializationException}이
     * 난다. 검증 9단계가 {@code tbl_user.active}를 봐야 하므로 회원이 반드시 필요하고,
     * 조회를 두 번 하느니 조인 한 번으로 끝낸다.
     */
    @Query("select s from UserSession s join fetch s.user where s.sessionId = :sessionId")
    Optional<UserSession> findBySessionIdWithUser(@Param("sessionId") UUID sessionId);

    /**
     * Refresh Token 해시로 세션을 찾는다(회원 포함). 갱신(1.5) 2단계의 조회다.
     *
     * <p><b>이 조회 하나가 해시 알고리즘을 정한다.</b> 값으로 행을 찾으려면 같은 입력이
     * 항상 같은 해시를 내야 하므로 SHA-256 을 쓴다 — bcrypt 는 salt 때문에 매번 다른
     * 값이 나와 조회 자체가 성립하지 않는다.
     */
    @Query("select s from UserSession s join fetch s.user where s.refreshTokenHash = :hash")
    Optional<UserSession> findByRefreshTokenHashWithUser(@Param("hash") String hash);

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

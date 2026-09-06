package com.dbdomino.moneylog.backend.service;

import com.dbdomino.moneylog.backend.config.JwtProperties;
import com.dbdomino.moneylog.common.security.JwtTokenProvider;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserSession;
import com.dbdomino.moneylog.data.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 생성·조회·갱신·폐기와 토큰 해시 계산.
 *
 * <p><b>해시 계산이 이 클래스 밖으로 나가지 않는다.</b> 알고리즘을 바꿔야 할 때 고칠
 * 자리가 하나여야 하기 때문이다(auth-pipeline.md §5).
 *
 * <h2>해시는 SHA-256 이다</h2>
 *
 * <p>bcrypt 가 아니다. 갱신(1.5) 2단계가 <b>해시로 세션을 조회</b>하는데, bcrypt 는
 * salt 때문에 같은 입력이 매번 다른 값을 내 그 조회가 성립하지 않는다. 토큰은 이미
 * 128비트 이상의 난수라 비밀번호처럼 사전 공격의 대상이 아니므로, 저장 해시의 목적은
 * "DB 가 유출돼도 토큰 원문이 바로 드러나지 않게" 하는 것이고 SHA-256 이 그 목적을
 * 충족한다. 컬럼이 {@code varchar(100)} 이고 SHA-256 hex 는 64자라 여유도 있다.
 *
 * <h2>폐기는 두 가지를 함께 한다</h2>
 *
 * <p>두 해시를 {@code NULL} 로 만들고 {@code revoked} 를 세운다. <b>둘 다 해야 한다</b> —
 * 해시만 비우면 부분 유니크 인덱스({@code ux_user_session_active})가 그 행을 여전히
 * 활성으로 보아 다음 로그인이 유니크 위반으로 막히고, {@code revoked} 만 세우면 해시가
 * 남아 검증 7단계를 통과해 버린다. 행은 삭제하지 않는다(FR-111).
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md</a>
 */
@Service
public class MemberSessionService {

    /** Refresh Token 의 엔트로피(바이트). Base64URL 로 43자가 된다. */
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserSessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public MemberSessionService(UserSessionRepository sessionRepository,
                                JwtTokenProvider jwtTokenProvider,
                                JwtProperties jwtProperties) {
        this.sessionRepository = sessionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 기존 활성 세션을 폐기하고 새 세션 1건을 만든다.
     *
     * <p><b>폐기와 삽입이 한 트랜잭션이다.</b> 폐기만 되고 삽입이 실패하면 회원이
     * 로그아웃된 채로 남는다.
     *
     * <p>동시 로그인 경합은 여기서 막지 않는다. 두 요청이 같은 순간 "활성 세션 없음"을
     * 보면 하나가 부분 유니크 인덱스 위반으로 실패하는데, 그 재시도는 트랜잭션 밖에서
     * 해야 하므로 호출자({@code AuthService})의 몫이다 — 이미 롤백 표시된 트랜잭션
     * 안에서는 다시 시도해도 커밋되지 않는다.
     */
    @Transactional
    public IssuedTokens issue(User user) {
        revokeActiveSession(user.getIdKey());

        UUID sessionId = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getRole(), sessionId, issuedAt);
        String refreshToken = generateRefreshToken();

        OffsetDateTime accessExpiresAt = toOffset(
                issuedAt.plusSeconds(jwtProperties.getAccessTokenValiditySeconds()));
        OffsetDateTime refreshExpiresAt = toOffset(
                issuedAt.plusSeconds(jwtProperties.getRefreshTokenValiditySeconds()));

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(sessionId);
        session.setAccessTokenHash(hash(accessToken));
        session.setRefreshTokenHash(hash(refreshToken));
        session.setAccessExpiresAt(accessExpiresAt);
        session.setRefreshExpiresAt(refreshExpiresAt);
        // 감사 컬럼을 직접 채운다. 로그인은 인증 "이전"이라 SecurityContext 가 비어 있어
        // AuditorAware 가 값을 주지 못하는데 이 테이블의 두 컬럼은 NOT NULL 이다
        // (nullable 예외는 tbl_user 하나뿐이다). 넣을 값은 세션 주인의 id_key 다.
        session.setCreatedBy(user.getIdKey());
        session.setUpdatedBy(user.getIdKey());
        sessionRepository.saveAndFlush(session);

        return new IssuedTokens(accessToken, refreshToken, sessionId,
                accessExpiresAt, refreshExpiresAt);
    }

    /**
     * 같은 세션 행의 토큰을 새로 발급하고 해시·만료를 갱신한다(Rotation).
     *
     * <p><b>{@code session_id} 는 바꾸지 않는다.</b> 새 행을 만들면 부분 유니크 인덱스에
     * 걸리고, 폐기 후 삽입하면 "같은 로그인 세션"이라는 의미가 끊긴다.
     *
     * <p>Access 와 Refresh 를 <b>둘 다</b> 새로 낸다(FR-113). Refresh 를 그대로 두면
     * 7일 내내 같은 값이 살아 있어 탈취됐을 때의 창이 길어진다.
     */
    @Transactional
    public IssuedTokens rotate(UserSession session) {
        User user = session.getUser();
        Instant issuedAt = Instant.now();
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getRole(), session.getSessionId(), issuedAt);
        String refreshToken = generateRefreshToken();

        OffsetDateTime accessExpiresAt = toOffset(
                issuedAt.plusSeconds(jwtProperties.getAccessTokenValiditySeconds()));
        OffsetDateTime refreshExpiresAt = toOffset(
                issuedAt.plusSeconds(jwtProperties.getRefreshTokenValiditySeconds()));

        UserSession managed = sessionRepository.findById(session.getIdx()).orElseThrow();
        managed.setAccessTokenHash(hash(accessToken));
        managed.setRefreshTokenHash(hash(refreshToken));
        managed.setAccessExpiresAt(accessExpiresAt);
        managed.setRefreshExpiresAt(refreshExpiresAt);
        stampUpdater(managed);
        sessionRepository.saveAndFlush(managed);

        return new IssuedTokens(accessToken, refreshToken, managed.getSessionId(),
                accessExpiresAt, refreshExpiresAt);
    }

    /** 세션 1건을 폐기한다. 로그아웃(1.6)·비밀번호 재설정(1.11)·관리자 정지(1.16)가 부른다. */
    @Transactional
    public void revoke(UserSession session) {
        // 엔티티의 revoke() 를 쓴다 — 두 해시 비우기와 revoked 세우기를 한 묶음으로 두어
        // 한쪽만 하는 실수를 구조로 막아 둔 메서드다. 세터는 일부러 닫혀 있다.
        UserSession managed = sessionRepository.findById(session.getIdx()).orElseThrow();
        managed.revoke();
        stampUpdater(managed);
        sessionRepository.saveAndFlush(managed);
    }

    /**
     * {@code updated_by} 의 기본값을 세션 주인으로 채운다.
     *
     * <p>세션을 고치는 경로 중 <b>로그인·갱신은 인증 이전</b>이라 {@code AuditorAware} 가
     * 빈 값을 돌려주는데 이 컬럼은 NOT NULL 이다. 인증된 요청(로그아웃·관리자 정지)에서는
     * 감사 리스너가 실제 행위자의 {@code id_key} 로 이 값을 덮어쓰므로, 여기서 넣는 값은
     * "행위자를 알 수 없을 때의 바닥값" 역할만 한다 — 관리자가 남의 세션을 폐기한 기록이
     * 회원 본인으로 둔갑하지 않는다.
     */
    private static void stampUpdater(UserSession session) {
        session.setUpdatedBy(session.getUser().getIdKey());
    }

    /** 회원의 활성 세션이 있으면 폐기한다. 없으면 아무 일도 하지 않는다. */
    @Transactional
    public void revokeActiveSession(Long idKey) {
        sessionRepository.findByUserIdKeyAndRevokedFalse(idKey).ifPresent(this::revoke);
    }

    /** 토큰의 {@code sid} 로 세션을 찾는다(회원 포함). 인증 필터가 쓴다. */
    @Transactional(readOnly = true)
    public Optional<UserSession> findBySessionId(UUID sessionId) {
        return sessionRepository.findBySessionIdWithUser(sessionId);
    }

    /** Refresh Token 원문으로 세션을 찾는다(회원 포함). 해시로 조회한다. */
    @Transactional(readOnly = true)
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return sessionRepository.findByRefreshTokenHashWithUser(hash(refreshToken));
    }

    /**
     * 토큰 원문의 저장용 해시. SHA-256 hex 소문자 64자.
     *
     * <p>알고리즘을 바꿀 자리는 여기 하나다.
     */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JVM 이 반드시 제공한다. 여기 오면 런타임이 깨진 것이다.
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
    }

    /**
     * Refresh Token 을 만든다. JWT 가 아닌 <b>불투명 랜덤 문자열</b>이다.
     *
     * <p>판정이 전적으로 DB 대조라 자기 완결 토큰으로 만들 이유가 없다. JWT 로 만들면
     * "서명만 맞으면 유효해 보이는" 오해를 부른다.
     */
    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static OffsetDateTime toOffset(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    /**
     * 발급된 토큰 한 벌.
     *
     * @param accessToken      JWT
     * @param refreshToken     불투명 랜덤 문자열
     * @param sessionId        세션 식별자. Rotation 에서도 바뀌지 않는다
     * @param accessExpiresAt  Access 만료 시각
     * @param refreshExpiresAt Refresh 만료 시각
     */
    public record IssuedTokens(String accessToken, String refreshToken, UUID sessionId,
                               OffsetDateTime accessExpiresAt, OffsetDateTime refreshExpiresAt) {

        /** 남은 초. 설정값이 아니라 <b>지금 기준</b>으로 센다(api-contract.md §3). */
        public long accessExpiresInSeconds() {
            return secondsUntil(accessExpiresAt);
        }

        /** Refresh 의 남은 초. */
        public long refreshExpiresInSeconds() {
            return secondsUntil(refreshExpiresAt);
        }

        private static long secondsUntil(OffsetDateTime at) {
            return Math.max(Duration.between(OffsetDateTime.now(), at).toSeconds(), 0L);
        }
    }
}

package com.dbdomino.moneylog.common.security;

import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

/**
 * Access Token(JWT, HS256)의 발급과 파싱.
 *
 * <p><b>DB 를 모른다.</b> 문자열을 서명하고 해석할 뿐이라 {@code common-mod}에 둔다.
 * 세션을 대조하는 일은 {@code money-backend-app}의 {@code MemberSessionService}·
 * {@code TokenAuthenticationFilter}가 한다 — 그쪽은 {@code data-mod}가 필요하다.
 *
 * <p>클레임은 다섯 개로 고정한다: {@code sub}(memberId)·{@code role}·{@code sid}(sessionId)·
 * {@code iat}·{@code exp}. <b>{@code sub}는 로그인 아이디({@code user_id})이지
 * {@code id_key}가 아니다</b> — 인증 필터가 이를 {@code id_key}로 환산해
 * {@code SecurityContext}에 함께 싣는다.
 *
 * <p>Refresh Token 은 여기서 만들지 않는다. JWT 가 아닌 불투명 랜덤 문자열이고 판정이
 * 전적으로 DB 대조라, 자기 완결 토큰으로 만들면 "서명만 맞으면 유효해 보이는" 오해를 부른다.
 *
 * <p>공개 메서드가 jjwt 타입을 노출하지 않는다 — 파싱 결과는 {@link JwtClaims}로 돌려준다.
 * jjwt 는 {@code common-mod}에 {@code implementation} 범위로 들어와 있어 소비 모듈의
 * 컴파일 클래스패스에 없다 — 노출하면 {@code money-backend-app} 컴파일이 깨진다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md</a>
 */
public class JwtTokenProvider {

    /** 세션 식별자 클레임 이름. */
    private static final String CLAIM_SESSION_ID = "sid";

    /** 권한 클레임 이름. 값은 {@code tbl_user.role}(1 관리자 / 3 일반)이다. */
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    /**
     * @param secret HS256 서명키. 256비트 이상이어야 한다(길이 검증은 호출부의
     *               {@code JwtProperties}가 기동 시점에 한다)
     * @param accessTokenValiditySeconds Access Token 수명(초)
     */
    public JwtTokenProvider(String secret, long accessTokenValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    /**
     * Access Token 을 발급한다.
     *
     * @param memberId 로그인 아이디({@code user_id}). {@code sub}에 실린다
     * @param role     {@code tbl_user.role}
     * @param sessionId {@code tbl_user_session.session_id}
     * @param issuedAt 발급 시각. 만료는 여기에 수명을 더해 정한다
     */
    public String createAccessToken(String memberId, short role, UUID sessionId, Instant issuedAt) {
        Instant expiresAt = issuedAt.plusSeconds(accessTokenValiditySeconds);
        return Jwts.builder()
                .subject(memberId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                // 발급마다 달라지는 식별자. 이것이 없으면 같은 초에 두 번 발급했을 때
                // 클레임이 전부 같아(iat·exp 는 초 단위다) 완전히 동일한 토큰이 나온다.
                // 갱신(Rotation)이 1초 안에 일어나면 "새 토큰"이 옛 토큰과 같은 값이 되고,
                // 저장 해시도 같아져 옛 토큰이 계속 통한다.
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                // 알고리즘을 명시한다. signWith(key) 만 쓰면 jjwt 가 키 길이를 보고
                // HS256·HS384·HS512 중 하나를 고르므로, 키가 길어지는 순간 명세가 정한
                // HS256 이 조용히 다른 알고리즘으로 바뀐다.
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 서명과 만료를 검증하고 클레임을 꺼낸다.
     *
     * <p>서명 실패·형식 오류·만료를 모두 {@link ErrorCode#UNAUTHORIZED}({@code 1001})로
     * 묶는다. 어느 쪽인지 응답으로 알려 주면 토큰을 조립해 보는 쪽에 단서가 된다.
     * 세션이 무효인 경우({@code 1006})와는 구분되는데, 그 판정은 DB 를 봐야 하므로
     * 이 클래스의 일이 아니다.
     *
     * @throws BusinessException {@code 1001} — 토큰 자체가 유효하지 않다
     */
    public JwtClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtClaims(
                    claims.getSubject(),
                    claims.get(CLAIM_ROLE, Integer.class).shortValue(),
                    UUID.fromString(claims.get(CLAIM_SESSION_ID, String.class)),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** Access Token 수명(초). 응답의 {@code expiresIn} 계산에 쓴다. */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    /**
     * 검증을 통과한 Access Token 의 클레임.
     *
     * <p>jjwt 타입을 밖으로 내보내지 않기 위한 값 객체다.
     *
     * @param memberId  {@code sub} — 로그인 아이디({@code user_id})
     * @param role      {@code tbl_user.role}
     * @param sessionId {@code tbl_user_session.session_id}
     * @param issuedAt  발급 시각
     * @param expiresAt 만료 시각. DB 의 {@code access_expires_at}과 <b>둘 다</b> 검사한다
     */
    public record JwtClaims(String memberId, short role, UUID sessionId,
                            Instant issuedAt, Instant expiresAt) {
    }
}

package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.backend.service.MemberSessionService;
import com.dbdomino.moneylog.common.error.BusinessException;
import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.common.security.JwtTokenProvider;
import com.dbdomino.moneylog.common.security.JwtTokenProvider.JwtClaims;
import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserSession;
import com.dbdomino.moneylog.data.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Access Token 검증. {@code _공통.md} 가 정한 10단계를 <b>순서 그대로</b> 수행한다.
 *
 * <p>순서를 바꾸면 응답 코드가 바뀐다. 단계마다 다른 코드가 나가기 때문이다.
 *
 * <pre>
 *  1. Authorization: Bearer 추출                없음   → 익명으로 통과(아래 참고)
 *  2. JWT 서명·형식 검증                         실패   → 1001
 *  3. JWT exp 확인                              만료   → 1001
 *  4. sub(memberId) · sid(sessionId) 추출
 *  5. 세션 조회 후 소유자 대조                    없음   → 1006
 *  6. access_token_hash 가 NULL·빈값                    → 1006
 *  7. sha256(요청 토큰) == access_token_hash    불일치  → 1006
 *  8. now &lt; access_expires_at                  지남   → 1001
 *  9. tbl_user.active                          false  → 1004
 * 10. SecurityContext 에 AuthPrincipal 설정
 * </pre>
 *
 * <h2>실패를 여기서 응답으로 만들지 않는다</h2>
 *
 * <p>실패 코드를 요청 속성에 담아 두고 체인을 계속 태운다. 그래야 두 요구를 동시에
 * 만족한다.
 *
 * <ul>
 *   <li>{@code permitAll} 경로(가입·로그인·갱신·찾기)는 <b>막히면 안 된다</b> —
 *       낡은 토큰을 들고 로그인하러 온 사용자가 거절당하는 상황을 만들지 않는다.
 *   <li>보호 경로는 {@code 1001} 이 아니라 <b>실제 원인 코드</b>({@code 1006}·
 *       {@code 1004})를 돌려줘야 한다. 그 값을 {@link RestAuthEntryPoint} 가 꺼내 쓴다.
 *   </ul>
 *
 * <p>필터가 경로별 인가 규칙을 다시 갖는 대신 판단을 인가 계층에 맡기는 방식이다.
 * 같은 규칙을 두 곳에 적으면 반드시 갈린다.
 *
 * <p>10단계의 {@code last_accessed_at} 갱신은 넣지 않았다. 선택 항목이고 어느 SC 에도
 * 쓰이지 않는데, 넣으면 모든 요청이 UPDATE 를 한 번씩 더 한다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md §2</a>
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    /** 검증 실패 코드를 담아 두는 요청 속성 이름. {@link RestAuthEntryPoint} 가 읽는다. */
    public static final String AUTH_ERROR_ATTRIBUTE =
            TokenAuthenticationFilter.class.getName() + ".ERROR_CODE";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberSessionService sessionService;
    private final UserRepository userRepository;

    public TokenAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                     MemberSessionService sessionService,
                                     UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token != null) {
            try {
                authenticate(request, token);
            } catch (BusinessException e) {
                // 응답은 만들지 않는다. 보호 경로면 RestAuthEntryPoint 가 이 코드를 쓰고,
                // permitAll 경로면 익명으로 계속 진행한다.
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 2~10단계. 실패는 {@link BusinessException} 으로 던진다. */
    private void authenticate(HttpServletRequest request, String token) {
        // 2·3단계 — 서명·형식·exp. 셋 다 1001 이다. 어느 쪽이 틀렸는지 알려 주면
        // 토큰을 조립해 보는 쪽에 단서가 된다.
        JwtClaims claims = jwtTokenProvider.parse(token);

        // 4·5단계 — sub 는 로그인 아이디라 id_key 로 환산해야 조회 조건이 성립한다.
        User user = userRepository.findByUserId(claims.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID));
        UserSession session = sessionService.findBySessionId(claims.sessionId())
                .filter(found -> found.getUser().getIdKey().equals(user.getIdKey()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_INVALID));

        // 6단계 — 폐기된 세션은 해시가 비어 있다. 7단계보다 먼저 본다.
        String storedHash = session.getAccessTokenHash();
        if (storedHash == null || storedHash.isBlank()) {
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        // 7단계 — 다른 곳에서 로그인해 세션이 교체되면 여기서 갈린다.
        if (!storedHash.equals(sessionService.hash(token))) {
            throw new BusinessException(ErrorCode.SESSION_INVALID);
        }
        // 8단계 — JWT exp 와 별개로 DB 만료도 본다. 어느 하나라도 지나면 1001 이다.
        if (session.getAccessExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 9단계 — 관리자가 방금 정지한 회원의 토큰이 아직 살아 있을 수 있다.
        // 세션 폐기(FR-119)와 중복 방어다.
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        AuthPrincipal principal = new AuthPrincipal(
                user.getUserId(), user.getIdKey(), user.getRole(), session.getSessionId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(principal.authority())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 1단계. 헤더가 없거나 형식이 다르면 {@code null} — 익명 요청으로 본다. */
    private static String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}

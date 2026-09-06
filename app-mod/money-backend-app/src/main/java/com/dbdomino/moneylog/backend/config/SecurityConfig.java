package com.dbdomino.moneylog.backend.config;

import com.dbdomino.moneylog.backend.security.RestAccessDeniedHandler;
import com.dbdomino.moneylog.backend.security.RestAuthEntryPoint;
import com.dbdomino.moneylog.common.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인가 경계와 보안 빈.
 *
 * <p>경계는 {@code contracts/api-contract.md §1} 표를 그대로 옮긴 것이다.
 * <b>Controller 애너테이션으로 흩지 않는다</b> — 규칙이 흩어지면 새 API 가 늘 때마다
 * 빠뜨릴 자리가 생긴다.
 *
 * <p><b>기본값이 {@code authenticated}인 것이 핵심이다.</b> 003~006 의 API 가 추가될 때
 * 경로를 빠뜨려도 열리지 않는다. 반대로 두면(기본 {@code permitAll} + 보호 경로 열거)
 * 빠뜨린 API 가 조용히 공개된다.
 *
 * <p>토큰 검증 필터는 US1({@code TokenAuthenticationFilter})에서 이 체인에 끼운다.
 * 그전까지 보호 경로는 인증 정보 없이 {@link RestAuthEntryPoint}로 떨어져
 * {@code 1001}을 돌려준다 — 형식은 이미 규격을 지킨다.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/api-contract.md">api-contract.md §1</a>
 */
@Configuration
public class SecurityConfig {

    /** 인증 없이 부를 수 있는 경로. 헬스체크·가입·로그인·갱신·찾기·재설정 7건이다. */
    private static final String[] PERMIT_ALL_POST = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/find-id",
            "/api/v1/auth/find-password",
            "/api/v1/auth/reset-password"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthEntryPoint authEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http
                // 토큰 기반이라 세션 쿠키가 없다. CSRF 토큰을 쓸 자리도 없다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 폼 로그인·HTTP Basic 은 쓰지 않는다. 켜 두면 인증 실패 응답이 이 경로로 갈린다.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/ha").permitAll()
                        .requestMatchers(HttpMethod.POST, PERMIT_ALL_POST).permitAll()
                        // 관리자 전용. role != 1 이면 RestAccessDeniedHandler 가 1002 를 돌려준다.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 나머지 전부. 003~006 이 붙을 자리도 여기에 걸린다.
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * 비밀번호 해시. bcrypt 만 저장한다(헌장 · FR-104).
     *
     * <p>토큰 해시에는 쓰지 않는다 — Refresh 검증이 해시로 세션을 조회하는데 bcrypt 는
     * salt 때문에 같은 입력이 매번 다른 값을 내 조회가 성립하지 않는다. 그쪽은
     * SHA-256 이고 계산은 {@code MemberSessionService} 한 곳에 가둔다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 발급·파싱. 구현은 {@code common-mod}에 있고 설정값만 여기서 넣는다.
     *
     * <p>{@code common-mod}는 {@link JwtProperties}(백엔드 클래스)를 모르므로 빈 등록을
     * 이 설정이 대신한다.
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
        return new JwtTokenProvider(properties.getSecret(),
                properties.getAccessTokenValiditySeconds());
    }
}

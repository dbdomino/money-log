package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.LoginRequest;
import com.dbdomino.moneylog.backend.dto.request.RefreshRequest;
import com.dbdomino.moneylog.backend.dto.request.SignupRequest;
import com.dbdomino.moneylog.backend.dto.response.LoginResponse;
import com.dbdomino.moneylog.backend.dto.response.MessageResponse;
import com.dbdomino.moneylog.backend.dto.response.SignupResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenResponse;
import com.dbdomino.moneylog.backend.dto.response.TokenValidateResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.AuthService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API — 로그인(1.3)·검증(1.4)·갱신(1.5)·폐기(1.6).
 *
 * <p>Controller 는 Service 만 부른다(헌장 원칙 II). try-catch 로 응답을 제각각 만들지
 * 않고 실패는 전역 예외 처리에 맡기며, 진입·종료 로그도 손으로 쓰지 않는다 —
 * {@code ApiLoggingAspect} 가 한다.
 *
 * <p>인가는 {@code SecurityConfig} 의 경계 표가 정한다. 여기에 권한 애너테이션을
 * 붙이지 않는 이유는 규칙을 두 곳에 두지 않기 위해서다.
 */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 1.2 회원가입. 성공하면 아이디·닉네임·권한을 돌려준다.
     *
     * <p>토큰을 함께 주지 않는다 — 가입과 로그인은 별개의 요청이고, 가입 응답에 토큰을
     * 실으면 "가입 직후 자동 로그인"을 서버가 강제하는 셈이 된다.
     */
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return RestResponseDto.ok(authService.signup(request));
    }

    /** 1.3 로그인. 성공하면 토큰 한 벌과 회원 식별 정보를 돌려준다. */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest servletRequest) {
        return RestResponseDto.ok(authService.login(request, clientIp(servletRequest)));
    }

    /** 1.4 토큰 검증. 필터의 10단계를 통과한 요청만 여기 닿는다. */
    @GetMapping("/validate")
    public RestResponseDto<TokenValidateResponse> validate(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(authService.validate(principal));
    }

    /** 1.5 토큰 갱신(Rotation). Access·Refresh 를 둘 다 새로 낸다. */
    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return RestResponseDto.ok(authService.refresh(request.refreshToken()));
    }

    /** 1.6 로그아웃. 현재 세션을 폐기한다. */
    @PostMapping("/revoke")
    public RestResponseDto<MessageResponse> revoke(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(authService.revoke(principal));
    }

    /**
     * 이력에 남길 요청 IP.
     *
     * <p>프록시 뒤에 있으면 {@code X-Forwarded-For} 의 첫 값이 원 클라이언트다. 그 헤더는
     * 위조할 수 있지만, 이력의 IP 는 인가 판정에 쓰이지 않고 운영자가 시도를 훑어볼 때만
     * 쓰므로 신뢰 수준을 그 용도에 맞춘다. 확보하지 못하면 {@code null} 로 두며 컬럼도
     * nullable 이다.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.PaymentMethodCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodListResponse;
import com.dbdomino.moneylog.backend.dto.response.PaymentMethodResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.PaymentMethodService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지출·소득 수단 API — 2.1~2.5.
 *
 * <p>경로에 회원 식별자를 두지 않는다. 대상 회원은 토큰이 정하며 요청이 지정할 수
 * 없다(FR-201). 경로가 지시하는 것은 <b>수단</b>뿐이고, 그 수단이 본인 것인지는 서비스가
 * 조회 조건으로 건다.
 *
 * <p>인가 애너테이션을 붙이지 않는다 — 002 의 {@code SecurityFilterChain} 이 이 경로를
 * {@code authenticated} 로 이미 덮는다. 여기에 또 적으면 두 곳이 서로 다르게 바뀔 수 있다.
 *
 * <p>Controller 는 Service 만 부른다(헌장 원칙 II). 예외를 잡지 않는다 — 실패 응답 변환은
 * 전역 예외 처리의 몫이다(원칙 III).
 */
@RestController
@RequestMapping(value = "/api/v1/payment-methods", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    /** 2.1 수단 등록. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<PaymentMethodResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PaymentMethodCreateRequest request) {
        return RestResponseDto.ok(paymentMethodService.create(principal, request));
    }

    /** 2.2 관리 목록. 삭제 표시된 수단도 포함한다. */
    @GetMapping
    public RestResponseDto<PaymentMethodListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(paymentMethodService.list(principal));
    }

    /** 2.3 상세 조회. */
    @GetMapping("/{paymentMethodId}")
    public RestResponseDto<PaymentMethodResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long paymentMethodId) {
        return RestResponseDto.ok(paymentMethodService.get(principal, paymentMethodId));
    }

    /**
     * 2.4 수단 수정.
     *
     * <p>Body 를 {@code Map} 으로 받는다. 레코드로 받으면 Jackson 이 "보내지 않은 필드"와
     * "{@code null} 을 보낸 필드"를 똑같이 {@code null} 로 만들어 버려, {@code cardExpiry} 를
     * <b>지우는</b> 조작과 <b>건드리지 않는</b> 조작을 서버가 구별할 수 없다(1.8 과 같다).
     */
    @PatchMapping(value = "/{paymentMethodId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<PaymentMethodResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long paymentMethodId,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(paymentMethodService.update(principal, paymentMethodId, body));
    }

    /**
     * 2.5 수단 삭제.
     *
     * <p>메서드는 {@code DELETE} 지만 <b>동작은 UPDATE</b> 다(삭제 표시, FR-206). 행을 지우면
     * 그 수단을 참조하는 과거 지출·소득의 FK 가 끊긴다.
     */
    @DeleteMapping("/{paymentMethodId}")
    public RestResponseDto<PaymentMethodDeleteResponse> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long paymentMethodId) {
        return RestResponseDto.ok(paymentMethodService.delete(principal, paymentMethodId));
    }
}

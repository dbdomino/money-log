package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.IncomeCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.IncomeCreateResponse;
import com.dbdomino.moneylog.backend.dto.response.IncomeDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.IncomeResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.IncomeService;
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
 * 소득 API — 3.7~3.10.
 *
 * <p><b>지출과 자원을 나눈다</b>({@code /expenses} ↔ {@code /incomes}). 구조가 달라
 * 별도 자원으로 다루기 때문이다(FR-306) — 소득에는 장소·지출유형·할부가 없고
 * {@code content} 가 선택이며, 실패 코드 대역도 {@code 33xx} 로 갈린다.
 *
 * <p>한 자원에 {@code type=EXPENSE|INCOME} 을 붙여 합치는 방안을 쓰지 않는다. 필드의 절반이
 * 한쪽에만 있어 요청·응답 어느 쪽도 하나로 정의할 수 없고, 그러면 "이 필드는 지출일 때만
 * 의미가 있다"는 주석이 계약을 대신하게 된다.
 *
 * <p>인가 애너테이션을 붙이지 않는다 — 002 의 {@code SecurityFilterChain} 이 이 경로를
 * {@code authenticated} 로 이미 덮는다. Controller 는 Service 만 부르고 예외를 잡지
 * 않는다(헌장 원칙 II·III). <b>{@code PUT} 을 쓰지 않는다.</b>
 */
@RestController
@RequestMapping(value = "/api/v1/incomes", produces = MediaType.APPLICATION_JSON_VALUE)
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    /** 3.7 소득 등록. 응답은 생성 PK 한 칸이며 상세는 3.8 로 읽는다. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<IncomeCreateResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody IncomeCreateRequest request) {
        return RestResponseDto.ok(incomeService.create(principal, request));
    }

    /** 3.8 상세 조회. */
    @GetMapping("/{incomeId}")
    public RestResponseDto<IncomeResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long incomeId) {
        return RestResponseDto.ok(incomeService.get(principal, incomeId));
    }

    /**
     * 3.9 소득 수정.
     *
     * <p>Body 를 {@code Map} 으로 받는다. 이 API 에서는 그 선택이 <b>두 가지</b>를 가른다 —
     * 참조를 안 보낸 수정이 스냅샷을 건드리지 않는 것(FR-304)과, {@code "content": null} 이
     * <b>비우라</b>는 뜻인 것이다. 레코드로 받으면 Jackson 이 둘 다 {@code null} 로 만들어
     * "안 보냈다"와 "비워 달라"를 구별할 수 없다.
     */
    @PatchMapping(value = "/{incomeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<IncomeResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long incomeId,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(incomeService.update(principal, incomeId, body));
    }

    /** 3.10 소득 삭제 — <b>물리 삭제</b>다(FR-308). */
    @DeleteMapping("/{incomeId}")
    public RestResponseDto<IncomeDeleteResponse> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long incomeId) {
        return RestResponseDto.ok(incomeService.delete(principal, incomeId));
    }
}

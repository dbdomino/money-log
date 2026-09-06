package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.AdminMemberCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.AdminMemberListQuery;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberDeactivateResponse;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberListResponse;
import com.dbdomino.moneylog.backend.dto.response.AdminMemberResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.AdminMemberService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원 관리 API — 1.12~1.16.
 *
 * <p>권한 애너테이션을 붙이지 않는다. {@code SecurityConfig} 가
 * {@code /api/v1/admin/**} 에 {@code hasRole('ADMIN')} 을 걸어 두었고, 일반 회원 토큰은
 * {@code RestAccessDeniedHandler} 가 {@code 1002} 로 돌려보낸다 — 규칙을 Controller 로
 * 흩으면 새 관리자 API 가 늘 때 빠뜨릴 자리가 생긴다.
 */
@RestController
@RequestMapping(value = "/api/v1/admin/members", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    public AdminMemberController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    /** 1.12 회원 추가. 가입과 달리 권한을 지정할 수 있고, 기본 지출유형 10종은 똑같이 만든다. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<AdminMemberResponse> create(
            @Valid @RequestBody AdminMemberCreateRequest request) {
        return RestResponseDto.ok(adminMemberService.create(request));
    }

    /**
     * 1.13 회원 목록.
     *
     * <p>{@code offset}·{@code limit} 을 {@code Integer} 로 받아 <b>누락과 0 을 구분</b>한다.
     * {@code int} 로 받으면 값이 없을 때 Spring 이 기본값을 채우거나 예외를 내는데, 계약은
     * "없으면 {@code 9001}" 이다. {@code Pageable} 자동 바인딩을 쓰지 않는 이유도 같다.
     */
    @GetMapping
    public RestResponseDto<AdminMemberListResponse> list(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String nickname) {
        return RestResponseDto.ok(adminMemberService.list(
                AdminMemberListQuery.of(offset, limit, memberId, nickname)));
    }

    /** 1.14 회원 상세. */
    @GetMapping("/{memberId}")
    public RestResponseDto<AdminMemberResponse> get(@PathVariable String memberId) {
        return RestResponseDto.ok(adminMemberService.get(memberId));
    }

    /** 1.15 회원 수정. omit 규칙은 1.8 과 같은 방식이다. */
    @PatchMapping(value = "/{memberId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<AdminMemberResponse> update(@PathVariable String memberId,
                                                       @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(adminMemberService.update(memberId, body));
    }

    /**
     * 1.16 회원 정지.
     *
     * <p>{@code DELETE} 가 아니라 {@code PATCH} 다. 지우는 것이 아니라 상태를 바꾸는
     * 것이고, 회원 행과 그 회원의 가계부 데이터는 그대로 남는다(FR-118).
     */
    @PatchMapping("/{memberId}/deactivate")
    public RestResponseDto<AdminMemberDeactivateResponse> deactivate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String memberId) {
        return RestResponseDto.ok(adminMemberService.deactivate(principal, memberId));
    }
}

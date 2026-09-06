package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.response.MemberResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.MemberService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본인 정보 API — 조회(1.7)·수정(1.8).
 *
 * <p>경로에 회원 식별자를 두지 않는다. 대상은 토큰이 정하며 요청이 지정할 수 없다(FR-116).
 *
 * <p>수정은 {@code PATCH} 다. <b>{@code PUT} 은 쓰지 않는다</b>(헌장 원칙 III) — 보내지
 * 않은 필드를 지우는 의미가 되면 화면이 항상 전체 값을 실어 보내야 한다.
 */
@RestController
@RequestMapping(value = "/api/v1/members", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 1.7 본인 정보 조회. */
    @GetMapping("/me")
    public RestResponseDto<MemberResponse> getMe(@AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(memberService.getMe(principal));
    }

    /**
     * 1.8 본인 정보 수정.
     *
     * <p>Body 를 {@code Map} 으로 받는다. 레코드로 받으면 Jackson 이 "보내지 않은 필드"와
     * "null 을 보낸 필드"를 똑같이 {@code null} 로 만들어 버려, 이메일을 <b>지우는</b>
     * 조작과 <b>건드리지 않는</b> 조작을 서버가 구별할 수 없다(api-contract.md §4).
     */
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<MemberResponse> updateMe(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(memberService.updateMe(principal, body));
    }
}

package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.response.ExpendGroupActiveResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupListResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ExpendGroupService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지출유형 API — 현재는 사용 중 목록(2.13)만 붙어 있다.
 *
 * <p>등록(2.7)·목록(2.8)·상세(2.9)·수정(2.11)·삭제(2.12)는 US3 에서 여기에 붙는다.
 *
 * <h2>{@code /active} 를 {@code /{expendGroupId}} 보다 먼저 적는다</h2>
 *
 * <p>둘은 <b>같은 깊이</b>다. 스프링은 리터럴 경로를 템플릿보다 먼저 고르므로 순서를 바꿔도
 * 동작은 같지만, 그 우선순위는 코드에 보이지 않는 규칙이다 — 나중에 {@code /{expendGroupId}}
 * 를 붙이는 사람이 순서를 근거로 삼을 수 있도록 리터럴을 위에 둔다. 매핑이 어긋나면
 * {@code active} 가 {@code Long} 변환에 실패해 {@code 9000} 으로 나가므로, 실패가
 * {@code 3103} 이 아니라 {@code 9000} 이면 이 자리를 의심한다.
 *
 * <p>아이콘 조회(2.10)는 <b>여기 두지 않는다</b> — 응답이 {@code { resCode, data }} 래퍼가
 * 아니라 이미지 바이트라 Controller 를 분리한다(US4).
 *
 * <p>인가 애너테이션을 붙이지 않는다 — 002 의 {@code SecurityFilterChain} 이 이 경로를
 * {@code authenticated} 로 이미 덮는다.
 */
@RestController
@RequestMapping(value = "/api/v1/expend-groups", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExpendGroupController {

    private final ExpendGroupService expendGroupService;

    public ExpendGroupController(ExpendGroupService expendGroupService) {
        this.expendGroupService = expendGroupService;
    }

    /** 2.13 사용 중 목록. {@code inUse=true} 이고 삭제 표시되지 않은 유형만 돌려준다. */
    @GetMapping("/active")
    public RestResponseDto<ExpendGroupListResponse<ExpendGroupActiveResponse>> listActive(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(expendGroupService.listActive(principal));
    }
}

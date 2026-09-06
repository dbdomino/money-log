package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.ExpendGroupCreateRequest;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupActiveResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupListResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ExpendGroupService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 지출유형 API — 2.7~2.9 · 2.11~2.13.
 *
 * <h2>등록·수정은 {@code multipart/form-data} 다</h2>
 *
 * <p>아이콘을 같은 요청의 파트로 받기 때문이다. JSON 에 base64 를 실으면 응답·요청이
 * 비대해지고 {@code Content-Type} 이 뒤섞인다(_공통.md § 지출유형 아이콘).
 *
 * <h2>{@code /active} 를 {@code /{expendGroupId}} 보다 먼저 적는다</h2>
 *
 * <p>둘은 <b>같은 깊이</b>다. 스프링은 리터럴 경로를 템플릿보다 먼저 고르므로 순서를 바꿔도
 * 동작은 같지만, 그 우선순위는 코드에 보이지 않는 규칙이다 — 리터럴을 위에 두어 눈으로도
 * 읽히게 한다. 매핑이 어긋나면 {@code active} 가 {@code Long} 변환에 실패해 {@code 9000} 이
 * 나가므로, 2.13 의 실패가 {@code 3103} 이 아니라 {@code 9000} 이면 이 자리를 의심한다.
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

    /**
     * 2.7 지출유형 등록.
     *
     * <p>{@code iconFile} 파트를 받아 두지만 <b>US4(T042)가 저장을 붙인다.</b> 파일 저장은
     * 커밋 이후로 미뤄야 해서(행을 넣어 PK 를 받아야 파일명을 정할 수 있다) 서비스의 트랜잭션
     * 경계를 함께 손봐야 하는 작업이다. 멀티파트 계약을 먼저 세워 두면 그때 Controller 를
     * 건드리지 않는다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResponseDto<ExpendGroupResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @ModelAttribute ExpendGroupCreateRequest request,
            @RequestParam(name = "iconFile", required = false) MultipartFile iconFile) {
        return RestResponseDto.ok(expendGroupService.create(principal, request));
    }

    /** 2.8 관리 목록. 삭제 표시된 유형도 포함한다. */
    @GetMapping
    public RestResponseDto<ExpendGroupListResponse<ExpendGroupResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(expendGroupService.list(principal));
    }

    /** 2.13 사용 중 목록. {@code inUse=true} 이고 삭제 표시되지 않은 유형만 돌려준다. */
    @GetMapping("/active")
    public RestResponseDto<ExpendGroupListResponse<ExpendGroupActiveResponse>> listActive(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return RestResponseDto.ok(expendGroupService.listActive(principal));
    }

    /** 2.9 상세 조회. */
    @GetMapping("/{expendGroupId}")
    public RestResponseDto<ExpendGroupResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expendGroupId) {
        return RestResponseDto.ok(expendGroupService.get(principal, expendGroupId));
    }

    /**
     * 2.11 지출유형 수정.
     *
     * <p>필드를 하나씩 받는다. multipart 라 <b>파트가 없으면 값이 {@code null}</b> 이고 그것이
     * 곧 omit 이다 — 두 필드 모두 DB 가 NOT NULL 이라 "{@code null} 로 비우기"가 없으므로
     * 1.8·2.4 가 쓰는 {@code PatchFields} 같은 장치가 필요 없다.
     *
     * <p>{@code iconFile} 은 등록과 같은 이유로 US4 에서 처리한다.
     */
    @PatchMapping(value = "/{expendGroupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResponseDto<ExpendGroupResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expendGroupId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "inUse", required = false) Boolean inUse,
            @RequestParam(name = "iconFile", required = false) MultipartFile iconFile) {
        return RestResponseDto.ok(
                expendGroupService.update(principal, expendGroupId, name, inUse));
    }

    /**
     * 2.12 지출유형 삭제.
     *
     * <p>메서드는 {@code DELETE} 지만 <b>동작은 UPDATE</b> 다(삭제 표시). 행도 아이콘 파일도
     * 남는다 — 목표금액·통계가 그 유형을 계속 참조한다(FR-211·FR-215).
     */
    @DeleteMapping("/{expendGroupId}")
    public RestResponseDto<ExpendGroupDeleteResponse> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expendGroupId) {
        return RestResponseDto.ok(expendGroupService.delete(principal, expendGroupId));
    }
}

package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.FixedExpenseCreateRequest;
import com.dbdomino.moneylog.backend.dto.request.FixedExpenseListQuery;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseDeleteResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseListResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.FixedExpenseService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고정지출 설정 API — 4.1 등록 · 4.2 목록 · 4.3 상세 · 4.4 수정 · 4.7 삭제.
 *
 * <p>월별 내역(4.5·4.6·4.9)과 가계부 목록(4.8)은 다른 컨트롤러가 맡는다. 여기는
 * <b>설정</b>만 다룬다 — 두 저장 단위의 성격이 다르고(기준값 vs 그 달의 실제 값)
 * 한 컨트롤러에 몰면 URL 이 섞여 읽기 어려워진다.
 *
 * <p><b>Repository 를 직접 부르지 않는다.</b> 서비스 하나에만 의존한다(헌장 원칙 II).
 *
 * <p><b>진입/종료 로그를 쓰지 않는다.</b> AOP 가 요청~응답을 남긴다(원칙 IV).
 * 005 에는 바이너리 응답이 없어 로깅에서 뺄 API 도 없다.
 */
@RestController
@RequestMapping(value = "/api/v1/fixed-expenses", produces = MediaType.APPLICATION_JSON_VALUE)
public class FixedExpenseController {

    private final FixedExpenseService fixedExpenseService;

    public FixedExpenseController(FixedExpenseService fixedExpenseService) {
        this.fixedExpenseService = fixedExpenseService;
    }

    /** 4.1 등록. <b>관리 행 1건만</b> 만든다 — 적용 기간 전체를 펼치지 않는다(FR-402). */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<FixedExpenseResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody FixedExpenseCreateRequest request) {
        return RestResponseDto.ok(fixedExpenseService.create(principal, request));
    }

    /**
     * 4.2 목록.
     *
     * <p><b>{@code offset}·{@code limit} 이 필수이고 기본값이 없다.</b> 그래서
     * {@code Integer} 로 받아 {@link FixedExpenseListQuery} 가 누락을 {@code 9001} 로
     * 거절한다 — {@code int} 로 받으면 스프링이 0 을 채워 조용히 통과시킨다.
     */
    @GetMapping
    public RestResponseDto<FixedExpenseListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit) {
        return RestResponseDto.ok(fixedExpenseService.list(principal,
                FixedExpenseListQuery.of(offset, limit)));
    }

    /** 4.3 상세. 없거나 남의 것이면 {@code 3402} 다 — 둘을 같은 코드로 낸다. */
    @GetMapping("/{fixedExpenseId}")
    public RestResponseDto<FixedExpenseResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long fixedExpenseId) {
        return RestResponseDto.ok(fixedExpenseService.get(principal, fixedExpenseId));
    }

    /**
     * 4.4 수정 — omit = 유지.
     *
     * <p>{@code Map} 으로 받는 것은 <b>omit 과 명시적 {@code null} 을 구분</b>하기
     * 위해서다. DTO 로 받으면 둘 다 {@code null} 이 되어 "안 보냈다"와 "비우라"를 가를 수 없다.
     *
     * <p><b>응답에 드러나지 않는 부작용이 있다</b> — 미래 달이면서 사용자가 직접 고치지
     * 않은 월별 내역이 새 값을 따라간다(FR-412).
     */
    @PatchMapping(value = "/{fixedExpenseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<FixedExpenseResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long fixedExpenseId,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(fixedExpenseService.update(principal, fixedExpenseId, body));
    }

    /**
     * 4.7 삭제 — <b>물리 삭제</b>다(FR-416).
     *
     * <p>그 고정지출의 월별 내역이 지난 달 것까지 전부 함께 사라진다(CASCADE).
     * 수단·지출유형의 삭제 표시와 다르다.
     */
    @DeleteMapping("/{fixedExpenseId}")
    public RestResponseDto<FixedExpenseDeleteResponse> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long fixedExpenseId) {
        return RestResponseDto.ok(fixedExpenseService.delete(principal, fixedExpenseId));
    }
}

package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.FixedExpenseMonthlyListQuery;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseMonthlyListResponse;
import com.dbdomino.moneylog.backend.dto.response.FixedExpenseMonthlyResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.FixedExpenseMonthlyService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 월별 고정지출 내역 API — 4.5 목록 · 4.6 단건 수정 · 4.9 재작성.
 *
 * <p>지금은 4.5 뿐이고 4.6 은 US3, 4.9 는 US4 에서 붙는다.
 *
 * <p><b>설정(4.1~4.4·4.7)과 컨트롤러를 나눴다.</b> 두 저장 단위의 성격이 다르기
 * 때문이다 — 설정은 "매달 얼마"라는 기준값이고 월별 내역은 "그 달에 실제로 얼마"다.
 * 한 달치만 다르게 고치는 일이 흔해서 나눈 구조이므로 API 도 그 경계를 따른다.
 *
 * <p><b>이 GET 은 상태를 바꾼다.</b> 그 연·월을 처음 조회하면 설정에서 복사해 월별
 * 내역을 만든다(FR-406). 통상적인 기대와 어긋나는 지점이라 plan.md 의 Complexity
 * Tracking 에 근거를 남겼다.
 */
@RestController
@RequestMapping(value = "/api/v1/fixed-expenses/monthly",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class FixedExpenseMonthlyController {

    private final FixedExpenseMonthlyService monthlyService;

    public FixedExpenseMonthlyController(FixedExpenseMonthlyService monthlyService) {
        this.monthlyService = monthlyService;
    }

    /**
     * 4.5 월별 내역 목록.
     *
     * <p><b>{@code year}·{@code month} 가 필수다.</b> {@code Integer} 로 받아
     * {@link FixedExpenseMonthlyListQuery} 가 누락·범위 오류를 <b>{@code 3403}</b> 으로
     * 거절한다 — {@code int} 로 받으면 스프링이 0 을 채워 조용히 통과시킨다.
     *
     * <p>같은 "연·월 범위 오류"인데 4.8(가계부 목록)은 {@code 3501} 이다. 자원별 코드
     * 블록 배정(고정지출 {@code 34xx} / 가계부 {@code 35xx})의 결과이며 의도된 차이다.
     *
     * <p>두 필터는 <b>생성 대상을 좁히지 않는다</b> — 결과만 좁힌다(FR-406).
     */
    @GetMapping
    public RestResponseDto<FixedExpenseMonthlyListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "paymentMethodId", required = false) Long paymentMethodId,
            @RequestParam(name = "expendGroupId", required = false) Long expendGroupId) {
        return RestResponseDto.ok(monthlyService.list(principal,
                FixedExpenseMonthlyListQuery.of(year, month, paymentMethodId, expendGroupId)));
    }

    /**
     * 4.6 월별 내역 단건 수정 — omit = 유지.
     *
     * <p><b>Path 가 세 조각인 것은 그래야 월별 1행이 특정되기 때문이다.</b>
     * {@code fixedExpenseId} 하나로는 안 된다 — 같은 고정지출이 여러 달에 걸쳐 행을
     * 갖는다. 유니크 제약 {@code ux_fixed_expense_monthly (fixed_expense_idx, year, month)}
     * 와 같은 조합이다.
     *
     * <p>바꿀 수 있는 것은 <b>금액·결제일·내용·수단 넷뿐</b>이다(FR-410). 이름·지출유형은
     * 대상이 아니며 보내면 {@code 9001} 이다 — 그것들은 설정(4.4)에서 바꾸고 월별 내역이
     * 따라간다.
     *
     * <p><b>빈 Body 는 {@code 3401} 이다.</b> PATCH omit 규칙상 "아무것도 안 바꾼다"로
     * 읽히지만, 성공으로 흘러가면 {@code modified=true} 만 세워지고 그 표시가 이후
     * 자동 반영(FR-412)에서 이 달을 영구히 제외한다.
     *
     * <p>판정 순서는 <b>{@code 3402} → {@code 3403} → {@code 3405} → {@code 3401}</b> 이며
     * 순서가 결과 코드를 바꾼다(api-contract §6).
     */
    @PatchMapping(value = "/{year}/{month}/{fixedExpenseId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<FixedExpenseMonthlyResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Long fixedExpenseId,
            @RequestBody Map<String, Object> body) {
        return RestResponseDto.ok(
                monthlyService.update(principal, fixedExpenseId, year, month, body));
    }
}

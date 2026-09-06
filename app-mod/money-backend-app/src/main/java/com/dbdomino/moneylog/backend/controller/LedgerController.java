package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.LedgerMonthlyListQuery;
import com.dbdomino.moneylog.backend.dto.response.LedgerMonthlyListResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.LedgerService;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 월별 가계부 API — 4.8.
 *
 * <p><b>사용자가 실제로 여는 화면이다.</b> 저장은 세 테이블로 나뉘어 있지만 사람은 한
 * 목록으로 본다 — 일반 지출·할부·소득·고정지출을 조회 시점에 합칠 뿐이며 별도의 "가계부"
 * 테이블은 없다.
 *
 * <p><b>고정지출 컨트롤러와 분리했다.</b> URL 자원이 다르고({@code /ledger} vs
 * {@code /fixed-expenses}) 이쪽은 네 출처를 읽는 조회 전용이다.
 *
 * <p><b>Controller 가 네 Repository 를 직접 부르지 않는다.</b> 조립은
 * {@code LedgerAssembler}(Service 계층)가 하고 Controller 는 {@code LedgerService} 하나만
 * 본다 — 헌장 원칙 II 이며 plan.md 의 Constitution Check 가 지목한 지점이다.
 */
@RestController
@RequestMapping(value = "/api/v1/ledger", produces = MediaType.APPLICATION_JSON_VALUE)
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * 4.8 월별 가계부 목록.
     *
     * <p><b>이 GET 은 상태를 바꾼다</b> — 그 달의 고정지출 내역이 없으면 4.5 와 같은
     * 규칙으로 만들어 저장한 뒤 목록에 넣는다(FR-418).
     *
     * <p><b>연·월 오류가 {@code 3501} 이다.</b> 4.5·4.6·4.9 는 {@code 3403} 인데 여기만
     * 다르다 — 자원별 코드 블록 배정(고정지출 {@code 34xx} / 가계부 {@code 35xx})의
     * 결과이며 의도된 차이다. 구현하며 놓치기 가장 쉬운 지점이다.
     *
     * <p>{@code type} 은 <b>콤마로 복수 지정</b>한다({@code type=EXPENSE,INSTALLMENT}).
     * 응답에 <b>페이징 필드가 없다</b> — 한 달치를 전부 돌려주고 합계를 함께 싣는다.
     */
    @GetMapping("/monthly")
    public RestResponseDto<LedgerMonthlyListResponse> monthly(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "paymentMethodId", required = false) Long paymentMethodId,
            @RequestParam(name = "expendGroupId", required = false) Long expendGroupId,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order) {
        return RestResponseDto.ok(ledgerService.monthly(principal,
                LedgerMonthlyListQuery.of(year, month, type, paymentMethodId, expendGroupId,
                        dateFrom, dateTo, keyword, sort, order)));
    }
}

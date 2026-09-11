package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.ExpendTargetDefaultUpsertRequest;
import com.dbdomino.moneylog.backend.dto.request.ExpendTargetListQuery;
import com.dbdomino.moneylog.backend.dto.request.ExpendTargetMonthlyUpsertRequest;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDefaultResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetDetailResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetListResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendTargetMonthlyResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.ExpendTargetService;
import com.dbdomino.moneylog.backend.support.StatisticsYearMonth;
import com.dbdomino.moneylog.common.api.RestResponseDto;
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
 * 목표금액 API — 5.1 목록 · 5.2 상세 · 5.3 기본 저장 · 5.4 월별 저장.
 *
 * <p>통계(5.5·5.6)는 다른 컨트롤러가 맡는다. 목표금액은 <b>사용자가 정하는 설정</b>이고
 * 통계는 <b>그 설정과 거래에서 나오는 결과</b>라 성격이 다르다.
 *
 * <h2>저장이 {@code PATCH} 이고 {@code PUT} 이 아니다</h2>
 *
 * <p>행이 없으면 만들고 있으면 갱신한다(upsert, FR-512). 사용자에게 "최초 설정"과
 * "변경"은 같은 행위 — 목표금액 칸에 숫자를 넣는 것뿐이다. 그리고 {@code PUT} 은 이
 * 저장소에서 쓰지 않는다(헌장 원칙 III).
 *
 * <h2>경로가 겹치지 않게 갈래를 앞에 둔다</h2>
 *
 * <pre>{@code
 * GET    /api/v1/expend-targets                                    5.1
 * GET    /api/v1/expend-targets/{year}/{month}/{expendGroupId}     5.2
 * PATCH  /api/v1/expend-targets/default/{expendGroupId}            5.3
 * PATCH  /api/v1/expend-targets/monthly/{year}/{month}/{groupId}   5.4
 * }</pre>
 *
 * <p>5.2 와 5.4 의 연·월 자리가 겹쳐 보이지만 5.4 는 {@code /monthly} 하위라 갈리고,
 * 메서드도 다르다.
 *
 * <p><b>Repository 를 직접 부르지 않는다.</b> 서비스 하나에만 의존한다(헌장 원칙 II).
 *
 * <p><b>진입/종료 로그를 쓰지 않는다.</b> AOP 가 요청~응답을 남긴다(원칙 IV).
 */
@RestController
@RequestMapping(value = "/api/v1/expend-targets", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExpendTargetController {

    private final ExpendTargetService expendTargetService;

    public ExpendTargetController(ExpendTargetService expendTargetService) {
        this.expendTargetService = expendTargetService;
    }

    /**
     * 5.1 목록.
     *
     * <p><b>넷 다 필수이고 기본값이 없다.</b> 그래서 전부 {@code Integer} 로 받아
     * {@link ExpendTargetListQuery} 가 누락을 거절한다 — {@code int} 로 받으면 스프링이
     * 0 을 채워 조용히 통과시키고, {@code year=0} 이 {@code 3603} 대신 빈 목록이 된다.
     *
     * <p>연·월 오류는 {@code 3603}, 페이징 오류는 {@code 9001} 로 <b>코드가 다르다</b>.
     */
    @GetMapping
    public RestResponseDto<ExpendTargetListResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "offset", required = false) Integer offset,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "expendGroupName", required = false) String expendGroupName) {
        return RestResponseDto.ok(expendTargetService.list(principal,
                ExpendTargetListQuery.of(year, month, offset, limit, expendGroupName)));
    }

    /** 5.2 상세. 없거나 남의 것이면 {@code 3103}, 사용하지 않는 유형이면 {@code 3601} 이다. */
    @GetMapping("/{year}/{month}/{expendGroupId}")
    public RestResponseDto<ExpendTargetDetailResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Long expendGroupId) {
        return RestResponseDto.ok(expendTargetService.get(principal,
                StatisticsYearMonth.require(year, month), expendGroupId));
    }

    /**
     * 5.3 기본 목표 저장.
     *
     * <p><b>{@code Map} 으로 받지 않는다.</b> 005 의 4.4·4.6 이 {@code Map} 을 쓴 것은
     * omit 과 명시적 {@code null} 을 가르기 위해서였는데, 여기 몸통은 필드 하나이고
     * 그 하나가 필수라 가를 것이 없다 — 없으면 {@code 3602} 다.
     */
    @PatchMapping(value = "/default/{expendGroupId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<ExpendTargetDefaultResponse> upsertDefault(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long expendGroupId,
            @RequestBody ExpendTargetDefaultUpsertRequest request) {
        return RestResponseDto.ok(expendTargetService.upsertDefault(principal, expendGroupId,
                request.defaultTargetAmount()));
    }

    /** 5.4 월별 목표 저장. 연·월이 Path 에 붙고 몸통 필드 이름이 5.3 과 다르다. */
    @PatchMapping(value = "/monthly/{year}/{month}/{expendGroupId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponseDto<ExpendTargetMonthlyResponse> upsertMonthly(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @PathVariable Long expendGroupId,
            @RequestBody ExpendTargetMonthlyUpsertRequest request) {
        return RestResponseDto.ok(expendTargetService.upsertMonthly(principal,
                StatisticsYearMonth.require(year, month), expendGroupId,
                request.monthlyTargetAmount()));
    }
}

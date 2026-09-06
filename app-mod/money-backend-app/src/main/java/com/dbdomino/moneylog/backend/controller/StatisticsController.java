package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.request.StatisticsViewQuery;
import com.dbdomino.moneylog.backend.dto.response.StatisticsResponse;
import com.dbdomino.moneylog.backend.security.AuthPrincipal;
import com.dbdomino.moneylog.backend.service.StatisticsQueryService;
import com.dbdomino.moneylog.backend.support.StatisticsYearMonth;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 API — 5.5 조회.
 *
 * <p>목표금액(5.1~5.4)은 다른 컨트롤러가 맡는다. 목표금액은 <b>사용자가 정하는 설정</b>이고
 * 통계는 <b>그 설정과 거래에서 나오는 결과</b>다.
 *
 * <h2>이 API 는 GET 이고 부작용이 없다</h2>
 *
 * <p>005 의 4.5·4.8 과 다르다 — 그쪽은 GET 이 월별 고정지출 내역을 <b>만든다</b>(lazy 생성).
 * 006 은 읽기만 하므로 한 번도 열지 않은 달의 고정지출 합계는 0 이다. 화면이 그 달을
 * 채우려면 4.8 이나 4.9 를 먼저 호출한다.
 *
 * <h2>Path 와 Query 를 함께 쓰는 유일한 API 다</h2>
 *
 * <p>연·월은 자원의 좌표라 Path 이고 {@code view} 는 <b>모드 스위치</b>라 Query 다
 * (api-contract §6). 둘 다 어긋난 요청은 연·월이 먼저 걸려 {@code 3603} 인데, 어차피
 * {@code view} 오류도 같은 코드라 사용자에게 보이는 결과는 같다.
 *
 * <p><b>Repository 를 직접 부르지 않는다</b>(헌장 원칙 II). <b>진입/종료 로그를 쓰지
 * 않는다</b> — AOP 가 남긴다(원칙 IV).
 */
@RestController
@RequestMapping(value = "/api/v1/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;

    public StatisticsController(StatisticsQueryService statisticsQueryService) {
        this.statisticsQueryService = statisticsQueryService;
    }

    /**
     * 5.5 월별 통계 조회.
     *
     * <p>{@code view} 는 <b>선택</b>이며 생략은 {@code saved} 와 같은 동작이다. 셋 중 어느
     * 것도 아닌 값은 {@code 3603} 으로 거절한다 — 조용히 무시하면 오타({@code view=lives})가
     * 기본 동작으로 읽혀 사용자가 최신값을 본다고 믿는다.
     */
    @GetMapping("/monthly/{year}/{month}")
    public RestResponseDto<StatisticsResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestParam(name = "view", required = false) String view) {
        return RestResponseDto.ok(statisticsQueryService.get(principal,
                StatisticsYearMonth.require(year, month), StatisticsViewQuery.of(view)));
    }
}

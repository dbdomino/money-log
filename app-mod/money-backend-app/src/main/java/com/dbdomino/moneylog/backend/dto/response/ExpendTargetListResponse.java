package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 5.1 목표금액 목록.
 *
 * <p><b>006 의 여섯 API 중 페이징이 있는 것은 여기뿐이다</b>(api-contract §3). 통계
 * 응답의 배열 3종({@code weeklyExpenses}·{@code expendGroupSummaries}·
 * {@code paymentMethodSummaries})은 한 통계 객체의 구성 요소라 {@code data.list} 규칙의
 * 대상이 아니다.
 *
 * <p><b>부가 필드가 {@code list} 와 같은 레벨이다</b>(FR-526). {@code list} 안이 아니다.
 *
 * <p><b>연·월을 되돌려주는 이유</b>: 목록의 {@code monthlyTargetAmount} 가 어느 달의
 * 값인지는 응답만 봐서는 알 수 없다. 요청과 같은 값이지만 실어 보내야 화면이 응답 하나로
 * 표를 그릴 수 있다 — 기본 목표만 보는 화면도 조회 연·월을 넘기므로 응답 형태가 하나로
 * 유지된다.
 *
 * @param year       조회 연도. 요청과 같다
 * @param month      조회 월. 요청과 같다
 * @param list       <b>사용 중</b> 유형별 목표 (현재 페이지)
 * @param offset     이번 조회에서 건너뛴 건수. 요청값과 같다
 * @param limit      이번 조회에서 가져온 최대 건수. 요청값과 같다
 * @param totalCount 조건에 맞는 <b>전체 건수</b>. <b>사용 중 유형만</b> 센다(FR-526) —
 *                   목록이 필터돼 있는데 전체 건수만 필터를 안 걸면 페이지 수가 맞지 않는다
 */
public record ExpendTargetListResponse(
        int year,
        int month,
        List<ExpendTargetResponse> list,
        int offset,
        int limit,
        long totalCount) {
}

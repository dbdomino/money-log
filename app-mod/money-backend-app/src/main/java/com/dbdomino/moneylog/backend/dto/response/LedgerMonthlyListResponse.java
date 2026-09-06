package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 4.8 월별 가계부 통합 목록.
 *
 * <p><b>부가 필드가 {@code list} 와 같은 레벨이다</b>(FR-423).
 *
 * <p><b>페이징 필드가 하나도 없다</b>(FR-422). {@code offset}·{@code limit}·
 * {@code totalCount} 어느 것도 싣지 않는다 — {@code _공통.md} 의 목록 응답 규칙 적용 대상
 * 표가 4.8 을 "페이징 ❌(합계 필드 병행)"으로 확정했다. 한 회원의 한 달 거래 건수가
 * 규모의 상한이라 전부 돌려주고 합계를 함께 싣는 편이 화면에 맞다.
 *
 * @param list         네 출처를 합친 목록. 정렬·필터가 적용된 결과다
 * @param year         조회 연도
 * @param month        조회 월
 * @param expenseTotal 그 달 지출 합계 — <b>일반 + 할부 + 고정</b> 셋을 합친 값이다.
 *                     <b>필터와 무관하게 그 달 전체 기준</b>이므로 필터를 걸면
 *                     {@code list} 의 합과 다를 수 있고 그것이 의도다 — 합계는 화면 상단
 *                     요약이고 필터는 아래 목록을 좁히는 도구다. 필터마다 상단 숫자가
 *                     흔들리면 사용자가 기준을 잃는다(ledger-list.md § 정한 것)
 * @param incomeTotal  그 달 소득 합계. 위와 같이 필터와 무관하다
 */
public record LedgerMonthlyListResponse(
        List<LedgerItemResponse> list,
        int year,
        int month,
        long expenseTotal,
        long incomeTotal) {
}

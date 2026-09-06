package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 4.5 월별 고정지출 내역 목록.
 *
 * <p><b>부가 필드가 {@code list} 와 같은 레벨이다</b>(FR-423). {@code data.list} 와
 * {@code data.total} 이 형제다.
 *
 * <p><b>페이징 필드가 없다.</b> {@code offset}·{@code limit}·{@code totalCount} 어느 것도
 * 싣지 않는다 — 한 달치를 전부 돌려주기 때문이다. 005 의 목록 넷 중 페이징이 있는 것은
 * 4.2 뿐이다.
 *
 * @param list  그 달의 월별 고정지출 내역. <b>필터가 걸렸으면 좁혀진 결과</b>다
 * @param year  조회 연도
 * @param month 조회 월
 * @param total 그 달 고정지출 합계. <b>필터와 무관하게 그 달 전체 기준</b>이므로 필터를
 *              걸면 {@code list} 의 합과 다를 수 있고, 그것이 의도다. 4.8 의
 *              {@code expenseTotal}·{@code incomeTotal} 과 <b>같은 규칙</b>이다 —
 *              합계는 화면 상단 요약이고 필터는 아래 목록을 좁히는 도구다.
 *              두 API 가 서로 다른 규칙을 쓰면 구현이 반드시 한쪽을 틀린다
 */
public record FixedExpenseMonthlyListResponse(
        List<FixedExpenseMonthlyResponse> list,
        int year,
        int month,
        long total) {
}

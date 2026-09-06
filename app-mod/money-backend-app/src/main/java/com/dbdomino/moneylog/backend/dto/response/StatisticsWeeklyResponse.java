package com.dbdomino.moneylog.backend.dto.response;

import java.time.LocalDate;

/**
 * 주별 지출 한 행 (5.5·5.6).
 *
 * <p><b>경계를 함께 내린다.</b> {@code weekIndex} 만 주면 화면이 "3주가 며칠부터
 * 며칠까지인가"를 스스로 계산해야 하는데, 그 규칙(월요일 시작 · 첫 주는 1일부터 첫
 * 일요일 · 마지막 주는 말일에서 끊음)이 서버에만 있어 <b>같은 규칙을 프론트에도
 * 복제해야 한다</b>. 복제하면 둘이 갈릴 수 있다.
 *
 * <p><b>저장본은 저장된 경계를 그대로 쓴다</b>(statistics-snapshot.md §4). 경계 규칙이
 * 나중에 바뀌어도 이미 저장된 통계는 그때 경계로 남는다 — 저장본 불변(FR-518)의 한
 * 갈래다. 다시 계산하는 것은 저장본이 없는 달과 {@code view=live} 뿐이다.
 *
 * @param weekIndex 주차. <b>1부터</b>다({@code ck_stat_weekly_index})
 * @param weekStart 주 시작일. 1주는 그 달 1일이다
 * @param weekEnd   주 종료일. 마지막 주는 말일이다
 * @param amount    그 주의 지출 합계. <b>0원인 주도 행이 남는다</b> — 빠뜨리면 화면의
 *                  주별 막대에 구멍이 생긴다
 */
public record StatisticsWeeklyResponse(
        int weekIndex,
        LocalDate weekStart,
        LocalDate weekEnd,
        long amount) {
}

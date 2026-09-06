package com.dbdomino.moneylog.backend.service.statistics;

import com.dbdomino.moneylog.backend.support.YearMonthValue;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 주별 지출의 주 경계 — <b>월요일 시작</b>(FR-520).
 *
 * <h2>규칙 셋</h2>
 *
 * <pre>{@code
 * 1. 주는 월요일에 시작한다.
 * 2. 그 달 1일이 월요일이 아니면 첫 주는 1일부터 첫 일요일까지다 (짧은 주).
 * 3. 마지막 주는 말일에서 끊는다 (짧은 주).
 * }</pre>
 *
 * <p>예: 2026-07-01 은 수요일이다.
 *
 * <pre>{@code
 * 1주  07-01(수) ~ 07-05(일)   ← 3일이 아니라 5일. 첫 일요일까지다
 * 2주  07-06(월) ~ 07-12(일)
 * 3주  07-13(월) ~ 07-19(일)
 * 4주  07-20(월) ~ 07-26(일)
 * 5주  07-27(월) ~ 07-31(금)   ← 말일에서 끊긴다
 * }</pre>
 *
 * <h2>왜 한 곳에 두는가</h2>
 *
 * <p>이 계산이 <b>두 경로</b>에 걸린다 — 5.5 의 즉석 계산은 매번 경계를 만들고, 5.6 의
 * 저장본은 만든 경계를 {@code week_start}·{@code week_end} 컬럼에 <b>저장해 두고</b>
 * 나중에 그대로 읽는다. 계산 규칙이 두 곳에 있으면 "지금 계산한 주"와 "저장된 주"의
 * 구분이 갈리고, 같은 달을 두 방식으로 본 사용자가 다른 표를 본다.
 *
 * <p>{@code week_index} 는 <b>1부터</b>다. 유니크 제약이
 * {@code ux_stat_weekly (statistics_idx, week_index)} 라 한 통계에 같은 번호가 둘일 수 없다.
 *
 * @see <a href="../../../../../../../../../specs/006-backend-target-statistics/contracts/statistics-snapshot.md">statistics-snapshot.md §4</a>
 */
@Component
public class WeekBoundaryResolver {

    /**
     * 한 주의 경계. 양 끝을 포함한다.
     *
     * @param weekIndex 1부터 매기는 주 번호
     * @param start     그 주의 첫날. 1주는 그 달 1일이다
     * @param end       그 주의 마지막 날. 마지막 주는 말일이다
     */
    public record Week(int weekIndex, LocalDate start, LocalDate end) {

        /** 그 날짜가 이 주에 속하는가. 양 끝을 포함한다. */
        public boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }
    }

    /**
     * 그 달의 주 경계를 순서대로 만든다.
     *
     * <p>첫 주는 <b>1일부터 첫 일요일까지</b>다 — 1일이 월요일이면 온전한 7일이고,
     * 아니면 짧아진다. 그 뒤로는 월요일마다 끊고 마지막은 말일에서 멈춘다.
     *
     * @return 최소 1개. 빈 목록이 될 수 없다 — 어떤 달이든 하루는 있다
     */
    public List<Week> weeksOf(YearMonthValue yearMonth) {
        LocalDate firstDay = yearMonth.firstDay();
        LocalDate lastDay = yearMonth.lastDay();

        List<Week> weeks = new ArrayList<>();
        LocalDate start = firstDay;
        int index = 1;

        while (!start.isAfter(lastDay)) {
            // 이 주의 끝은 "다음 일요일"이되 말일을 넘지 않는다.
            LocalDate sunday = nextOrSameSunday(start);
            LocalDate end = sunday.isAfter(lastDay) ? lastDay : sunday;
            weeks.add(new Week(index, start, end));
            start = end.plusDays(1);
            index++;
        }
        return weeks;
    }

    /**
     * 그 날짜이거나 그 뒤의 첫 일요일.
     *
     * <p>1일이 일요일이면 <b>그날이 곧 첫 주의 끝</b>이라 1주가 하루짜리가 된다.
     * 규칙("1일부터 첫 일요일까지")의 직접적인 결과이며 예외로 두지 않는다.
     */
    private static LocalDate nextOrSameSunday(LocalDate date) {
        int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - date.getDayOfWeek().getValue();
        return date.plusDays(daysUntilSunday);
    }
}

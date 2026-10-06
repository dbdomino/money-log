package com.dbdomino.moneylog.front.fixedexpense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 그 달의 고정지출 내역 — 요약(합계)과 목록을 함께 담는다.
 *
 * <h2>합계를 화면이 다시 더하지 않는다</h2>
 *
 * <p>상단 합계는 <b>그 달 전체 기준</b>이며 목록을 좁히는 조건과 무관하다. 010 의 가계부에서
 * 정한 것과 같은 규칙이고, <b>백엔드가 두 API 에 같은 규칙을 둔 이유도 같다</b> — 다르게
 * 두면 구현이 반드시 한쪽을 틀린다.
 *
 * <h2>조회가 쓰기를 겸한다</h2>
 *
 * <p>그 연·월의 행이 없으면 <b>서버가 만들어 저장한 뒤</b> 목록에 넣는다. 조회처럼 보이지만
 * 부작용이 있는 유일한 호출이다.
 *
 * <p><b>화면은 그 달을 열 뿐이고 만들라고 따로 요청하지 않는다</b> — 만드는 규칙(적용 기간·
 * 말일 보정)이 백엔드에 있고, 화면이 따로 요청하면 같은 판단이 두 곳에 생긴다.
 *
 * <p>그래서 <b>한 번도 열지 않은 달은 통계(012)의 고정지출 합계가 0</b> 이다.
 *
 * @param year 조회 연도
 * @param month 조회 월
 * @param total 그 달 고정지출 합계. <b>그 달 전체 기준</b>이며 목록의 합과 다를 수 있다
 * @param list 그 달 내역
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyResult(
        Integer year,
        Integer month,
        Long total,
        List<MonthlyRow> list) {

    /**
     * 응답이 통째로 비어 왔을 때 쓰는 값. <b>빈 목록과 합계 0</b> 이다.
     *
     * <p>여기서 터지지 않게 하는 것이 요점이다 — 모달이 열리지 않으면 사용자는 고정지출
     * 화면 전체가 죽었다고 읽는다.
     */
    public static MonthlyResult empty(int year, int month) {
        return new MonthlyResult(year, month, 0L, List.of());
    }

    /** 행이 없으면 빈 목록으로 다룬다. */
    public List<MonthlyRow> rows() {
        return list == null ? List.of() : list;
    }

    /** 그 달 합계. <b>백엔드가 준 값 그대로</b>이며 목록을 다시 더하지 않는다. */
    public long sum() {
        return total == null ? 0L : total;
    }

    /**
     * 그 달 대상이 없는가. <b>「이 달에 해당하는 고정지출이 없습니다」의 근거</b>다.
     *
     * <p>적용 기간이 그 달을 포함하지 않으면 나오지 않는다는 것이 이유이며, 빈 목록만
     * 두면 사용자는 불러오지 못한 것으로 읽는다.
     */
    public boolean isEmpty() {
        return rows().isEmpty();
    }
}

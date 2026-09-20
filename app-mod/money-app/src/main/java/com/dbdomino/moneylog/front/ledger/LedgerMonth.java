package com.dbdomino.moneylog.front.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 백엔드 월별 가계부 응답. 그 달의 <b>요약과 목록</b>을 함께 담는다.
 *
 * <h2>쪽 정보가 없다</h2>
 *
 * <p>이 목록은 조회 구간을 받지 않고 <b>한 달치를 전부</b> 돌려주며 합계도 함께 온다. 007 의
 * 페이징 환산기를 쓰지 않는다 — 쪽을 나누면 합계와 목록의 기준이 갈린다.
 *
 * <h2>합계를 화면이 다시 더하지 않는다</h2>
 *
 * <p>상단 요약은 <b>그 달 전체 기준</b>이고 필터와 무관하다. 필터를 걸면 목록의 합과 상단
 * 합계가 달라지는데 <b>그것이 의도다</b> — 합계는 화면 상단의 요약이고 필터는 아래 목록을
 * 좁히는 도구다.
 *
 * <p>맞추려 들면 "필터를 걸었는데 이번 달 지출이 줄어드는" 화면이 되고, <b>사용자는 자기가
 * 쓴 돈이 줄었다고 읽는다.</b>
 *
 * <h2>잔액은 응답에 없다</h2>
 *
 * <p>화면이 두 합계를 뺀다. <b>같은 기준의 두 값을 빼는 것</b>이라 필터와 무관하다는 성질이
 * 그대로 유지된다 — 목록을 더해 만들면 그 성질이 깨진다.
 *
 * @param year 조회 연도
 * @param month 조회 월
 * @param expenseTotal 그 달 지출 합계. 일반·할부·고정지출을 모두 더한 값이다
 * @param incomeTotal 그 달 소득 합계
 * @param list 그 달의 가계부 행. 필터·정렬이 적용된 뒤의 목록이다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerMonth(
        Integer year,
        Integer month,
        Long expenseTotal,
        Long incomeTotal,
        List<LedgerRow> list) {

    /** 응답이 통째로 비어 왔을 때 쓰는 값. <b>빈 목록과 합계 0</b> 이다. */
    public static LedgerMonth empty(int year, int month) {
        return new LedgerMonth(year, month, 0L, 0L, List.of());
    }

    /** 행이 없으면 빈 목록으로 다룬다. 템플릿이 null 을 가리는 분기를 갖지 않게 한다. */
    public List<LedgerRow> rows() {
        return list == null ? List.of() : list;
    }

    /** 지출 합계. <b>백엔드가 준 값 그대로</b>이며 목록을 다시 더하지 않는다. */
    public long expenseSum() {
        return expenseTotal == null ? 0L : expenseTotal;
    }

    /** 소득 합계. 백엔드가 준 값 그대로다. */
    public long incomeSum() {
        return incomeTotal == null ? 0L : incomeTotal;
    }

    /**
     * 잔액. 소득 합계에서 지출 합계를 뺀다.
     *
     * <p>응답에 없는 값이라 화면이 만든다. <b>목록을 더해 만들지 않는 것</b>이 요점이다 —
     * 그러면 필터를 걸 때마다 잔액이 흔들린다.
     */
    public long balance() {
        return incomeSum() - expenseSum();
    }

    /** 거래가 없는 달인가. 합계 0 과 함께 <b>빈 목록임을 알리는</b> 근거다. */
    public boolean isEmpty() {
        return rows().isEmpty();
    }
}

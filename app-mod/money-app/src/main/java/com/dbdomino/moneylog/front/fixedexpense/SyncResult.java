package com.dbdomino.moneylog.front.fixedexpense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 「고정지출 반영」의 결과. <b>건수 넷과 재작성 후 목록이 함께</b> 온다.
 *
 * <h2>「보존」이 특히 중요하다</h2>
 *
 * <p>네 건수 가운데 <b>보존</b>이 이 화면의 요점이다. 그 숫자가 <b>직접 고친 값이
 * 살아남았다는 증거</b>이고, 「직접 수정분도 되돌리기」를 켰을 때 <b>0 이 되는 것으로
 * 무엇이 달랐는지 읽힌다.</b>
 *
 * <p>셋만 보이면(추가·갱신·삭제) 사용자는 자기가 손으로 고친 값이 어떻게 됐는지 알 수 없다.
 *
 * <h2>목록이 함께 와서 다시 조회하지 않는다</h2>
 *
 * <p>재작성 후 목록과 합계가 응답에 들어 있다. 화면은 그것으로 갱신하고 <b>다시 조회하지
 * 않는다</b> — 다시 부르면 그사이 바뀐 값이 섞여 "방금 반영한 결과"가 아닌 것을 보게 된다.
 *
 * @param year 재작성한 연도
 * @param month 재작성한 월
 * @param createdCount 새로 만든 내역 수
 * @param updatedCount 설정 값으로 갱신한 내역 수
 * @param deletedCount 없앤 내역 수
 * @param keptCount <b>직접 수정 행이라 손대지 않은 수</b>
 * @param total 재작성 후 그 달 합계
 * @param list 재작성 후 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SyncResult(
        Integer year,
        Integer month,
        Integer createdCount,
        Integer updatedCount,
        Integer deletedCount,
        Integer keptCount,
        Long total,
        List<MonthlyRow> list) {

    public int created() {
        return createdCount == null ? 0 : createdCount;
    }

    public int updated() {
        return updatedCount == null ? 0 : updatedCount;
    }

    public int deleted() {
        return deletedCount == null ? 0 : deletedCount;
    }

    /** 직접 고쳐서 보존된 수. <b>되돌리기를 켜면 0 이 된다.</b> */
    public int kept() {
        return keptCount == null ? 0 : keptCount;
    }

    /**
     * 재작성 후 목록과 합계를 그 달 내역으로 옮긴다.
     *
     * <p>화면이 응답 두 모양(반영 결과 · 그 달 내역)을 따로 다루지 않게 한다 — 목록을
     * 그리는 코드가 하나면 반영 뒤와 조회 뒤의 표가 달라 보일 일이 없다.
     */
    public MonthlyResult toMonthly() {
        return new MonthlyResult(year, month, total, list);
    }
}

package com.dbdomino.moneylog.backend.dto.request;

import com.dbdomino.moneylog.backend.support.YearMonthValue;
import com.dbdomino.moneylog.common.error.ErrorCode;

/**
 * 4.9 월별 내역 재작성의 요청.
 *
 * <h2>연·월을 Body 로 받는다 (FR-421)</h2>
 *
 * <p>Path 나 Query 를 쓰지 않는다. 이 저장소의 규칙상 <b>POST 는 Path·Query 를 쓰지
 * 않고</b> 생성·실행에 필요한 값을 전부 Body 로 받는다 — 4.8 이 GET 이라 Query 를 쓰는
 * 것과 대비된다.
 *
 * <p>연·월 오류는 <b>{@code 3403}</b> 이다(4.5·4.6 과 같다). 4.8 만 {@code 3501} 이며
 * 자원별 대역 배정의 결과다.
 *
 * <h2>{@code overwriteModified} 가 이 API 의 성격을 가른다</h2>
 *
 * <table border="1">
 *   <caption>사용자가 직접 고친 달을 어떻게 하나</caption>
 *   <tr><th>값</th><th>처리</th><th>언제 쓰나</th></tr>
 *   <tr><td>{@code false}(기본)</td><td><b>보존</b> — {@code keptCount} 로 센다</td>
 *       <td>"설정대로 다시 맞추되 내가 손댄 달은 그대로 둔다"</td></tr>
 *   <tr><td>{@code true}</td><td><b>갱신</b>하고 {@code modified} 를 내린다</td>
 *       <td>"직접 고친 것까지 전부 되돌린다"</td></tr>
 * </table>
 *
 * <p><b>기본이 보존인 이유</b>는 되돌리기가 파괴적이기 때문이다 — 사용자가 일부러 넣은
 * 값이 사라지므로 명시적으로 요청할 때만 한다. 화면에서 확인을 받는 것을 전제한다.
 *
 * @param yearMonth         재작성할 연·월. 검증을 통과한 값이다
 * @param overwriteModified 직접 수정분까지 되돌릴지. 생략하면 {@code false}
 */
public record FixedExpenseMonthlySyncRequest(YearMonthValue yearMonth,
                                             boolean overwriteModified) {

    /**
     * 요청 Body 를 검증한다.
     *
     * <p>{@code Boolean} 으로 받아 {@code null}(생략)을 {@code false} 로 접는다 —
     * {@code boolean} 으로 받으면 Jackson 이 이미 {@code false} 를 채워 "생략"과
     * "명시적 false" 를 가를 수 없는데, 여기서는 그 둘의 처리가 같아 문제되지 않는다.
     * 다만 <b>기본값이 무엇인지가 계약</b>이므로 그 접기를 눈에 보이게 둔다.
     *
     * @throws com.dbdomino.moneylog.common.error.BusinessException {@code 3403} —
     *         연·월 누락 또는 범위 오류
     */
    public static FixedExpenseMonthlySyncRequest of(Integer year, Integer month,
                                                    Boolean overwriteModified) {
        return new FixedExpenseMonthlySyncRequest(
                YearMonthValue.require(year, month, ErrorCode.FIXED_EXPENSE_MONTH_INVALID),
                Boolean.TRUE.equals(overwriteModified));
    }
}

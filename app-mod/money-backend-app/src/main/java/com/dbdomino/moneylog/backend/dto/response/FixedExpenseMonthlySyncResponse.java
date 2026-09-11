package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 4.9 재작성 결과 — <b>네 처리의 건수와 결과 목록</b>.
 *
 * <h2>건수를 넷으로 나눠 돌려주는 이유</h2>
 *
 * <p>재작성은 한 번의 호출로 성격이 다른 네 가지 일을 한다. 합쳐서 "N건 처리"로 주면
 * 사용자가 <b>무엇이 일어났는지 알 수 없다</b> — 특히 {@code keptCount} 와
 * {@code deletedCount} 는 "왜 내가 고친 값이 그대로인가", "왜 행이 사라졌는가"라는
 * 질문에 직접 답한다.
 *
 * <table border="1">
 *   <caption>네 처리(FR-414)</caption>
 *   <tr><th>필드</th><th>조건</th></tr>
 *   <tr><td>{@code createdCount}</td><td>기간에 걸리는데 그 연·월 행이 없음(신규 등록분 포함)</td></tr>
 *   <tr><td>{@code updatedCount}</td><td>행이 있고 {@code modified=false}</td></tr>
 *   <tr><td>{@code keptCount}</td><td>행이 있고 {@code modified=true} — 손대지 않았다</td></tr>
 *   <tr><td>{@code deletedCount}</td><td>행이 있는데 적용 기간이 그 연·월을 더는 포함하지 않음</td></tr>
 * </table>
 *
 * <p>{@code overwriteModified=true} 면 ③이 ②로 넘어가 {@code keptCount} 가 0 이 되고
 * 그만큼 {@code updatedCount} 가 는다.
 *
 * <h2>결과 목록을 함께 돌려준다 (FR-415)</h2>
 *
 * <p>호출 후 <b>재조회가 필요 없어야</b> 화면이 한 번의 왕복으로 끝난다. 구조는 4.5 의
 * {@code list[]} 와 같아 프론트가 같은 렌더링 코드를 쓴다.
 *
 * <p><b>{@code deletedCount} 에 잡힌 행은 목록에 없다.</b> 재작성 <b>후</b>의 상태이기
 * 때문이며, 그래서 {@code list} 의 길이가 네 건수의 합과 다르다 —
 * {@code createdCount + updatedCount + keptCount} 가 목록 길이다.
 *
 * @param list          재작성 후 그 달의 목록. 4.5 의 {@code list[]} 와 같은 구조다
 * @param year          재작성한 연도
 * @param month         재작성한 월
 * @param createdCount  새로 만든 내역 수
 * @param updatedCount  관리 값으로 갱신한 내역 수
 * @param keptCount     직접 수정 행이라 손대지 않은 수
 * @param deletedCount  기간 밖이 되어 없앤 내역 수
 * @param total         재작성 후 그 달 고정지출 합계
 */
public record FixedExpenseMonthlySyncResponse(
        List<FixedExpenseMonthlyResponse> list,
        int year,
        int month,
        int createdCount,
        int updatedCount,
        int keptCount,
        int deletedCount,
        long total) {
}

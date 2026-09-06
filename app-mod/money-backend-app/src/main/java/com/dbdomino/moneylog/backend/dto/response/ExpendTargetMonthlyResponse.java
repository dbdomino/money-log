package com.dbdomino.moneylog.backend.dto.response;

/**
 * 5.4 월별 목표 등록·수정의 결과.
 *
 * <p>연·월이 붙는 것 말고는 {@link ExpendTargetDefaultResponse} 와 같은 성격이다 —
 * <b>담당한 층만</b> 돌려준다. 기본 목표는 이 호출로 변하지 않는다(FR-505).
 *
 * <p>{@code monthlyTargetAmount} 가 여기서는 {@code long} 이다. 방금 저장한 값이라
 * "행이 없음"({@code null})이 나올 수 없다 — 조회 응답에서만 {@code Long} 이 필요하다.
 *
 * @param year                저장한 연도
 * @param month               저장한 월
 * @param expendGroupId       지출유형 PK
 * @param expendGroupName     지출유형의 <b>현재</b> 이름
 * @param monthlyTargetAmount 저장된 월별 목표금액. <b>{@code 0} 도 정상 결과다</b>
 */
public record ExpendTargetMonthlyResponse(
        int year,
        int month,
        long expendGroupId,
        String expendGroupName,
        long monthlyTargetAmount) {
}

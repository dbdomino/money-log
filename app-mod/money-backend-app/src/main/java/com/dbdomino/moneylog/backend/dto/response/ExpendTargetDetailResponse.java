package com.dbdomino.moneylog.backend.dto.response;

/**
 * 5.2 목표금액 상세 조회.
 *
 * <p>금액 두 필드는 {@link ExpendTargetResponse} 와 같고 <b>연·월이 앞에 붙는다</b> —
 * 목록에서는 연·월이 응답 전체에 한 번 실리지만 단건에서는 그 자리가 없다.
 *
 * <p>두 필드의 미설정 표현과 적용 금액 필드를 두지 않은 이유는
 * {@link ExpendTargetResponse} 의 설명이 그대로 적용된다.
 *
 * @param year                조회 연도. Path 와 같다
 * @param month               조회 월. Path 와 같다
 * @param expendGroupId       지출유형 PK
 * @param expendGroupName     지출유형의 <b>현재</b> 이름
 * @param defaultTargetAmount 기본 월 목표금액. 행이 없으면 {@code 0}
 * @param monthlyTargetAmount 그 연·월에 별도 저장된 목표. 행이 없으면 {@code null}
 */
public record ExpendTargetDetailResponse(
        int year,
        int month,
        long expendGroupId,
        String expendGroupName,
        long defaultTargetAmount,
        Long monthlyTargetAmount) {
}

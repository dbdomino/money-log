package com.dbdomino.moneylog.backend.dto.response;

import java.math.BigDecimal;

/**
 * 유형별 요약 한 행 (5.5·5.6).
 *
 * <p><b>모집단은 그 달 지출이 1건 이상인 유형뿐이다</b>(FR-521). 목표만 정해 두고 한 푼도
 * 쓰지 않은 유형은 없다 — 수단별과 정반대이며, "이 달에 어디에 썼나"를 보는 표라
 * 안 쓴 유형을 넣을 이유가 없다.
 *
 * <p><b>{@code expendGroupId} 가 실재하지 않을 수 있다</b>(FR-519). 통계 상세에는 지출유형
 * FK 가 <b>없어서</b> 원본이 삭제돼도 이 행이 남는다. 화면 복원은 함께 저장된
 * {@code expendGroupName} 이 맡으므로 프론트는 ID 로 원본을 조회하려 하지 말고 <b>저장된
 * 이름을 그대로 표시</b>해야 한다.
 *
 * @param expendGroupId   지출유형 PK. <b>실재를 보장하지 않는다</b>
 * @param expendGroupName <b>저장 시점 이름 스냅샷</b>. 목표금액 API 의 현재 이름과 반대다
 * @param amount          그 달 그 유형의 지출 합계
 * @param target          <b>적용 금액</b> — 월별 목표가 있으면 그것, 없으면 기본 목표.
 *                        기본이 없으면 0 이라 <b>절대 {@code null} 이 아니다</b>
 * @param usageRate       사용률 % ({@code amount / target × 100}).
 *                        <b>목표가 0 이면 0</b>, <b>상한은 {@code 9999.99}</b>
 *                        ({@code numeric(6,2)} 라 넘으면 저장이 실패한다)
 * @param status          {@code UNDER}(90% 미만) · {@code OK}(90~110) · {@code OVER}(110% 초과).
 *                        <b>경계 양쪽이 다 {@code OK}</b> 다
 */
public record StatisticsExpendGroupResponse(
        long expendGroupId,
        String expendGroupName,
        long amount,
        long target,
        BigDecimal usageRate,
        String status) {
}

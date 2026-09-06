package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.6 중도상환 응답.
 *
 * <p>{@code settledCount} 는 <b>제거된 미래 회차 수</b>다. 그룹의 전체 회차 수가 아니며,
 * 과거·오늘 회차는 남아 있다(FR-315).
 *
 * <p>0건이 될 수 없다 — 지울 것이 없으면 {@code 3207} 로 거절하기 때문이다. "방금
 * 정리했다"와 "이미 정리되어 있었다"를 화면이 구분해야 해서 멱등 성공으로 흘리지 않는다.
 *
 * @param installmentGroupId 중도상환 처리한 할부 그룹
 * @param settledCount       제거된 남은 회차 수. 항상 1 이상이다
 * @param message            화면에 보여 줄 문구
 */
public record InstallmentSettleResponse(Long installmentGroupId, long settledCount, String message) {
}

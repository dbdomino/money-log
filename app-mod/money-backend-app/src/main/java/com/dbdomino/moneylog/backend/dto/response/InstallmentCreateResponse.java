package com.dbdomino.moneylog.backend.dto.response;

/**
 * 3.5 할부 등록 응답 — <b>그룹 식별에 필요한 최소 정보</b>다.
 *
 * <p><b>지출 목록을 싣지 않는다.</b> 12개월 할부면 12건이 생기는데 그것을 전부 돌려주면
 * 응답이 커지고, 화면이 실제로 필요할 때는 월별 가계부나 3.2 로 읽는 편이 낫다.
 *
 * <p>목록 응답이 아니므로 {@code data.list} 규칙이 적용되지 않는다(api-contract.md §2) —
 * {@code createdCount} 는 목록의 건수가 아니라 <b>생성 결과의 요약</b>이다.
 *
 * @param installmentGroupId 생성된 할부 그룹 식별자. 중도상환(3.6)이 이 값을 받는다
 * @param createdCount       생성된 지출 건수. 요청의 {@code installmentMonths} 와 같다
 */
public record InstallmentCreateResponse(Long installmentGroupId, int createdCount) {
}

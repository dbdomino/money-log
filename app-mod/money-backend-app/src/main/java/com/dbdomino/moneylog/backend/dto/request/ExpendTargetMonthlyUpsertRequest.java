package com.dbdomino.moneylog.backend.dto.request;

/**
 * 5.4 월별 목표 등록·수정의 Body.
 *
 * <p>연·월과 지출유형은 Path 에 있고 몸통은 금액 하나다.
 *
 * @param monthlyTargetAmount 그 연·월의 목표금액. {@code 0 ~ 100,000,000}.
 *                            <b>{@code 0} 은 "그 달엔 쓰지 않겠다"</b>라 행이 없는 상태와
 *                            다르다(FR-506)
 * @see ExpendTargetDefaultUpsertRequest 필드 이름을 나눈 이유와 {@code Long} 인 이유
 */
public record ExpendTargetMonthlyUpsertRequest(Long monthlyTargetAmount) {
}

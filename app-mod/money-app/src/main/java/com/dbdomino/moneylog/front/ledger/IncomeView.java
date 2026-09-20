package com.dbdomino.moneylog.front.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 수정 모달이 채워 넣는 소득 한 건. 3.4 가 모달을 열기 전에 조회해 싣는다.
 *
 * <p><b>지출유형·장소 자리를 두지 않는다.</b> 백엔드가 소득에 그 칸을 주지도 받지도 않는다 —
 * 타입에 두면 언제나 비어 있는 자리가 생기고, 그 자리를 본 다음 사람이 모달에 칸을 만든다.
 *
 * @param incomeId 식별자. 수정 요청 경로에 쓴다
 * @param paymentMethodId 수단 식별자. 선택 칸의 현재 값이다
 * @param amount 금액
 * @param paymentDate 입금일 {@code YYYY-MM-DD}
 * @param content 내용. 비어 있을 수 있다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncomeView(
        Long incomeId,
        Long paymentMethodId,
        Long amount,
        String paymentDate,
        String content) {
}

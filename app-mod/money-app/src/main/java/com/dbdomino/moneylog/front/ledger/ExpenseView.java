package com.dbdomino.moneylog.front.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 수정 모달이 채워 넣는 지출 한 건. 3.2 가 모달을 열기 전에 조회해 싣는다.
 *
 * <p><b>할부인지가 이 응답에 들어 있다.</b> 그래서 화면이 미리 막을 수 있다 — 고칠 수 없는
 * 칸을 열어 두고 저장에서 실패시킬 이유가 없다.
 *
 * @param expenseId 식별자. 수정 요청 경로에 쓴다
 * @param paymentMethodId 수단 식별자. 선택 칸의 현재 값이다
 * @param amount 금액. <b>할부 건이면 그 회차 하나의 금액</b>이다
 * @param paymentDate 결제일 {@code YYYY-MM-DD}
 * @param place 장소
 * @param content 내용
 * @param expendGroupId 지출유형 식별자
 * @param installmentGroupId 할부 그룹. 있으면 할부 건이다
 * @param installmentIndex 할부 회차
 * @param installmentTotal 할부 총 개월. <b>잠긴 칸에 보이는 값</b>이다
 * @param startYearMonth 할부 시작 연월 {@code YYYY-MM}. 잠긴 칸에 보이는 값이다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpenseView(
        Long expenseId,
        Long paymentMethodId,
        Long amount,
        String paymentDate,
        String place,
        String content,
        Long expendGroupId,
        Long installmentGroupId,
        Integer installmentIndex,
        Integer installmentTotal,
        String startYearMonth) {

    /**
     * 할부 건인가. <b>개월 수·시작 연월 칸을 잠그는 근거</b>다.
     *
     * <p>화면이 미리 막아도 <b>백엔드 실패를 함께 다룬다</b> — 주소를 직접 쳐서 올 수 있고,
     * 화면이 막는 것과 서버가 막는 것은 서로를 대신하지 않는다.
     */
    public boolean isInstallment() {
        return installmentGroupId != null;
    }
}

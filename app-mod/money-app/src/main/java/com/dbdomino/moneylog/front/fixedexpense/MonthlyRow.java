package com.dbdomino.moneylog.front.fixedexpense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 그 달에 실제로 잡힌 고정지출 한 행. 4.6 이 보여 주고 고친다.
 *
 * <h2>설정과 다른 저장 단위다</h2>
 *
 * <p>{@link FixedExpenseView} 가 <b>"매달 이만큼 나간다"는 기준값</b>이라면 이것은 <b>그
 * 달에 잡힌 값</b>이다. 둘은 다른 행이고 한쪽을 고쳐도 다른 쪽이 자동으로 따라오지 않는다.
 *
 * <p>그래서 금액의 이름이 다르다 — 저쪽은 <b>기본 금액</b>, 이쪽은 <b>그 달 금액</b>이다.
 * 같은 말로 부르면 사용자는 하나로 읽고 <b>"고쳤는데 지난달이 안 바뀐다"를 버그로
 * 신고한다.</b>
 *
 * <h2>식별자가 셋이다</h2>
 *
 * <p>고정지출 식별자 하나로는 한 행이 특정되지 않는다 — <b>연·월과 함께</b>여야 한다.
 * 단건 수정 주소가 그 셋을 모두 싣는 이유다.
 *
 * <h2>결제일을 화면이 계산하지 않는다</h2>
 *
 * <p>매달 결제일이 그 달 말일보다 크면 <b>서버가 말일로 맞춘다.</b> 화면은 <b>받은 날짜를
 * 그대로</b> 보인다 — 화면이 보정하면 같은 규칙이 두 곳에 생기고, 윤년처럼 드문 달에서
 * 두 값이 갈린다.
 *
 * @param fixedExpenseId 고정지출 식별자. 연·월과 함께 이 행을 가리킨다
 * @param year 연도
 * @param month 월 1~12
 * @param fixedExpenseName 고정지출 이름. 관리 테이블의 <b>현재</b> 값
 * @param amount 그 달 금액. <b>설정의 기본 금액과 다를 수 있다</b>
 * @param paymentDate 그 달 결제일 {@code YYYY-MM-DD}. <b>말일로 보정된 값일 수 있다</b>
 * @param content 그 달 내용
 * @param paymentMethodId 그 달 수단 식별자. 편집 폼의 선택 칸이 쓴다
 * @param paymentMethodName 수단 이름. 현재 값이다
 * @param expendGroupId 그 달 지출유형 식별자
 * @param expendGroupName 지출유형 이름. 현재 값이다
 * @param modified <b>사용자가 그 달 값을 직접 고쳤는가.</b> 반영이 보존할지 정하는 값이다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyRow(
        Long fixedExpenseId,
        Integer year,
        Integer month,
        String fixedExpenseName,
        Long amount,
        String paymentDate,
        String content,
        Long paymentMethodId,
        String paymentMethodName,
        Long expendGroupId,
        String expendGroupName,
        Boolean modified) {

    /**
     * 직접 고친 행인가. <b>「수정됨」 뱃지를 그리는 근거</b>다.
     *
     * <p>구분이 없으면 <b>반영을 누를 때 무엇이 보존되는지 알 수 없고</b>, 「직접 수정분도
     * 되돌리기」가 무엇을 되돌리는지도 이해되지 않는다.
     *
     * <p>템플릿이 값을 직접 비교하지 않게 여기 둔다 — 비교가 흩어지면 목록과 편집 폼이
     * 다른 판단을 하게 된다.
     */
    public boolean isModified() {
        return modified != null && modified;
    }

    /**
     * 상태를 화면에 보일 말로 바꾼다. 고치지 않은 행은 <b>빈 문자열</b>이다.
     *
     * <p>「기본」 같은 말을 채우지 않는다 — 표에서 눈에 띄어야 하는 것은 <b>고친 행</b>
     * 하나뿐이고, 양쪽에 다 말이 붙으면 그 대비가 사라진다.
     */
    public String modifiedLabel() {
        return isModified() ? "수정됨" : "";
    }

    /** 내용을 보일 자리가 있는가. */
    public boolean hasContent() {
        return content != null && !content.isBlank();
    }
}

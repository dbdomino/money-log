package com.dbdomino.moneylog.front.fixedexpense.form;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 4.6 월별 내역의 <b>그 달 한 건</b> 폼. 칸이 넷이다.
 *
 * <h2>이름과 지출유형 자리를 두지 않는다</h2>
 *
 * <p>백엔드가 이 통로로 그 둘을 받지 않는다 — 고정지출 이름과 지출유형은 <b>4.5 에서</b>
 * 바꾼다. 받지 않는 칸을 타입에 두면 <b>언제나 버려지는 값</b>이 생기고, 읽는 사람은 그것이
 * 보내지는 줄 안다. 010 의 소득 폼에서 같은 판단을 했다.
 *
 * <h2>설정 폼과 다른 타입이다</h2>
 *
 * <p>{@link FixedExpenseForm} 은 <b>기준값</b>을 만들고 이것은 <b>그 달에 잡힌 값</b>을
 * 고친다. 금액의 이름부터 다르다 — 저쪽은 「기본 금액」, 이쪽은 「그 달 금액」이다. 한
 * 타입으로 합치면 <b>고치면 어디까지 번지는지가 타입에서 사라진다</b>: 저쪽은 미래 달로
 * 번지고 이쪽은 그 달에서 끝난다.
 *
 * <h2>결제일이 문자열 한 칸이다</h2>
 *
 * <p>설정의 적용 기간은 정수 네 칸인데 여기는 {@code YYYY-MM-DD} 한 칸이다. <b>연·월을
 * 고르는 값이 아니라 그 달 안의 하루</b>이기 때문이며, 그 달 안인지는 화면이 먼저 알리고
 * 서버가 거절한다.
 *
 * @param amount 그 달 금액. 원 단위 양의 정수
 * @param paymentDate 그 달 결제일 {@code YYYY-MM-DD}. <b>그 달 안이어야 한다</b>
 * @param content 그 달 내용
 * @param paymentMethodId 그 달 수단. <b>사용 중 지출용</b> 목록에서 고른다
 */
public record MonthlyRowForm(
        Long amount,
        String paymentDate,
        String content,
        Long paymentMethodId) {

    /**
     * 백엔드 단건 수정 요청 본문. <b>칸 넷만</b> 담는다.
     *
     * <p>순서를 고정하는 이유는 로그에 찍힌 본문을 사람이 눈으로 맞춰 볼 때 순서가 흔들리면
     * 같은 요청이 달라 보이기 때문이다.
     */
    public Map<String, Object> toRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("paymentDate", paymentDate);
        body.put("content", content);
        body.put("paymentMethodId", paymentMethodId);
        return body;
    }
}

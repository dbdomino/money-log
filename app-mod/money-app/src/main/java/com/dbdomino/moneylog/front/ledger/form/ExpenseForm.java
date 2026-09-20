package com.dbdomino.moneylog.front.ledger.form;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 지출 등록·수정 폼. <b>나가는 모양이 둘이다.</b>
 *
 * <p>일시불은 금액과 결제일을 싣고, 할부는 월 납부액과 개월 수와 시작 연월을 싣는다. 금액
 * 칸의 <b>이름까지 다르다</b> — 일시불 폼 그대로 할부를 보내면 형식 오류다.
 *
 * <h2>어느 쪽인지 가리는 판단을 여기 둔다</h2>
 *
 * <p>컨트롤러가 가르면 <b>등록과 수정 두 곳에 같은 판단이 생긴다.</b> 한 곳만 고치면 등록은
 * 올바른 통로로 가는데 수정은 옛 통로로 가고, 증상만 보고는 판단이 두 벌이라는 것을 짐작하기
 * 어렵다.
 *
 * <h2>접는 것은 브라우저, 가르는 것은 여기다</h2>
 *
 * <p>모달은 한 벌이고 결제 방식 토글에 따라 칸을 접고 편다. 그것은 <b>입력 중의 편의</b>이며
 * 보장이 아니다 — 스크립트가 막혀 칸이 접히지 않고 양쪽 값이 다 넘어와도, <b>고른 값을 보고</b>
 * 통로를 가르는 것은 이 클래스다.
 */
public final class ExpenseForm {

    /** 결제 방식. 브라우저가 보내는 값이며 이 둘 말고는 일시불로 본다. */
    public static final String PAY_TYPE_INSTALLMENT = "INSTALLMENT";

    private final String payType;
    private final Long paymentMethodId;
    private final Long expendGroupId;
    private final Long amount;
    private final String paymentDate;
    private final String place;
    private final String content;
    private final Long monthlyAmount;
    private final Integer installmentMonths;
    private final String startYearMonth;

    public ExpenseForm(String payType, Long paymentMethodId, Long expendGroupId, Long amount,
            String paymentDate, String place, String content, Long monthlyAmount,
            Integer installmentMonths, String startYearMonth) {
        this.payType = payType;
        this.paymentMethodId = paymentMethodId;
        this.expendGroupId = expendGroupId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.place = place;
        this.content = content;
        this.monthlyAmount = monthlyAmount;
        this.installmentMonths = installmentMonths;
        this.startYearMonth = startYearMonth;
    }

    /**
     * 할부로 보내야 하는가. <b>사용자가 고른 값 하나로만 가른다.</b>
     *
     * <p>할부 칸이 채워져 있는지로 짐작하지 않는다 — 스크립트가 막힌 환경에서는 일시불을
     * 골라도 할부 칸에 값이 남아 있을 수 있고, 그것을 할부로 읽으면 <b>사용자가 고르지 않은
     * 통로로 나간다.</b>
     */
    public boolean isInstallment() {
        return PAY_TYPE_INSTALLMENT.equals(payType);
    }

    /**
     * 일시불 등록 본문. 할부일 때는 부르지 않는다.
     *
     * <p>금액 칸의 이름이 {@code amount} 다.
     */
    public Map<String, Object> toLumpCreateRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethodId", paymentMethodId);
        body.put("expendGroupId", expendGroupId);
        body.put("amount", amount);
        body.put("paymentDate", paymentDate);
        body.put("place", place);
        body.put("content", content);
        return body;
    }

    /**
     * 할부 등록 본문. 일시불일 때는 부르지 않는다.
     *
     * <p>금액 칸의 이름이 {@code monthlyAmount} 이고 <b>결제일이 없다</b> — 회차는 시작
     * 연월로 만들어진다.
     */
    public Map<String, Object> toInstallmentCreateRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethodId", paymentMethodId);
        body.put("expendGroupId", expendGroupId);
        body.put("monthlyAmount", monthlyAmount);
        body.put("installmentMonths", installmentMonths);
        body.put("startYearMonth", startYearMonth);
        body.put("place", place);
        body.put("content", content);
        return body;
    }

    /**
     * 수정 본문. <b>통로가 하나다</b> — 이미 정해진 건이라 갈릴 것이 없다.
     *
     * <p><b>개월 수와 시작 연월을 싣지 않는다.</b> 화면이 그 칸을 잠그지만 주소로 직접 올 수
     * 있고, 실어 보내면 백엔드가 거절한다. 보내지 않은 칸은 백엔드가 그대로 유지한다.
     *
     * <p>할부 건의 금액을 고치면 <b>그 회차 하나만</b> 바뀐다. 할부 전체가 아니다.
     */
    public Map<String, Object> toUpdateRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethodId", paymentMethodId);
        body.put("expendGroupId", expendGroupId);
        body.put("amount", amount);
        body.put("paymentDate", paymentDate);
        body.put("place", place);
        body.put("content", content);
        return body;
    }
}

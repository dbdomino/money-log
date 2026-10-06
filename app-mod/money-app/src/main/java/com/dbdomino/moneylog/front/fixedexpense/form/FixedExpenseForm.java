package com.dbdomino.moneylog.front.fixedexpense.form;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 고정지출 설정 폼. 등록(4.3)과 수정(4.5)이 같은 모양을 쓴다.
 *
 * <h2>적용 기간 네 칸이 여기 산다</h2>
 *
 * <p>백엔드는 적용 기간을 <b>연과 월을 각각 정수로</b> 받는다 — 시작·종료 두 쌍이라 네
 * 칸이다. 010 의 할부는 문자열 한 칸({@code "2026-01"})이라 <b>같은 저장소 안에서 형식이
 * 갈린다.</b>
 *
 * <p><b>폼 타입이 네 칸을 그대로 들고 있게 해 옮기다 틀릴 자리를 없앤다.</b> 입력 칸도
 * 넷이고 보내는 값도 넷이라 중간에 쪼개거나 합치는 자리가 없다 — 그 자리를 만들면
 * <b>쪼개는 규칙이 또 하나 생기고</b>, 010 을 본 사람이 그쪽 형식으로 보낼 위험이 남는다.
 *
 * <p>할부가 문자열인 이유는 그 값이 「기간의 끝」이 아니라 <b>첫 회차가 떨어지는 한
 * 지점</b>이고 개월 수와 함께 읽히는 단일 좌표라서다. 쌍이 아니므로 나눌 이유가 없다.
 *
 * <h2>등록과 수정이 같은 모양을 보낸다</h2>
 *
 * <p>백엔드 수정은 보내지 않은 항목을 그대로 유지하지만, 이 화면은 <b>폼이 열 칸을 모두
 * 채운 채로 뜬다</b> — 사용자가 본 값이 곧 보낼 값이다. 그래서 등록과 수정이 같은 본문을
 * 만든다.
 *
 * <p>007 의 {@code PatchBody}(빈 칸의 뜻이 칸마다 다른 경우)를 쓰지 않는 이유가 이것이다 —
 * <b>여기에는 "건드리지 않았다"와 "비웠다"의 구분이 없다.</b> 열 칸이 전부 필수라 비울 수
 * 있는 칸 자체가 없다.
 */
public final class FixedExpenseForm {

    private final String name;
    private final Long paymentMethodId;
    private final Long expendGroupId;
    private final Long amount;
    private final Integer paymentDayOfMonth;
    private final String content;
    private final Integer startYear;
    private final Integer startMonth;
    private final Integer endYear;
    private final Integer endMonth;

    public FixedExpenseForm(String name, Long paymentMethodId, Long expendGroupId, Long amount,
            Integer paymentDayOfMonth, String content, Integer startYear, Integer startMonth,
            Integer endYear, Integer endMonth) {
        this.name = name;
        this.paymentMethodId = paymentMethodId;
        this.expendGroupId = expendGroupId;
        this.amount = amount;
        this.paymentDayOfMonth = paymentDayOfMonth;
        this.content = content;
        this.startYear = startYear;
        this.startMonth = startMonth;
        this.endYear = endYear;
        this.endMonth = endMonth;
    }

    /**
     * 등록·수정이 함께 쓰는 본문.
     *
     * <p><b>적용 기간이 정수 네 칸으로 나간다.</b> 문자열로 합치지 않는다 — 합치면
     * 백엔드가 형식 오류로 거절한다.
     */
    public Map<String, Object> toRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("paymentMethodId", paymentMethodId);
        body.put("expendGroupId", expendGroupId);
        body.put("amount", amount);
        body.put("paymentDayOfMonth", paymentDayOfMonth);
        body.put("content", content);
        body.put("startYear", startYear);
        body.put("startMonth", startMonth);
        body.put("endYear", endYear);
        body.put("endMonth", endMonth);
        return body;
    }
}

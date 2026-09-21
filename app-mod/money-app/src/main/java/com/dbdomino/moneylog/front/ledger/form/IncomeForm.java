package com.dbdomino.moneylog.front.ledger.form;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 소득 등록·수정 폼. <b>칸이 넷이다</b> — 수단·금액·결제일·내용.
 *
 * <h2>지출유형·장소 자리를 두지 않는다</h2>
 *
 * <p>백엔드가 소득에 그 칸을 받지 않는다. <b>타입에 두면 언제나 버려지는 값이 생기고</b>,
 * 그 자리를 본 다음 사람이 모달에 칸을 만든다 — 그러면 사용자가 채운 값이 조용히 사라진다.
 *
 * <h2>할부도 없다</h2>
 *
 * <p>소득에 할부라는 개념이 없어 통로가 갈리지 않는다. 지출 폼과 달리 나가는 모양이 하나다.
 */
public final class IncomeForm {

    private final Long paymentMethodId;
    private final Long amount;
    private final String paymentDate;
    private final String content;

    public IncomeForm(Long paymentMethodId, Long amount, String paymentDate, String content) {
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.content = content;
    }

    /** 등록 본문. 내용은 선택이라 비어 있으면 담지 않는다. */
    public Map<String, Object> toCreateRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethodId", paymentMethodId);
        body.put("amount", amount);
        body.put("paymentDate", paymentDate);
        if (content != null && !content.isBlank()) {
            body.put("content", content);
        }
        return body;
    }

    /**
     * 수정 본문.
     *
     * <p><b>내용을 비우면 비우라는 뜻으로 보낸다.</b> 현재 값이 채워진 채로 뜨는 칸이라,
     * 사용자가 지웠다면 지우려는 뜻이다 — 「유지」로 읽으면 사용자는 지우고 저장했는데
     * 그대로 남아 있는 화면을 보게 된다. 007 의 {@code PatchBody} 가 정한 규칙과 같다.
     */
    public Map<String, Object> toUpdateRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethodId", paymentMethodId);
        body.put("amount", amount);
        body.put("paymentDate", paymentDate);
        body.put("content", content == null || content.isBlank() ? null : content);
        return body;
    }
}

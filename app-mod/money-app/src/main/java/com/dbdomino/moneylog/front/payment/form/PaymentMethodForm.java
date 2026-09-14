package com.dbdomino.moneylog.front.payment.form;

import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import com.dbdomino.moneylog.front.support.PatchBody;
import java.util.Map;

/**
 * 2.2 수단 등록과 2.4 수단 수정이 함께 쓰는 폼. 칸 다섯이다.
 *
 * <h2>왜 폼을 나누지 않았나</h2>
 *
 * <p>008 이 폼을 화면별로 나눈 이유는 <b>칸이 달랐기 때문</b>이다 — 가입 폼에 로그인 폼의
 * 규칙이 따라붙으면 로그인이 엉뚱한 이유로 거절된다. 등록과 수정은 칸이 똑같아 나눌 근거가
 * 없다.
 *
 * <p>대신 <b>나가는 모양이 둘</b>이다. 등록은 전체를 싣고, 수정은 바뀐 항목만 싣는 본문으로
 * 옮긴다.
 *
 * <h2>계좌면 유효기간을 뺀다</h2>
 *
 * <p>브라우저가 칸을 접어도 <b>보장은 여기가 한다</b>. 스크립트가 막힌 환경에서는 접히지 않은
 * 칸의 값이 그대로 나가는데, 백엔드가 지금은 그것을 무시하지만 계약이 "무시한다"에서
 * "거절한다"로 바뀌면 화면만 깨진다.
 *
 * @param name 수단 이름 (필수)
 * @param type 구분 (필수). 카드 또는 계좌
 * @param purpose 용도 (필수). 지출용 또는 소득용
 * @param inUse 사용 여부 (필수)
 * @param cardExpiry 카드 유효기간 {@code YYYY-MM}. 구분이 계좌면 보내지 않는다
 */
public record PaymentMethodForm(
        String name,
        String type,
        String purpose,
        Boolean inUse,
        String cardExpiry) {

    /** 백엔드 생성 요청. 새로 만드는 자리라 전체를 싣는다. */
    public Request toCreateRequest() {
        return new Request(name, type, purpose, inUse, expiryToSend());
    }

    /**
     * 백엔드 수정 요청. 008 이 만든 장치로 옮긴다.
     *
     * <p>칸 넷은 필수라 언제나 싣고, 유효기간만 구분에 따라 갈린다 — 계좌면 <b>비우라는 값</b>
     * 으로 싣는다. 싣지 않으면 카드에서 계좌로 바꿨을 때 옛 유효기간이 그대로 남는다.
     */
    public Map<String, Object> toUpdateRequest() {
        PatchBody body = new PatchBody()
                .always("name", name)
                .always("type", type)
                .always("purpose", purpose)
                .always("inUse", inUse);

        if (isCard()) {
            body.clearIfBlank("cardExpiry", cardExpiry);
        } else {
            body.always("cardExpiry", null);
        }
        return body.toMap();
    }

    private boolean isCard() {
        return PaymentMethodView.TYPE_CARD.equals(type);
    }

    /** 구분이 카드일 때만 유효기간을 내보낸다. 계좌면 {@code null} 이다. */
    private String expiryToSend() {
        if (!isCard() || cardExpiry == null || cardExpiry.isBlank()) {
            return null;
        }
        return cardExpiry;
    }

    /** 백엔드 {@code PaymentMethodCreate} 요청 본문. */
    public record Request(
            String name,
            String type,
            String purpose,
            Boolean inUse,
            String cardExpiry) {
    }
}

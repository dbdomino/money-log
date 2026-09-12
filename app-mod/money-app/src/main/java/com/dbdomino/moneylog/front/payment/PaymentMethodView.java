package com.dbdomino.moneylog.front.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 화면이 보여 주는 지출·소득 수단. 목록(2.1)과 모달 셋(2.2~2.4)이 같은 모양을 쓴다.
 *
 * <h2>백엔드 값을 말로 바꾸는 자리가 여기다</h2>
 *
 * <p>구분과 용도는 백엔드에서 영문 값으로 오는데 <b>화면 밖의 값</b>이다. 바꾸는 자리를 한
 * 곳에 두면 목록·상세·수정이 같은 문구를 쓴다 — 화면마다 따로 바꾸면 어느 화면에서는
 * 「카드」이고 다른 화면에서는 「신용카드」가 된다.
 *
 * <h2>참조 건수를 담지 않는다</h2>
 *
 * <p>백엔드가 목록에 그 값을 주지 않는다. 자리를 두면 채우려고 다른 API 를 찾게 되고, 찾을
 * 수 없어 언제나 비어 있게 된다. <b>용도를 바꿀 수 있는지는 저장해 봐야 안다.</b>
 *
 * @param paymentMethodId 식별자. 주소와 요청 경로에만 쓰고 화면에 보이지 않는다
 * @param name 수단 이름
 * @param type 구분. {@code CARD} 또는 {@code ACCOUNT}
 * @param purpose 용도. {@code EXPENSE} 또는 {@code INCOME}
 * @param inUse 사용 여부. <b>되돌릴 수 있는</b> 상태다
 * @param cardExpiry 카드 유효기간 {@code YYYY-MM}. 구분이 계좌면 {@code null}
 * @param deleted 삭제 표시 여부. <b>되돌릴 수 없는</b> 상태다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentMethodView(
        Long paymentMethodId,
        String name,
        String type,
        String purpose,
        Boolean inUse,
        String cardExpiry,
        Boolean deleted) {

    /** 백엔드가 쓰는 구분 값. 화면은 이 문자열을 사용자에게 보이지 않는다. */
    public static final String TYPE_CARD = "CARD";
    public static final String TYPE_ACCOUNT = "ACCOUNT";

    /** 백엔드가 쓰는 용도 값. */
    public static final String PURPOSE_EXPENSE = "EXPENSE";
    public static final String PURPOSE_INCOME = "INCOME";

    public boolean isCard() {
        return TYPE_CARD.equals(type);
    }

    /** 구분을 화면에 보일 말로 바꾼다. */
    public String typeLabel() {
        return isCard() ? "카드" : "계좌";
    }

    /** 용도를 화면에 보일 말로 바꾼다. */
    public String purposeLabel() {
        return PURPOSE_INCOME.equals(purpose) ? "소득용" : "지출용";
    }

    /** 사용 여부를 화면에 보일 말로 바꾼다. 값이 없으면 사용 중으로 본다. */
    public String inUseLabel() {
        return inUse == null || inUse ? "사용" : "사용 안 함";
    }

    /** 삭제 여부를 화면에 보일 말로 바꾼다. */
    public String statusLabel() {
        return isDeleted() ? "삭제됨" : "정상";
    }

    /**
     * 삭제 표시됐는가. 목록이 이 값으로 <b>수정·삭제 버튼을 그릴지</b> 정한다.
     *
     * <p>되돌릴 수 없는 상태라 같은 동작을 다시 권하지 않는다.
     */
    public boolean isDeleted() {
        return deleted != null && deleted;
    }

    /** 유효기간을 보일 자리가 있는가. 계좌에는 그 값 자체가 없다. */
    public boolean hasCardExpiry() {
        return isCard() && cardExpiry != null && !cardExpiry.isBlank();
    }
}

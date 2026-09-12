package com.dbdomino.moneylog.front.payment;

import java.util.List;

/**
 * 수단 시험이 함께 쓰는 자료.
 *
 * <p>목록에 <b>정상·사용 안 함·삭제됨 세 가지</b>를 함께 담는다. 화면이 셋을 구분해 보이는
 * 것이 이 기능의 요점이라, 시험마다 다른 자료를 세우면 어느 시험이 무엇을 보는지 흐려진다.
 */
final class PaymentMethodFixture {

    private PaymentMethodFixture() {
    }

    /** 정상·사용 중인 카드. 수정과 삭제를 열 수 있다. */
    static PaymentMethodView card() {
        return new PaymentMethodView(1L, "국민카드", PaymentMethodView.TYPE_CARD,
                PaymentMethodView.PURPOSE_EXPENSE, true, "2027-05", false);
    }

    /** 사용 안 함으로 꺼 둔 계좌. <b>되돌릴 수 있는</b> 상태다. */
    static PaymentMethodView unusedAccount() {
        return new PaymentMethodView(2L, "묵은통장", PaymentMethodView.TYPE_ACCOUNT,
                PaymentMethodView.PURPOSE_INCOME, false, null, false);
    }

    /** 삭제 표시된 수단. <b>되돌릴 수 없는</b> 상태이며 상세만 열 수 있다. */
    static PaymentMethodView deleted() {
        return new PaymentMethodView(3L, "없앤카드", PaymentMethodView.TYPE_CARD,
                PaymentMethodView.PURPOSE_EXPENSE, true, "2025-01", true);
    }

    static PaymentMethodListResult page() {
        return new PaymentMethodListResult(List.of(card(), unusedAccount(), deleted()));
    }
}

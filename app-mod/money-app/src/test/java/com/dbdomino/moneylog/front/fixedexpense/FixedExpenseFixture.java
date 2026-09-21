package com.dbdomino.moneylog.front.fixedexpense;

import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupResponse;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import java.util.List;

/**
 * 고정지출 시험이 함께 쓰는 자료.
 *
 * <p><b>수단 이름을 목록마다 다르게 두었다.</b> 설정이 들고 온 이름(「국민카드」)과 사용 중
 * 목록의 이름(「국민카드(새이름)」)이 다르다 — 화면이 이름을 <b>다시 조회해 덮어쓰는지</b>
 * 보는 자리다. 덮어쓰면 목록의 수단 열이 선택지의 이름으로 바뀐다.
 *
 * <p>적용 기간은 <b>정수 네 칸</b>으로 둔다. 시험이 문자열이 새어 들어오지 않는지 본다.
 */
final class FixedExpenseFixture {

    private FixedExpenseFixture() {
    }

    /** 적용 기간이 한 해 전체인 설정. */
    static FixedExpenseView monthlyRent() {
        return new FixedExpenseView(1L, "월세", 1L, "국민카드", 800_000L, 5,
                "원룸 월세", 2L, "주거", 2026, 1, 2026, 12);
    }

    /** 적용 기간이 반년인 설정. 기간 표시가 달라지는지 본다. */
    static FixedExpenseView internet() {
        return new FixedExpenseView(2L, "인터넷", 1L, "국민카드", 35_000L, 15,
                "기가 인터넷", 3L, "통신", 2026, 7, 2026, 12);
    }

    /** 한 쪽에 두 건, 전체는 스물다섯 건. 쪽 넘기기를 보는 자료다. */
    static FixedExpenseListResult page(int offset, int limit) {
        return new FixedExpenseListResult(List.of(monthlyRent(), internet()), offset, limit, 25L);
    }

    static FixedExpenseListResult firstPage() {
        return page(0, 10);
    }

    // ── 선택 목록 ───────────────────────────────────────────────────────

    /**
     * 사용 중인 지출용 수단.
     *
     * <p><b>설정이 들고 온 이름과 일부러 다르게</b> 두었다 — 화면이 이름을 덮어쓰면
     * 목록의 수단 열이 이 이름으로 바뀐다.
     */
    static PaymentMethodListResult activePaymentMethods() {
        return new PaymentMethodListResult(List.of(
                new PaymentMethodView(1L, "국민카드(새이름)", PaymentMethodView.TYPE_CARD,
                        PaymentMethodView.PURPOSE_EXPENSE, true, "2027-05", false)));
    }

    /** 사용 중인 지출유형. */
    static ExpendGroupListResult activeExpendGroups() {
        return new ExpendGroupListResult(List.of(
                new ExpendGroupResponse(2L, "주거", true, null, true, false),
                new ExpendGroupResponse(3L, "통신", true, null, true, false)));
    }
}

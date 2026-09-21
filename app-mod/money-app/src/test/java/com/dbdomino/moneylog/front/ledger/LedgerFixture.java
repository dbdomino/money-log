package com.dbdomino.moneylog.front.ledger;

import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupResponse;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import java.util.List;

/**
 * 가계부 시험이 함께 쓰는 자료.
 *
 * <p>목록에 <b>네 종류를 함께</b> 담는다. 한 목록에 종류가 섞이는 것이 이 기능의 요점이라,
 * 시험마다 다른 자료를 세우면 어느 시험이 무엇을 보는지 흐려진다.
 *
 * <p><b>비는 칸을 일부러 비워 두었다.</b> 소득 행에는 지출유형·장소가 없고 고정지출 행에는
 * 장소가 없다 — 화면이 그 자리에 말을 지어내지 않는지 보는 것이 시험의 목적이다.
 */
final class LedgerFixture {

    static final int YEAR = 2026;
    static final int MONTH = 7;

    private LedgerFixture() {
    }

    /** 일반 지출. 수정·삭제를 열 수 있고 중도상환은 없다. */
    static LedgerRow expense() {
        return new LedgerRow("expense:101", LedgerRow.TYPE_EXPENSE, 101L, "2026-07-02",
                9_500L, 1L, "국민카드", 5L, "식비", "회사 근처", "김치찌개",
                null, null, null, null);
    }

    /** 할부 지출. <b>중도상환이 하나 더 있다.</b> */
    static LedgerRow installment() {
        return new LedgerRow("expense:205", LedgerRow.TYPE_INSTALLMENT, 205L, "2026-07-15",
                150_000L, 1L, "국민카드", 6L, "쇼핑", "가전매장", "노트북",
                null, 50L, 3, 12);
    }

    /** 소득. <b>지출유형과 장소가 없다</b> — 응답에 없는 것이 정상이다. */
    static LedgerRow income() {
        return new LedgerRow("income:501", LedgerRow.TYPE_INCOME, 501L, "2026-07-25",
                3_500_000L, 3L, "월급통장", null, null, null, "7월 급여",
                null, null, null, null);
    }

    /**
     * 고정지출. <b>수단·유형 이름이 조회 시점 현재 이름</b>이다.
     *
     * <p>다른 세 행과 수단이 같은 것(1번)을 가리키면서 이름만 다르게 두었다 — 009 에서
     * 이름을 바꾼 뒤의 상태를 본뜬 것이며, 화면이 이름을 맞추려 드는지 보는 자리다.
     */
    static LedgerRow fixed() {
        return new LedgerRow("fixed:1:2026:7", LedgerRow.TYPE_FIXED, 1L, "2026-07-05",
                500_000L, 1L, "새이름카드", 2L, "주거", null, "원룸 월세",
                "월세", null, null, null);
    }

    /** 네 종류가 섞인 한 달. 합계는 <b>목록의 합과 일부러 다르게</b> 두었다. */
    static LedgerMonth month() {
        return new LedgerMonth(YEAR, MONTH, 659_500L, 3_500_000L,
                List.of(income(), fixed(), installment(), expense()));
    }

    /**
     * 필터를 건 뒤의 한 달. <b>목록만 줄고 합계는 그대로</b>다.
     *
     * <p>이것이 이 화면의 핵심이다 — 화면이 목록을 다시 더해 맞추면 사용자는 자기가 쓴 돈이
     * 줄었다고 읽는다.
     */
    static LedgerMonth filteredMonth() {
        return new LedgerMonth(YEAR, MONTH, 659_500L, 3_500_000L, List.of(expense()));
    }

    /** 거래가 없는 달. 합계 0 과 빈 목록이 함께 온다. */
    static LedgerMonth emptyMonth() {
        return LedgerMonth.empty(YEAR, MONTH);
    }

    // ── 선택 목록 ───────────────────────────────────────────────────────

    /** 사용 중인 지출용 수단. */
    static PaymentMethodListResult expensePaymentMethods() {
        return new PaymentMethodListResult(List.of(
                new PaymentMethodView(1L, "국민카드", PaymentMethodView.TYPE_CARD,
                        PaymentMethodView.PURPOSE_EXPENSE, true, "2027-05", false)));
    }

    /** 사용 중인 소득용 수단. */
    static PaymentMethodListResult incomePaymentMethods() {
        return new PaymentMethodListResult(List.of(
                new PaymentMethodView(3L, "월급통장", PaymentMethodView.TYPE_ACCOUNT,
                        PaymentMethodView.PURPOSE_INCOME, true, null, false)));
    }

    /** 사용 중인 지출유형. */
    static ExpendGroupListResult expendGroups() {
        return new ExpendGroupListResult(List.of(
                new ExpendGroupResponse(5L, "식비", true, null, true, false),
                new ExpendGroupResponse(6L, "쇼핑", true, null, false, false)));
    }
}

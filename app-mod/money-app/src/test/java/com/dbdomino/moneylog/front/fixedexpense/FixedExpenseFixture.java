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

    // ── 그 달 내역 ──────────────────────────────────────────────────────

    /**
     * 2026년 7월 내역.
     *
     * <p><b>합계를 목록의 합과 일부러 다르게</b> 두었다 — 행의 합은 555,000 인데 합계는
     * 900,000 이다. 상단 합계는 <b>그 달 전체 기준</b>이고 목록은 좁혀질 수 있어 둘은 다를
     * 수 있다. 화면이 목록을 다시 더해 맞추면 이 자료에서 두 값이 갈린다.
     *
     * <p>둘째 행만 <b>직접 고친 행</b>이다. 뱃지가 고친 행에만 붙는지 본다.
     */
    static MonthlyResult monthly() {
        return new MonthlyResult(2026, 7, 900_000L, List.of(
                new MonthlyRow(1L, 2026, 7, "월세", 500_000L, "2026-07-05", "원룸 월세",
                        1L, "국민카드", 2L, "주거", false),
                new MonthlyRow(2L, 2026, 7, "통신비", 55_000L, "2026-07-25", "휴대폰 요금",
                        1L, "국민카드", 3L, "통신", true)));
    }

    /** 단건 수정 뒤의 그 달. 첫 행에 직접 수정 표시가 켜져 있다. */
    static MonthlyResult monthlyAfterUpdate() {
        return new MonthlyResult(2026, 7, 900_000L, List.of(
                new MonthlyRow(1L, 2026, 7, "월세", 550_000L, "2026-07-10", "7월만 관리비 포함",
                        1L, "국민카드", 2L, "주거", true),
                new MonthlyRow(2L, 2026, 7, "통신비", 55_000L, "2026-07-25", "휴대폰 요금",
                        1L, "국민카드", 3L, "통신", true)));
    }

    /** 적용 기간이 이 달을 포함하지 않는 달. 빈 달 안내를 보는 자료다. */
    static MonthlyResult monthlyEmpty() {
        return MonthlyResult.empty(2026, 7);
    }

    /**
     * 결제일이 <b>말일로 보정된</b> 달.
     *
     * <p>매달 결제일이 31 인데 2026년 2월은 28일까지다. 서버가 맞춘 값이며 화면은 받은
     * 날짜를 그대로 보인다 — 화면이 보정하면 같은 규칙이 두 곳에 생긴다.
     */
    static MonthlyResult monthlyAdjusted() {
        return new MonthlyResult(2026, 2, 500_000L, List.of(
                new MonthlyRow(1L, 2026, 2, "월세", 500_000L, "2026-02-28", "원룸 월세",
                        1L, "국민카드", 2L, "주거", false)));
    }

    // ── 반영 결과 ───────────────────────────────────────────────────────

    /**
     * 되돌리기를 <b>끄고</b> 반영한 결과. <b>보존이 1</b> 이다.
     *
     * <p>목록에 「전기요금」이 늘어 있다 — 반영이 돌려준 목록으로 갱신하는지 보는 자리다.
     * 조회 자료에는 없는 이름이라 <b>다시 조회하면 사라진다.</b>
     */
    static SyncResult syncKept() {
        return new SyncResult(2026, 7, 1, 1, 0, 1, 955_000L, List.of(
                new MonthlyRow(1L, 2026, 7, "월세", 800_000L, "2026-07-05", "원룸 월세",
                        1L, "국민카드", 2L, "주거", false),
                new MonthlyRow(2L, 2026, 7, "통신비", 55_000L, "2026-07-25", "휴대폰 요금",
                        1L, "국민카드", 3L, "통신", true),
                new MonthlyRow(3L, 2026, 7, "전기요금", 100_000L, "2026-07-20", "여름 전기",
                        1L, "국민카드", 3L, "공과금", false)));
    }

    /**
     * 되돌리기를 <b>켜고</b> 반영한 결과. <b>보존이 0</b> 이다.
     *
     * <p>그 0 이 무엇이 달랐는지를 말해 준다 — 직접 고친 값이 설정 값으로 돌아갔다.
     */
    static SyncResult syncOverwritten() {
        return new SyncResult(2026, 7, 1, 2, 0, 0, 1_000_000L, List.of(
                new MonthlyRow(1L, 2026, 7, "월세", 800_000L, "2026-07-05", "원룸 월세",
                        1L, "국민카드", 2L, "주거", false),
                new MonthlyRow(2L, 2026, 7, "통신비", 100_000L, "2026-07-25", "휴대폰 요금",
                        1L, "국민카드", 3L, "통신", false),
                new MonthlyRow(3L, 2026, 7, "전기요금", 100_000L, "2026-07-20", "여름 전기",
                        1L, "국민카드", 3L, "공과금", false)));
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

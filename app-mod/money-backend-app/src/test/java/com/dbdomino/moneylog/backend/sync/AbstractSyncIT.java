package com.dbdomino.moneylog.backend.sync;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.time.YearMonth;
import tools.jackson.databind.JsonNode;

/**
 * 4.4 자동 반영과 4.9 재작성 통합 테스트의 공통 바탕 (US4).
 *
 * <h2>연월을 상대값으로 잡는다</h2>
 *
 * <p>이 스토리는 <b>"지금이 언제인가"에 답이 달려 있다</b>. 미래 달은 따라가고 지난 달·이번
 * 달은 그대로여야 하는데(FR-412), 시험에 {@code 2026-11} 같은 고정값을 박으면 그 날짜가
 * 지나는 순간 "미래 달"이 "지난 달"이 되어 <b>시험이 조용히 반대를 검증한다.</b>
 *
 * <p>그래서 {@link #lastMonth()}·{@link #thisMonth()}·{@link #futureMonth(int)} 로
 * {@code YearMonth.now()} 기준 상대값을 만든다 — 004 의
 * {@code startYearMonthSoThatTodayIs} 와 같은 처방이다.
 *
 * <p><b>적용 기간도 함께 넓혀야 한다.</b> 지난 달부터 먼 미래까지 걸쳐 두지 않으면
 * 검증하려는 달이 기간 밖이라 애초에 만들어지지 않는다.
 */
abstract class AbstractSyncIT extends AbstractApiIT {

    protected static final String FIXED_URL = "/api/v1/fixed-expenses";
    protected static final String MONTHLY_URL = "/api/v1/fixed-expenses/monthly";
    protected static final String SYNC_URL = "/api/v1/fixed-expenses/monthly/sync";

    /** 지난 달. 자동 반영이 <b>건드리지 않아야</b> 하는 달이다(#32). */
    protected static YearMonth lastMonth() {
        return YearMonth.now().minusMonths(1);
    }

    /**
     * 이번 달. <b>미래가 아니다</b> — 자동 반영이 건드리지 않아야 한다(#33).
     *
     * <p>이 경계가 판단이 필요했던 지점이다. 포함하면 월세를 올렸을 때 사용자가 이미 본
     * 이번 달 금액이 소급해 바뀐다.
     */
    protected static YearMonth thisMonth() {
        return YearMonth.now();
    }

    /** {@code monthsAhead} 달 뒤. 자동 반영 대상이다(#30·#31). */
    protected static YearMonth futureMonth(int monthsAhead) {
        return YearMonth.now().plusMonths(monthsAhead);
    }

    /** 지난 달부터 12개월 뒤까지 — 위 셋이 전부 기간 안에 들어온다. */
    protected static String wideStart() {
        return lastMonth().toString();
    }

    /** @see #wideStart() */
    protected static String wideEnd() {
        return futureMonth(12).toString();
    }

    /** 지난 달·이번 달·미래 달을 모두 덮는 설정 1건이 선 회원. */
    protected record Fixture(Member member, long paymentMethodId, long expendGroupId,
                             long fixedExpenseId) {

        String token() {
            return member.token();
        }
    }

    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        long paymentMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        long expendGroupId = defaultGroupId(member, "주거");
        long fixedExpenseId = createFixedExpense(member.token(), "월세", paymentMethodId,
                expendGroupId, 500000L, 25, wideStart(), wideEnd());
        return new Fixture(member, paymentMethodId, expendGroupId, fixedExpenseId);
    }

    /**
     * 그 달을 <b>열어서</b> 월별 내역을 만든다.
     *
     * <p>자동 반영·재작성은 이미 만들어진 행을 대상으로 하므로, 시험은 먼저 대상 달들을
     * 열어 둬야 한다. lazy 생성은 "처음 열 때"만 일어난다(FR-406).
     */
    protected void openMonth(Fixture fixture, YearMonth yearMonth) throws Exception {
        JsonNode response = getJson(MONTHLY_URL + "?year=" + yearMonth.getYear()
                + "&month=" + yearMonth.getMonthValue(), fixture.token());
        if (resCode(response) != 200) {
            throw new IllegalStateException("월 열기 실패(%s): %s".formatted(yearMonth, response));
        }
    }

    /** 지난 달·이번 달·미래 달 셋을 모두 열어 둔다. US4 시나리오의 공통 준비다. */
    protected void openPastPresentFuture(Fixture fixture) throws Exception {
        openMonth(fixture, lastMonth());
        openMonth(fixture, thisMonth());
        openMonth(fixture, futureMonth(1));
        openMonth(fixture, futureMonth(2));
    }

    /** 그 달 행의 금액. 자동 반영이 따라갔는지 보는 가장 짧은 방법이다. */
    protected long amountOf(Fixture fixture, YearMonth yearMonth) {
        return ((Number) monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                yearMonth.getYear(), yearMonth.getMonthValue()).get("amount")).longValue();
    }

    /** 그 달 행의 {@code modified}. */
    protected boolean modifiedOf(Fixture fixture, YearMonth yearMonth) {
        return Boolean.TRUE.equals(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(),
                yearMonth.getYear(), yearMonth.getMonthValue()).get("modified"));
    }
}

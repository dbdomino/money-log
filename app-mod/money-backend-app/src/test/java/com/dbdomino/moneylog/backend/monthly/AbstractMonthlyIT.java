package com.dbdomino.moneylog.backend.monthly;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import tools.jackson.databind.JsonNode;

/**
 * 4.5·4.6 월별 고정지출 내역 통합 테스트의 공통 바탕 (US2·US3).
 *
 * <p><b>조회가 쓰기를 일으킨다.</b> {@link #listMonthly} 를 부르는 것 자체가 그 달의
 * 내역을 만든다(FR-406). 그래서 "만들어졌는가"를 확인하려면 <b>조회 전후로</b>
 * {@code countMonthly} 를 봐야 하고, 응답 목록의 길이만 보면 필터가 걸렸을 때 답이 갈린다.
 */
abstract class AbstractMonthlyIT extends AbstractApiIT {

    protected static final String LIST_URL = "/api/v1/fixed-expenses/monthly";

    /** 적용 기간 — 해를 넘긴다. 기간 안: 2026-11·12, 2027-01·02. 밖: 2026-10, 2027-03. */
    protected static final String START = "2026-11";

    /** @see #START */
    protected static final String END = "2027-02";

    /** 설정 1건이 선 회원. */
    protected record Fixture(Member member, long paymentMethodId, long expendGroupId,
                             long fixedExpenseId) {

        String token() {
            return member.token();
        }
    }

    /**
     * 지출용 수단 하나와 고정지출 설정 하나를 만든다. <b>월별 내역은 아직 0건이다.</b>
     *
     * @param paymentDayOfMonth 결제일. 말일 보정 시험은 31 을 넘긴다
     */
    protected Fixture prepare(int paymentDayOfMonth) throws Exception {
        Member member = signupAndLogin();
        long paymentMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        long expendGroupId = defaultGroupId(member, "주거");
        long fixedExpenseId = createFixedExpense(member.token(), "월세", paymentMethodId,
                expendGroupId, 500000L, paymentDayOfMonth, START, END);
        return new Fixture(member, paymentMethodId, expendGroupId, fixedExpenseId);
    }

    /** 결제일 25일. 말일 보정이 걸리지 않는 평범한 값이다. */
    protected Fixture prepare() throws Exception {
        return prepare(25);
    }

    /**
     * 적용 기간을 <b>2026-01 ~ 2028-12</b> 로 넓게 잡는다.
     *
     * <p>말일 보정 시험이 2026-02(평년)·2028-02(윤년)·30일 달·31일 달을 모두 열어야 하는데,
     * {@link #START}~{@link #END} 는 넉 달이라 대상이 들어오지 않는다.
     */
    protected Fixture prepareWide(int paymentDayOfMonth) throws Exception {
        Member member = signupAndLogin();
        long paymentMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        long expendGroupId = defaultGroupId(member, "주거");
        long fixedExpenseId = createFixedExpense(member.token(), "월세", paymentMethodId,
                expendGroupId, 500000L, paymentDayOfMonth, "2026-01", "2028-12");
        return new Fixture(member, paymentMethodId, expendGroupId, fixedExpenseId);
    }

    /** 그 회원에게 고정지출 설정을 하나 더 붙인다. 필터·다건 생성 시험이 쓴다. */
    protected long addFixedExpense(Fixture fixture, String name, long paymentMethodId,
                                   long amount) throws Exception {
        return createFixedExpense(fixture.token(), name, paymentMethodId,
                fixture.expendGroupId(), amount, 10, START, END);
    }

    /** 4.5 조회. <b>이 호출이 그 달의 내역을 만든다.</b> */
    protected JsonNode listMonthly(Fixture fixture, int year, int month) throws Exception {
        return getJson(LIST_URL + "?year=" + year + "&month=" + month, fixture.token());
    }

    /** 필터를 건 4.5 조회. 생성 대상은 좁아지지 않는다(FR-406) — 결과만 좁아진다. */
    protected JsonNode listMonthly(Fixture fixture, int year, int month, String filter)
            throws Exception {
        return getJson(LIST_URL + "?year=" + year + "&month=" + month + "&" + filter,
                fixture.token());
    }
}

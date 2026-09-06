package com.dbdomino.moneylog.backend.fixedexpense;

import com.dbdomino.moneylog.backend.AbstractApiIT;

/**
 * 4.1~4.4·4.7 고정지출 설정 통합 테스트의 공통 바탕 (US1).
 *
 * <p><b>고정된 미래 연월을 쓴다.</b> 설정 CRUD 는 "지금이 언제인가"와 무관하다 — 자동
 * 반영(FR-412)만이 현재 연월을 보는데 그 검증은 US4 의 {@code sync} 패키지가 맡는다.
 * 여기서 상대 연월을 쓰면 시험이 읽기 어려워지기만 한다.
 */
abstract class AbstractFixedExpenseIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/fixed-expenses";

    /** 적용 기간의 기본값 — <b>해를 넘긴다</b>. 합성 비교가 틀리면 여기서 걸린다. */
    protected static final String START = "2026-11";

    /** @see #START */
    protected static final String END = "2027-02";

    /** 설정을 만들 준비가 끝난 회원 — 지출용 수단 하나와 가입이 만든 기본 유형을 갖는다. */
    protected record Fixture(Member member, long paymentMethodId, long expendGroupId) {

        String token() {
            return member.token();
        }
    }

    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        long paymentMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        return new Fixture(member, paymentMethodId, defaultGroupId(member, "주거"));
    }

    /** 기본값으로 설정 1건. 개별 값이 중요하지 않은 시험이 쓴다. */
    protected long createDefaultFixedExpense(Fixture fixture) throws Exception {
        return createFixedExpense(fixture.token(), "월세", fixture.paymentMethodId(),
                fixture.expendGroupId(), 500000L, 25, START, END);
    }
}

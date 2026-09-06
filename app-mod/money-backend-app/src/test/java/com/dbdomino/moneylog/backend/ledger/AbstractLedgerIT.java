package com.dbdomino.moneylog.backend.ledger;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * 4.8 월별 가계부 통합 목록 테스트의 공통 바탕 (US5).
 *
 * <p><b>네 종류를 한 달에 모아야 시험이 성립한다</b> — 일반 지출·할부·소득은 004 가,
 * 고정지출은 005 가 만든다. {@link #prepare()} 가 넷을 각 1건씩 세워 SC-408 의 전제를
 * 만든다.
 *
 * <p><b>고정지출 행은 조회가 만든다.</b> 설정만 만들어 두면 목록에 아직 없고, 4.8 을
 * 부르는 순간 생긴다(FR-418). 그래서 "4건"은 첫 조회의 결과다.
 */
abstract class AbstractLedgerIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/ledger/monthly";
    protected static final String EXPENSE_URL = "/api/v1/expenses";
    protected static final String INCOME_URL = "/api/v1/incomes";

    /** 조회 대상 달. 004 의 지출·소득을 이 달에 넣는다. */
    protected static final int YEAR = 2026;

    /** @see #YEAR */
    protected static final int MONTH = 7;

    /** 네 종류가 한 건씩 선 회원. */
    protected record Fixture(Member member, long expenseMethodId, long incomeMethodId,
                             long expendGroupId, long expenseId, long incomeId,
                             long installmentGroupId, long fixedExpenseId) {

        String token() {
            return member.token();
        }
    }

    /**
     * 일반 지출 1 · 할부 1 · 소득 1 · 고정지출 설정 1 을 만든다.
     *
     * <p>할부는 3회차를 {@code 2026-07} 부터 걸어 첫 회차가 대상 달에 떨어지게 한다.
     * 고정지출은 대상 달을 포함하는 기간으로 만든다.
     */
    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        String token = member.token();
        long expenseMethodId = createExpensePaymentMethod(token, "국민카드");
        long incomeMethodId = createIncomePaymentMethod(token, "월급통장");
        long expendGroupId = defaultGroupId(member, "식비");

        JsonNode expense = postJson(EXPENSE_URL, token, """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000,
                 "paymentDate":"2026-07-15","place":"편의점","content":"점심"}
                """.formatted(expenseMethodId, expendGroupId));
        require(expense, "일반 지출");

        JsonNode installment = postJson(EXPENSE_URL + "/installments", token, """
                {"paymentMethodId":%d,"expendGroupId":%d,"monthlyAmount":100000,
                 "installmentMonths":3,"startYearMonth":"2026-07",
                 "place":"백화점","content":"노트북 할부"}
                """.formatted(expenseMethodId, expendGroupId));
        require(installment, "할부");

        JsonNode income = postJson(INCOME_URL, token, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-07-25","content":"급여"}
                """.formatted(incomeMethodId));
        require(income, "소득");

        long fixedExpenseId = createFixedExpense(token, "월세", expenseMethodId,
                defaultGroupId(member, "주거"), 500000L, 25, "2026-07", "2026-12");

        return new Fixture(member, expenseMethodId, incomeMethodId, expendGroupId,
                expense.get("data").get("expenseId").asLong(),
                income.get("data").get("incomeId").asLong(),
                installment.get("data").get("installmentGroupId").asLong(),
                fixedExpenseId);
    }

    private void require(JsonNode response, String what) {
        if (resCode(response) != 200) {
            throw new IllegalStateException(what + " 준비 실패: " + response);
        }
    }

    /** 4.8 조회. <b>이 호출이 그 달의 고정지출 내역을 만든다</b>(FR-418). */
    protected JsonNode ledger(Fixture fixture) throws Exception {
        return getJson(URL + "?year=" + YEAR + "&month=" + MONTH, fixture.token());
    }

    /** 필터를 건 4.8 조회. */
    protected JsonNode ledger(Fixture fixture, String query) throws Exception {
        return getJson(URL + "?year=" + YEAR + "&month=" + MONTH + "&" + query, fixture.token());
    }

    /** 응답 목록의 {@code type} 들. 순서를 그대로 유지한다 — 정렬 시험이 쓴다. */
    protected List<String> typesOf(JsonNode response) {
        List<String> types = new ArrayList<>();
        for (JsonNode item : response.get("data").get("list")) {
            types.add(item.get("type").asString());
        }
        return types;
    }

    /** 응답 목록의 {@code ledgerItemId} 들. */
    protected List<String> itemIdsOf(JsonNode response) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : response.get("data").get("list")) {
            ids.add(item.get("ledgerItemId").asString());
        }
        return ids;
    }

    /** 그 종류의 첫 행. 없으면 {@code null} 이다. */
    protected JsonNode firstOfType(JsonNode response, String type) {
        for (JsonNode item : response.get("data").get("list")) {
            if (type.equals(item.get("type").asString())) {
                return item;
            }
        }
        return null;
    }
}

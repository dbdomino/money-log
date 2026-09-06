package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.time.YearMonth;
import tools.jackson.databind.JsonNode;

/**
 * 5.5·5.6 통계 통합 테스트의 공통 바탕 (US2·US3).
 *
 * <h2>연월을 상대값으로 잡는다</h2>
 *
 * <p>통계 저장은 <b>"지금이 언제인가"에 답이 달려 있다</b> — 현재 연월을 <b>초과</b>하는
 * 달만 {@code 3604} 로 거절하고 이번 달은 저장할 수 있다(FR-527). 시험에 고정값을 박으면
 * 그 날짜가 지나는 순간 "미래 월"이 "지난 달"이 되어 <b>시험이 조용히 반대를 검증한다.</b>
 *
 * <p>005 의 {@code AbstractSyncIT} 와 같은 처방이다.
 *
 * <p>다만 <b>주 경계·계산 시험은 고정 연월</b>({@link #FIXED_YEAR}·{@link #FIXED_MONTH})을
 * 쓴다 — 1일의 요일이 달마다 달라 상대값으로는 기대값을 적을 수 없다.
 */
abstract class AbstractStatisticsIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/statistics/monthly";
    protected static final String SAVE_URL = "/api/v1/statistics/monthly/save";
    protected static final String EXPENSE_URL = "/api/v1/expenses";
    protected static final String INCOME_URL = "/api/v1/incomes";

    /**
     * 계산 시험이 쓰는 고정 연월. <b>2026-07-01 은 수요일</b>이라 첫 주가 짧아진다 —
     * 주 경계 규칙(FR-520)을 거는 데 딱 맞다.
     */
    protected static final int FIXED_YEAR = 2026;

    /** @see #FIXED_YEAR */
    protected static final int FIXED_MONTH = 7;

    /** 이번 달. <b>미래가 아니므로 저장할 수 있다</b>(FR-527). */
    protected static YearMonth thisMonth() {
        return YearMonth.now();
    }

    /** 다음 달. 저장 시도는 {@code 3604} 여야 한다. */
    protected static YearMonth nextMonth() {
        return YearMonth.now().plusMonths(1);
    }

    /** 지난 달. */
    protected static YearMonth lastMonth() {
        return YearMonth.now().minusMonths(1);
    }

    /** 지출·소득·수단·유형이 선 회원. */
    protected record Fixture(Member member, long expenseMethodId, long incomeMethodId,
                             long foodGroupId) {

        String token() {
            return member.token();
        }
    }

    /** 수단 둘과 기본 유형을 세운다. 거래는 만들지 않는다. */
    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        long expenseMethodId = createExpensePaymentMethod(member.token(), "국민카드");
        long incomeMethodId = createIncomePaymentMethod(member.token(), "월급통장");
        return new Fixture(member, expenseMethodId, incomeMethodId,
                defaultGroupId(member, "식비"));
    }

    /** 그 날짜에 지출 1건. */
    protected void addExpense(Fixture fixture, String paymentDate, long amount,
                              long expendGroupId) throws Exception {
        assertThat(resCode(postJson(EXPENSE_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":%d,
                 "paymentDate":"%s","place":"편의점","content":"지출"}
                """.formatted(fixture.expenseMethodId(), expendGroupId, amount, paymentDate))))
                .isEqualTo(200);
    }

    /** 그 날짜에 소득 1건. */
    protected void addIncome(Fixture fixture, String paymentDate, long amount) throws Exception {
        assertThat(resCode(postJson(INCOME_URL, fixture.token(), """
                {"paymentMethodId":%d,"amount":%d,"paymentDate":"%s","content":"급여"}
                """.formatted(fixture.incomeMethodId(), amount, paymentDate)))).isEqualTo(200);
    }

    /** 5.5 조회. {@code view} 를 생략한다(기본 동작). */
    protected JsonNode statistics(Fixture fixture, int year, int month) throws Exception {
        return getJson(URL + "/" + year + "/" + month, fixture.token());
    }

    /** 5.5 조회. {@code view} 를 명시한다. */
    protected JsonNode statistics(Fixture fixture, int year, int month, String view)
            throws Exception {
        return getJson(URL + "/" + year + "/" + month + "?view=" + view, fixture.token());
    }

    /** 5.6 저장. 연·월을 <b>Body</b> 로 보낸다(FR-524). */
    protected JsonNode save(Fixture fixture, int year, int month) throws Exception {
        return postJson(SAVE_URL, fixture.token(), """
                {"year":%d,"month":%d}
                """.formatted(year, month));
    }

    /** 응답의 유형별 요약에서 그 유형의 행. 없으면 {@code null} 이다. */
    protected JsonNode groupSummaryOf(JsonNode response, long expendGroupId) {
        for (JsonNode item : response.get("data").get("expendGroupSummaries")) {
            if (item.get("expendGroupId").asLong() == expendGroupId) {
                return item;
            }
        }
        return null;
    }

    /** 응답의 수단별 요약에서 그 수단의 행. 없으면 {@code null} 이다. */
    protected JsonNode methodSummaryOf(JsonNode response, long paymentMethodId) {
        for (JsonNode item : response.get("data").get("paymentMethodSummaries")) {
            if (item.get("paymentMethodId").asLong() == paymentMethodId) {
                return item;
            }
        }
        return null;
    }
}

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

    /**
     * 저장본 1건을 <b>JDBC 로 직접</b> 만든다.
     *
     * <p>5.6 을 부르지 않는 것은 <b>US2 가 US3 없이 완결되어야 하기</b> 때문이다 —
     * 저장 API 를 아직 만들지 않은 시점에도 "저장본이 있을 때의 조회"를 시험할 수 있어야
     * 한다. 005 에서 {@code insertMonthlyRow} 를 같은 이유로 두었다.
     *
     * <p><b>합계를 일부러 계산값과 다르게 넣는다.</b> 그래야 응답이 저장본에서 왔는지
     * 즉석 계산에서 왔는지가 숫자로 구분된다 — {@code source} 필드만 보면 분기는 맞는데
     * 값을 다른 데서 읽는 구현이 통과한다.
     *
     * <p><b>{@code tx.executeWithoutResult} 안에서 한다.</b> datasource 가
     * {@code auto-commit: false} 라 트랜잭션 밖 갱신은 조용히 사라진다.
     *
     * @return 만들어진 통계 행의 {@code idx}. 상세를 붙일 때 쓴다
     */
    protected long insertStatistics(Member member, int year, int month, long incomeTotal,
                                    long expenseTotal) {
        Long idKey = idKeyOf(member);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_statistics
                    (id_key, year, month, saved_at, income_total, expense_total,
                     fixed_amount, regular_amount, fixed_percent, regular_percent,
                     created_at, updated_at, created_by, updated_by)
                values (?, ?, ?, now(), ?, ?, 0, ?, 0.00, 100.00, now(), now(), ?, ?)
                """, idKey, year, month, incomeTotal, expenseTotal, expenseTotal, idKey, idKey));
        Long idx = jdbc.queryForObject("""
                select idx from moneylog.tbl_statistics
                 where id_key = ? and year = ? and month = ?
                """, Long.class, idKey, year, month);
        return idx == null ? 0L : idx;
    }

    /** 저장본에 주별 상세 1행을 붙인다. 저장본이 <b>저장된 경계를 그대로 쓰는지</b> 보는 데 쓴다. */
    protected void insertStatisticsWeekly(Member member, long statisticsIdx, int weekIndex,
                                          String weekStart, String weekEnd, long amount) {
        Long idKey = idKeyOf(member);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_statistics_weekly
                    (id_key, statistics_idx, week_index, week_start, week_end, amount,
                     created_at, updated_at, created_by, updated_by)
                values (?, ?, ?, cast(? as date), cast(? as date), ?, now(), now(), ?, ?)
                """, idKey, statisticsIdx, weekIndex, weekStart, weekEnd, amount, idKey, idKey));
    }

    /** 그 회원의 그 달 저장본 {@code saved_at}. 없으면 {@code null} 이다. */
    protected java.time.OffsetDateTime savedAtOf(Member member, int year, int month) {
        return jdbc.query("""
                select s.saved_at from moneylog.tbl_statistics s
                  join moneylog.tbl_user u on u.id_key = s.id_key
                 where u.user_id = ? and s.year = ? and s.month = ?
                """, rs -> rs.next()
                ? rs.getObject(1, java.time.OffsetDateTime.class) : null,
                member.memberId(), year, month);
    }

    /** 응답의 주별 배열에서 그 주차의 행. 없으면 {@code null} 이다. */
    protected JsonNode weekOf(JsonNode response, int weekIndex) {
        for (JsonNode item : response.get("data").get("weeklyExpenses")) {
            if (item.get("weekIndex").asInt() == weekIndex) {
                return item;
            }
        }
        return null;
    }

    /** 응답의 비율 객체. */
    protected JsonNode ratioOf(JsonNode response) {
        return response.get("data").get("fixedVsRegularRatio");
    }
}

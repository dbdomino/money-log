package com.dbdomino.moneylog.backend.installment;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * 할부(3.5·3.6) 통합 테스트의 공통 바탕.
 *
 * <h2>실행 날짜에 흔들리지 않게 만든다</h2>
 *
 * <p>중도상환은 <b>서버의 오늘</b>을 기준으로 판정한다(FR-315). 시험이 고정 연월을 쓰면
 * 그 날짜가 지나는 순간 "미래 회차"가 과거가 되어 결과가 바뀐다. 그래서 시작 연월을
 * <b>오늘로부터 상대적으로</b> 계산한다 — {@link #startYearMonthSoThatTodayIs}.
 */
abstract class AbstractInstallmentIT extends AbstractApiIT {

    protected static final String EXPENSE_URL = "/api/v1/expenses";
    protected static final String INSTALLMENT_URL = EXPENSE_URL + "/installments";

    /** 할부를 만들 준비가 끝난 회원. */
    protected record Fixture(Member member, long paymentMethodId, long expendGroupId) {

        String token() {
            return member.token();
        }
    }

    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        long methodId = createExpensePaymentMethod(member.token(), "국민카드");
        return new Fixture(member, methodId, defaultGroupId(member, "식비"));
    }

    /**
     * 회차 {@code n} 이 <b>오늘</b>이 되도록 시작 연월을 잡는다.
     *
     * <p>결제일이 매월 1일이라 "오늘이 결제일인 회차"는 <b>이번 달 1일 회차</b>다. 시작
     * 연월을 이번 달에서 {@code n-1} 개월 뺀 값으로 두면 n 회차가 이번 달 1일이 된다.
     *
     * <p><b>오늘이 1일이 아니어도 된다.</b> 경계 판정은 {@code payment_date > today} 이므로
     * 이번 달 1일 회차는 오늘이 며칠이든 {@code <= today} 라 남는다 — "오늘 회차"라는
     * 표현은 "이번 달 회차"를 뜻한다. 오늘이 정확히 1일이면 말 그대로 오늘이 결제일이다.
     */
    protected String startYearMonthSoThatTodayIs(int index) {
        return YearMonth.now().minusMonths(index - 1L).toString();
    }

    /** 3.5 등록. 성공을 전제하지 않는다. */
    protected JsonNode createInstallment(Fixture fixture, long monthlyAmount, int months,
                                         String startYearMonth) throws Exception {
        return postJson(INSTALLMENT_URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"monthlyAmount":%d,
                 "installmentMonths":%d,"startYearMonth":"%s",
                 "place":"백화점","content":"노트북 할부"}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId(),
                monthlyAmount, months, startYearMonth));
    }

    /** 등록에 성공했다고 보고 그룹 식별자를 꺼낸다. */
    protected long groupIdOf(JsonNode response) {
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("installmentGroupId").asLong();
    }

    /** 그 그룹의 회차를 순번대로 읽는다. */
    protected List<Map<String, Object>> rowsOf(long installmentGroupId) {
        return jdbc.queryForList("""
                select idx, amount, payment_date, installment_group_id,
                       installment_index, installment_total
                  from moneylog.tbl_expense
                 where installment_group_id = ?
                 order by installment_index
                """, installmentGroupId);
    }

    /** 그 그룹에 남은 회차 수. */
    protected int countRows(long installmentGroupId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_expense where installment_group_id = ?",
                Integer.class, installmentGroupId);
        return count == null ? 0 : count;
    }

    /** 그 회원의 지출 건수. 전체 롤백을 확인할 때 쓴다. */
    protected int countExpenses(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_expense e
                  join moneylog.tbl_user u on u.id_key = e.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }

    /** 3.6 중도상환. */
    protected JsonNode settle(Fixture fixture, long installmentGroupId) throws Exception {
        return patchJson(INSTALLMENT_URL + "/" + installmentGroupId + "/remainder",
                fixture.token(), "{}");
    }

    /** 행의 결제일. JDBC 는 {@code date} 를 {@link java.sql.Date} 로 준다. */
    protected LocalDate paymentDateOf(Map<String, Object> row) {
        return ((java.sql.Date) row.get("payment_date")).toLocalDate();
    }
}

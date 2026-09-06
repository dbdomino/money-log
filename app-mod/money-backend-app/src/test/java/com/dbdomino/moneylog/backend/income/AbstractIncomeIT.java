package com.dbdomino.moneylog.backend.income;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * 소득(3.7~3.10) 통합 테스트의 공통 바탕.
 *
 * <p>지출과 달리 <b>지출유형이 필요 없다</b>(FR-306). 준비물은 {@code purpose=INCOME} 인
 * 수단 하나뿐이다 — 그 차이가 이 클래스와 {@code AbstractExpenseIT} 의 차이 전부다.
 */
abstract class AbstractIncomeIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/incomes";

    /** 소득을 만들 준비가 끝난 회원 — 토큰과 소득용 수단을 함께 들고 다닌다. */
    protected record Fixture(Member member, long paymentMethodId) {

        String token() {
            return member.token();
        }
    }

    /** 가입하고 소득용 수단 1건을 만든다. */
    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        return new Fixture(member, createIncomePaymentMethod(member.token(), "월급통장"));
    }

    /** 3.7 등록. Body 를 그대로 받아 <b>명세에 없는 필드를 실어 보내는</b> 시험도 쓸 수 있게 한다. */
    protected JsonNode createRaw(Fixture fixture, String body) throws Exception {
        return postJson(URL, fixture.token(), body);
    }

    /** 3.7 등록 — 기본값으로 1건. */
    protected JsonNode create(Fixture fixture) throws Exception {
        return createRaw(fixture, """
                {"paymentMethodId":%d,"amount":3000000,"paymentDate":"2026-03-25","content":"급여"}
                """.formatted(fixture.paymentMethodId()));
    }

    /** 등록에 성공했다고 보고 그 PK 를 꺼낸다. */
    protected long idOf(JsonNode response) {
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("incomeId").asLong();
    }

    /** 준비용 소득 1건을 만들고 PK 를 돌려준다. */
    protected long createIncome(Fixture fixture) throws Exception {
        return idOf(create(fixture));
    }

    /** 3.8 상세 조회. */
    protected JsonNode get(Fixture fixture, long incomeId) throws Exception {
        return getJson(URL + "/" + incomeId, fixture.token());
    }

    /** 저장된 행을 DB 에서 직접 본다. 응답만 보면 스냅샷이 어떻게 들어갔는지 모른다. */
    protected Map<String, Object> row(long incomeId) {
        return jdbc.queryForMap("""
                select payment_method_idx, payment_method_name, amount, payment_date, content
                  from moneylog.tbl_income where idx = ?
                """, incomeId);
    }

    /** 그 회원의 소득 건수. 물리 삭제를 확인할 때 쓴다. */
    protected int countIncomes(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_income i
                  join moneylog.tbl_user u on u.id_key = i.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }
}

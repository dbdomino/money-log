package com.dbdomino.moneylog.backend.expense;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * 일시불 지출(3.1~3.4) 통합 테스트의 공통 바탕.
 *
 * <p>지출 하나를 만들려면 <b>사용 중인 수단과 지출유형</b>이 먼저 있어야 한다(FR-325).
 * 가입이 만들어 준 기본 유형과 003 의 2.1 로 만든 수단을 함께 묶어 두는 것이 이 클래스다 —
 * 네 시험 클래스가 매번 같은 준비를 반복하지 않게 한다.
 */
abstract class AbstractExpenseIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/expenses";

    /** 지출을 만들 준비가 끝난 회원 — 토큰·수단·지출유형을 함께 들고 다닌다. */
    protected record Fixture(Member member, long paymentMethodId, long expendGroupId) {

        String token() {
            return member.token();
        }
    }

    /**
     * 가입하고 지출용 수단 1건을 만든다. 지출유형은 <b>가입이 만들어 준 기본 10종</b> 중
     * "식비"를 쓴다 — 유형을 새로 만들려면 2.7 이 multipart 라 준비가 길어진다.
     */
    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        long methodId = createExpensePaymentMethod(member.token(), "국민카드");
        return new Fixture(member, methodId, defaultGroupId(member, "식비"));
    }

    /** 3.1 등록. 성공을 전제하지 않는다 — 실패 코드를 보는 시험도 이걸 쓴다. */
    protected JsonNode create(Fixture fixture, long paymentMethodId, long expendGroupId,
                              long amount, String paymentDate, String place, String content)
            throws Exception {
        return postJson(URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":%d,
                 "paymentDate":"%s","place":"%s","content":"%s"}
                """.formatted(paymentMethodId, expendGroupId, amount, paymentDate, place, content));
    }

    /** 3.1 등록 — 기본값으로 1건. 준비가 목적일 때 쓴다. */
    protected JsonNode create(Fixture fixture) throws Exception {
        return create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-03-15", "편의점", "점심");
    }

    /** 등록에 성공했다고 보고 그 PK 를 꺼낸다. */
    protected long idOf(JsonNode response) {
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("expenseId").asLong();
    }

    /** 준비용 지출 1건을 만들고 PK 를 돌려준다. */
    protected long createExpense(Fixture fixture) throws Exception {
        return idOf(create(fixture));
    }

    /** 3.2 상세 조회의 {@code data}. */
    protected JsonNode get(Fixture fixture, long expenseId) throws Exception {
        return getJson(URL + "/" + expenseId, fixture.token());
    }

    /** 저장된 행을 DB 에서 직접 본다. 응답만 보면 스냅샷이 실제로 어떻게 들어갔는지 모른다. */
    protected Map<String, Object> row(long expenseId) {
        return jdbc.queryForMap("""
                select payment_method_idx, payment_method_name,
                       expend_group_idx, expend_group_name, amount, place, content,
                       installment_group_id, installment_index, installment_total
                  from moneylog.tbl_expense where idx = ?
                """, expenseId);
    }

    /** 그 회원의 지출 건수. 물리 삭제와 롤백을 확인할 때 쓴다. */
    protected int countExpenses(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_expense e
                  join moneylog.tbl_user u on u.id_key = e.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }
}

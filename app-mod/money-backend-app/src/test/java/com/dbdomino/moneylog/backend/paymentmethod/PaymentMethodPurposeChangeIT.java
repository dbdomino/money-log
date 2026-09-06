package com.dbdomino.moneylog.backend.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

/**
 * 2.4 수단 {@code purpose} 변경의 참조 검사 — quickstart #39·#40·#41 (SC-206).
 *
 * <h2>4개 테이블을 각각 참조원으로 삼는다</h2>
 *
 * <p>{@code tbl_expense}·{@code tbl_income}·{@code tbl_fixed_expense}·
 * {@code tbl_fixed_expense_monthly} 넷 중 <b>하나라도 검사에서 빠지면 그 케이스만
 * 통과한다.</b> 한 테이블로만 시험하면 나머지 셋을 빠뜨린 구현이 그대로 통과하고, 그 결과
 * "소득 수단으로 낸 지출"이 만들어져 월별 집계와 통계의 수단별 요약이 어긋난다(FR-205).
 *
 * <h2>참조 행을 JDBC 로 만든다</h2>
 *
 * <p>지출·소득·고정지출(004)의 등록 API 가 아직 없다. 검증 대상은 <b>003 의 판정</b>이지
 * 004 의 등록 흐름이 아니므로 행을 직접 넣는다. 감사 컬럼이 NOT NULL 이라 값을 명시해야
 * 하고, 갱신은 트랜잭션 안에서 해야 커밋된다.
 */
class PaymentMethodPurposeChangeIT extends AbstractApiIT {

    private static final String URL = "/api/v1/payment-methods";

    /** 수단 1건을 등록하고 PK 를 돌려준다. 용도는 지출용으로 시작한다. */
    private long createMethod(String token) throws Exception {
        JsonNode response = postJson(URL, token, """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("paymentMethodId").asLong();
    }

    /** 지출유형 1건. 지출·고정지출 행이 FK 로 요구한다. */
    private long createExpendGroup(User user) {
        Long idKey = user.getIdKey();
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_user_expend_group
                       (name, in_use, default_group, deleted,
                        id_key, created_at, created_by, updated_at, updated_by)
                values ('시험유형', true, false, false, ?, now(), ?, now(), ?)
                """, idKey, idKey, idKey));
        return jdbc.queryForObject("""
                select idx from moneylog.tbl_user_expend_group
                 where id_key = ? and name = '시험유형'
                """, Long.class, idKey);
    }

    /**
     * 그 수단을 참조하는 행을 {@code table} 에 1건 만든다.
     *
     * <p>테이블마다 필수 컬럼이 다르므로 INSERT 문을 따로 적는다 — 공통으로 묶으면
     * NOT NULL 을 채우느라 시험이 읽기 어려워진다.
     */
    private void insertReference(String table, User user, long methodId, long groupId) {
        Long idKey = user.getIdKey();
        tx.executeWithoutResult(status -> {
            switch (table) {
                case "tbl_expense" -> jdbc.update("""
                        insert into moneylog.tbl_expense
                               (payment_date, amount, expend_group_idx, id_key, payment_method_idx,
                                expend_group_name, payment_method_name, place, content,
                                created_at, created_by, updated_at, updated_by)
                        values (current_date, 10000, ?, ?, ?, '시험유형', '국민카드', '편의점', '점심',
                                now(), ?, now(), ?)
                        """, groupId, idKey, methodId, idKey, idKey);
                case "tbl_income" -> jdbc.update("""
                        insert into moneylog.tbl_income
                               (payment_date, amount, id_key, payment_method_idx,
                                payment_method_name, content,
                                created_at, created_by, updated_at, updated_by)
                        values (current_date, 3000000, ?, ?, '국민카드', '급여',
                                now(), ?, now(), ?)
                        """, idKey, methodId, idKey, idKey);
                case "tbl_fixed_expense" -> jdbc.update("""
                        insert into moneylog.tbl_fixed_expense
                               (start_year, start_month, end_year, end_month,
                                payment_day_of_month, amount, expend_group_idx, id_key,
                                payment_method_idx, name, content,
                                created_at, created_by, updated_at, updated_by)
                        values (2026, 1, 2026, 12, 25, 50000, ?, ?, ?, '통신비', '휴대폰',
                                now(), ?, now(), ?)
                        """, groupId, idKey, methodId, idKey, idKey);
                case "tbl_fixed_expense_monthly" -> jdbc.update("""
                        insert into moneylog.tbl_fixed_expense_monthly
                               (year, month, payment_date, modified, amount, expend_group_idx,
                                fixed_expense_idx, id_key, payment_method_idx, content,
                                created_at, created_by, updated_at, updated_by)
                        values (2026, 3, current_date, false, 50000, ?, ?, ?, ?, '휴대폰',
                                now(), ?, now(), ?)
                        """, groupId, parentFixedExpense(idKey, groupId), idKey, methodId,
                        idKey, idKey);
                default -> throw new IllegalArgumentException("모르는 테이블: " + table);
            }
        });
    }

    /**
     * 월별 고정지출이 FK 로 요구하는 관리 행({@code tbl_fixed_expense}).
     *
     * <p><b>이 부모 행은 다른 수단을 가리킨다.</b> 시험 대상 수단을 쓰면 참조가 둘이 되어,
     * {@code tbl_fixed_expense_monthly} 를 검사에서 빠뜨린 구현도 부모 행 때문에
     * {@code 3005} 를 내며 통과한다 — 이 케이스가 잡으려던 실패 모드가 그대로 빠져나간다.
     */
    private Long parentFixedExpense(Long idKey, long groupId) {
        jdbc.update("""
                insert into moneylog.tbl_user_payment_method
                       (name, type, purpose, in_use, deleted,
                        id_key, created_at, created_by, updated_at, updated_by)
                values ('다른수단', 'CARD', 'EXPENSE', true, false, ?, now(), ?, now(), ?)
                """, idKey, idKey, idKey);
        Long otherMethodId = jdbc.queryForObject("""
                select idx from moneylog.tbl_user_payment_method
                 where id_key = ? and name = '다른수단'
                """, Long.class, idKey);

        jdbc.update("""
                insert into moneylog.tbl_fixed_expense
                       (start_year, start_month, end_year, end_month,
                        payment_day_of_month, amount, expend_group_idx, id_key,
                        payment_method_idx, name, content,
                        created_at, created_by, updated_at, updated_by)
                values (2026, 1, 2026, 12, 25, 50000, ?, ?, ?, '통신비-월별부모', '휴대폰',
                        now(), ?, now(), ?)
                """, groupId, idKey, otherMethodId, idKey, idKey);
        return jdbc.queryForObject("""
                select idx from moneylog.tbl_fixed_expense
                 where id_key = ? and name = '통신비-월별부모'
                """, Long.class, idKey);
    }

    @ParameterizedTest(name = "#39 {0} 에 참조가 있으면 purpose 변경은 3005 다")
    @ValueSource(strings = {
            "tbl_expense", "tbl_income", "tbl_fixed_expense", "tbl_fixed_expense_monthly"})
    void purposeChangeIsBlockedByEveryReferencingTable(String table) throws Exception {
        User user = createMember();
        String token = login(user).accessToken();
        long methodId = createMethod(token);
        long groupId = createExpendGroup(user);
        insertReference(table, user, methodId, groupId);

        JsonNode response = patchJson(URL + "/" + methodId, token, """
                {"purpose":"INCOME"}
                """);

        assertThat(resCode(response)).as(table).isEqualTo(3005);

        String purpose = jdbc.queryForObject(
                "select purpose from moneylog.tbl_user_payment_method where idx = ?",
                String.class, methodId);
        assertThat(purpose).as("거절했으면 값도 그대로여야 한다").isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("#40 purpose 를 omit 하면 참조가 있어도 다른 필드는 수정된다")
    void omittingPurposeSkipsTheReferenceCheck() throws Exception {
        User user = createMember();
        String token = login(user).accessToken();
        long methodId = createMethod(token);
        long groupId = createExpendGroup(user);
        insertReference("tbl_expense", user, methodId, groupId);

        JsonNode response = patchJson(URL + "/" + methodId, token, """
                {"name":"국민카드(메인)","inUse":false}
                """);

        // 참조 검사를 했다면 3005 가 나온다. omit 은 "건드리지 않는다"이므로 검사할 것도 없다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("name").asString()).isEqualTo("국민카드(메인)");
        assertThat(response.get("data").get("inUse").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("#40 purpose 를 같은 값으로 보내면 참조가 있어도 성공한다 — 값이 바뀌지 않는다")
    void sendingTheSamePurposeSkipsTheReferenceCheck() throws Exception {
        User user = createMember();
        String token = login(user).accessToken();
        long methodId = createMethod(token);
        long groupId = createExpendGroup(user);
        insertReference("tbl_expense", user, methodId, groupId);

        // "보냈는가"만 보고 검사하면 여기서 3005 가 나온다. 실제로 바뀌는지까지 봐야 한다.
        assertThat(resCode(patchJson(URL + "/" + methodId, token, """
                {"purpose":"EXPENSE"}
                """))).isEqualTo(200);
    }

    @Test
    @DisplayName("#41 참조가 0건이면 purpose 변경이 성공한다")
    void purposeChangeSucceedsWithoutReferences() throws Exception {
        String token = login(createMember()).accessToken();
        long methodId = createMethod(token);

        JsonNode response = patchJson(URL + "/" + methodId, token, """
                {"purpose":"INCOME"}
                """);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("purpose").asString()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("남의 수단이면 참조 검사보다 3003 이 먼저다 — 사용 내역의 존재가 새어 나가지 않는다")
    void ownershipIsCheckedBeforeReferences() throws Exception {
        User owner = createMember();
        String ownerToken = login(owner).accessToken();
        long methodId = createMethod(ownerToken);
        long groupId = createExpendGroup(owner);
        insertReference("tbl_expense", owner, methodId, groupId);

        String intruder = login(createMember()).accessToken();

        assertThat(resCode(patchJson(URL + "/" + methodId, intruder, """
                {"purpose":"INCOME"}
                """))).isEqualTo(3003);
    }
}

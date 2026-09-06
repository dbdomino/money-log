package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 이름은 참조가 아니라 <b>그 시점의 값</b>이다 — quickstart #38 (SC-205·FR-208).
 *
 * <p>과거 지출·소득은 수단 이름과 유형 이름을 <b>복사해서</b> 들고 있다
 * ({@code expend_group_name}·{@code payment_method_name}). 수단이나 유형의 이름을 바꿔도
 * 그 값은 그대로여야 한다 — 3월에 "국민카드"로 적힌 지출은 카드 이름을 바꿔도 3월에는
 * "국민카드"였던 것이 사실이기 때문이다.
 *
 * <p>이 규칙이 깨지면 과거 명세서를 다시 뽑을 때마다 내용이 달라진다. 삭제 표시(FR-206)가
 * 행을 남기는 것과 짝이 되는 결정이다.
 *
 * <p>지출 행은 004 의 API 가 없어 JDBC 로 직접 만든다 — 검증 대상은 <b>003 의 수정이
 * 스냅샷을 건드리지 않는가</b>이지 004 의 등록 흐름이 아니다.
 */
class NameSnapshotIT extends AbstractExpendGroupIT {

    private Long idKeyOf(Member member) {
        return jdbc.queryForObject(
                "select id_key from moneylog.tbl_user where user_id = ?",
                Long.class, member.memberId());
    }

    /** 수단 1건을 2.1 로 만든다. */
    private long createPaymentMethod(String token, String name) throws Exception {
        JsonNode response = postJson("/api/v1/payment-methods", token, """
                {"name":"%s","type":"CARD","purpose":"EXPENSE","inUse":true}
                """.formatted(name));
        assertThat(resCode(response)).isEqualTo(200);
        return response.get("data").get("paymentMethodId").asLong();
    }

    /** 두 이름을 복사해 담은 지출 1건. */
    private void insertExpense(Member member, long groupId, String groupName,
                               long methodId, String methodName) {
        Long idKey = idKeyOf(member);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_expense
                       (payment_date, amount, expend_group_idx, id_key, payment_method_idx,
                        expend_group_name, payment_method_name, place, content,
                        created_at, created_by, updated_at, updated_by)
                values (current_date, 12000, ?, ?, ?, ?, ?, '편의점', '점심',
                        now(), ?, now(), ?)
                """, groupId, idKey, methodId, groupName, methodName, idKey, idKey));
    }

    private Map<String, Object> expenseRow(Member member) {
        return jdbc.queryForMap("""
                select e.expend_group_name, e.payment_method_name
                  from moneylog.tbl_expense e
                  join moneylog.tbl_user u on u.id_key = e.id_key
                 where u.user_id = ?
                """, member.memberId());
    }

    @Test
    @DisplayName("#38 수단·유형 이름을 바꿔도 과거 지출의 스냅샷 이름은 그대로다")
    void renamingDoesNotTouchPastSnapshots() throws Exception {
        Member member = signupAndLogin();
        long groupId = idOf(createGroup(member.token(), "취미", true));
        long methodId = createPaymentMethod(member.token(), "국민카드");
        insertExpense(member, groupId, "취미", methodId, "국민카드");

        assertThat(resCode(updateGroup(member.token(), groupId, "여가", null))).isEqualTo(200);
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + methodId, member.token(), """
                {"name":"국민카드(메인)"}
                """))).isEqualTo(200);

        Map<String, Object> row = expenseRow(member);
        assertThat(row.get("expend_group_name"))
                .as("유형 이름을 바꿔도 그때 적힌 이름은 사실이다")
                .isEqualTo("취미");
        assertThat(row.get("payment_method_name"))
                .as("수단 이름을 바꿔도 그때 적힌 이름은 사실이다")
                .isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#38 삭제 표시해도 과거 지출의 스냅샷 이름은 그대로다")
    void softDeleteDoesNotTouchPastSnapshots() throws Exception {
        Member member = signupAndLogin();
        long groupId = idOf(createGroup(member.token(), "취미", true));
        long methodId = createPaymentMethod(member.token(), "국민카드");
        // 지출이 참조하는 유형은 3106 으로 삭제가 막히므로, 삭제 대상은 수단으로 확인한다.
        insertExpense(member, groupId, "취미", methodId, "국민카드");

        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + methodId, member.token())))
                .isEqualTo(200);

        assertThat(expenseRow(member).get("payment_method_name")).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("이름을 바꾼 뒤에도 지출은 같은 행을 계속 가리킨다 — 스냅샷과 참조는 별개다")
    void snapshotAndReferenceAreIndependent() throws Exception {
        Member member = signupAndLogin();
        long groupId = idOf(createGroup(member.token(), "취미", true));
        long methodId = createPaymentMethod(member.token(), "국민카드");
        insertExpense(member, groupId, "취미", methodId, "국민카드");

        assertThat(resCode(updateGroup(member.token(), groupId, "여가", null))).isEqualTo(200);

        Long referencedGroup = jdbc.queryForObject("""
                select e.expend_group_idx from moneylog.tbl_expense e
                  join moneylog.tbl_user u on u.id_key = e.id_key
                 where u.user_id = ?
                """, Long.class, member.memberId());
        // FK 는 그대로, 스냅샷만 옛 이름이다. 화면은 둘을 다른 목적으로 쓴다.
        assertThat(referencedGroup).isEqualTo(groupId);
        assertThat(getJson(URL + "/" + groupId, member.token())
                .get("data").get("name").asString()).isEqualTo("여가");
    }
}

package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.12 지출유형 삭제 — quickstart #18·#19·#20·#23.
 *
 * <h2>판정 순서가 곧 응답 코드다</h2>
 *
 * <pre>{@code
 * 1. 대상 조회(본인 소유?)   없음·타인 → 3103
 * 2. 이미 deleted=true       → 3108
 * 3. default_group=true      → 3107
 * 4. 그 유형을 쓴 지출 있음   → 3106
 * 5. deleted=true 로 UPDATE
 * }</pre>
 *
 * <p>#20 이 순서 검증이다 — <b>이미 삭제된 기본 유형</b>을 다시 지우면 {@code 3108} 이지
 * {@code 3107} 이 아니다. 3·2 를 뒤집은 구현은 이 시험에서만 걸린다.
 *
 * <h2>참조 행을 JDBC 로 만든다</h2>
 *
 * <p>지출(004)·목표금액(005)의 등록 API 가 아직 없다. 여기서 검증할 것은 <b>003 의 판정</b>
 * 이지 004·005 의 등록 흐름이 아니므로 행을 직접 넣는다. 감사 컬럼이 NOT NULL 이라 값을
 * 명시해야 하고, 갱신은 트랜잭션 안에서 해야 커밋된다.
 */
class ExpendGroupDeleteIT extends AbstractExpendGroupIT {

    /** 그 유형을 쓴 지출 1건을 만든다. 수단은 2.1 로 만들어 FK 를 채운다. */
    private void insertExpense(Member member, long expendGroupId, String groupName)
            throws Exception {
        JsonNode method = postJson("/api/v1/payment-methods", member.token(), """
                {"name":"국민카드","type":"CARD","purpose":"EXPENSE","inUse":true}
                """);
        assertThat(resCode(method)).isEqualTo(200);
        long methodId = method.get("data").get("paymentMethodId").asLong();
        Long idKey = idKeyOf(member);

        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_expense
                       (payment_date, amount, expend_group_idx, id_key, payment_method_idx,
                        expend_group_name, payment_method_name, place, content,
                        created_at, created_by, updated_at, updated_by)
                values (current_date, 10000, ?, ?, ?, ?, '국민카드', '편의점', '점심',
                        now(), ?, now(), ?)
                """, expendGroupId, idKey, methodId, groupName, idKey, idKey));
    }

    /** 그 유형에 걸린 기본 목표금액 1건을 만든다. */
    private void insertDefaultTarget(Member member, long expendGroupId) {
        Long idKey = idKeyOf(member);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into moneylog.tbl_expend_target_default
                       (expend_group_idx, id_key, target_amount,
                        created_at, created_by, updated_at, updated_by)
                values (?, ?, 300000, now(), ?, now(), ?)
                """, expendGroupId, idKey, idKey, idKey));
    }

    private int countTargets(long expendGroupId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from moneylog.tbl_expend_target_default where expend_group_idx = ?",
                Integer.class, expendGroupId);
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("#18 그 유형을 쓴 지출이 있으면 3106 이다")
    void groupUsedByExpenseCannotBeDeleted() throws Exception {
        Member member = signupAndLogin();
        long id = idOf(createGroup(member.token(), "취미", true));
        insertExpense(member, id, "취미");

        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(3106);

        Boolean deleted = jdbc.queryForObject(
                "select deleted from moneylog.tbl_user_expend_group where idx = ?",
                Boolean.class, id);
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("#19 기본 유형은 삭제할 수 없다 — 3107")
    void defaultGroupCannotBeDeleted() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");

        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(3107);
    }

    @Test
    @DisplayName("#20 이미 삭제된 유형을 다시 삭제하면 3108 이다")
    void deletingTwiceIs3108() throws Exception {
        Member member = signupAndLogin();
        long id = idOf(createGroup(member.token(), "취미", true));

        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(200);
        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(3108);
    }

    @Test
    @DisplayName("#20 이미 삭제된 기본 유형은 3108 이지 3107 이 아니다 — 판정 순서")
    void alreadyDeletedDefaultGroupIs3108NotThe3107() throws Exception {
        Member member = signupAndLogin();
        long id = defaultGroupId(member, "식비");
        // 기본 유형은 API 로 지울 수 없으므로(3107) 삭제된 상태를 직접 만든다.
        tx.executeWithoutResult(status -> jdbc.update(
                "update moneylog.tbl_user_expend_group set deleted = true where idx = ?", id));

        // 3107 이 나오면 2번과 3번의 순서가 뒤집힌 것이다.
        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(3108);
    }

    @Test
    @DisplayName("#23 삭제 표시해도 목표금액의 참조는 유지된다 — FR-211")
    void targetReferencesSurviveSoftDelete() throws Exception {
        Member member = signupAndLogin();
        long id = idOf(createGroup(member.token(), "취미", true));
        insertDefaultTarget(member, id);

        // 목표금액이 걸려 있어도 삭제 표시는 된다 — 막는 것은 tbl_expense 하나뿐이다.
        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(200);

        assertThat(countTargets(id))
                .as("삭제 표시가 목표금액 행을 지우면 과거 집계가 무너진다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("삭제 응답은 expendGroupId 와 message 두 칸이다")
    void deleteResponseShape() throws Exception {
        Member member = signupAndLogin();
        long id = idOf(createGroup(member.token(), "취미", true));

        JsonNode data = deleteJson(URL + "/" + id, member.token()).get("data");

        assertThat(data.get("expendGroupId").asLong()).isEqualTo(id);
        assertThat(data.get("message").asString()).isNotBlank();
    }

    @Test
    @DisplayName("삭제해도 관리 목록(2.8)에는 deleted=true 로 남는다")
    void deletedStaysInManagementList() throws Exception {
        Member member = signupAndLogin();
        long id = idOf(createGroup(member.token(), "취미", true));
        assertThat(resCode(deleteJson(URL + "/" + id, member.token()))).isEqualTo(200);

        boolean found = false;
        for (JsonNode node : getJson(URL, member.token()).get("data").get("list")) {
            if (node.get("expendGroupId").asLong() == id) {
                found = true;
                assertThat(node.get("deleted").asBoolean()).isTrue();
            }
        }
        assertThat(found).isTrue();
    }
}

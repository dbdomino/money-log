package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 참조가 뒤늦게 죽어도 생성이 막히지 않는다 — quickstart #22-2 (FR-426).
 *
 * <h2>실제로 일어나는 경로다</h2>
 *
 * <pre>{@code
 * 1. "동호회비" 고정지출을 만든다 (지출유형: 취미)
 * 2. "취미" 유형으로 일반 지출은 한 번도 적지 않는다
 * 3. "취미" 유형을 삭제한다
 *    → 003 의 ExpendGroupService.delete 는 tbl_expense 참조만 본다.
 *      고정지출 참조는 보지 않으므로 막히지 않고 삭제 표시된다.
 * 4. 다음 달을 연다
 *    → 생성 대상에 "동호회비"가 들어오는데 유형이 사용 불가다
 * }</pre>
 *
 * <p>여기서 {@code ReferenceResolver.requireUsableExpendGroup} 을 재사용하면
 * <b>평범한 달 조회가 {@code 3103} 으로 죽고 사용자는 그 달을 영영 열지 못한다.</b>
 * 사용자는 아무것도 잘못하지 않았다.
 *
 * <h2>방향이 반대인 규칙이 짝으로 있다</h2>
 *
 * <table border="1">
 *   <caption>참조가 죽었을 때</caption>
 *   <tr><th>경로</th><th>사용 가능 여부를 묻는가</th><th>왜</th></tr>
 *   <tr><td>자동 생성·재작성(4.5·4.8·4.9)</td><td><b>묻지 않는다</b></td>
 *       <td>사용자가 이미 정해 둔 것을 그대로 펼칠 뿐이다</td></tr>
 *   <tr><td>사용자가 직접 고름(4.1·4.4·4.6)</td><td><b>묻는다</b></td>
 *       <td>죽은 참조로 갈아타는 것을 막아야 한다</td></tr>
 * </table>
 *
 * <p>한쪽 규칙을 양쪽에 쓰면 반드시 한쪽이 틀린다 — 자동에 검증을 걸면 조회가 죽고,
 * 사용자 선택에서 빼면 삭제한 수단이 되살아난다.
 */
class MonthlyDeadReferenceIT extends AbstractMonthlyIT {

    /** 고정지출만 쓰는 새 지출유형으로 설정 1건을 만든다. 일반 지출은 적지 않는다. */
    private Fixture prepareWithOwnGroup() throws Exception {
        Member member = signupAndLogin();
        long methodId = createExpensePaymentMethod(member.token(), "국민카드");
        long groupId = createExpendGroup(member.token(), "취미");
        long fixedExpenseId = createFixedExpense(member.token(), "동호회비", methodId,
                groupId, 30000L, 10, START, END);
        return new Fixture(member, methodId, groupId, fixedExpenseId);
    }

    @Test
    @DisplayName("#22-2 고정지출만 쓰던 지출유형을 삭제해도 003 이 막지 않는다")
    void deletingAGroupUsedOnlyByFixedExpenseSucceeds() throws Exception {
        Fixture fixture = prepareWithOwnGroup();

        // 이 전제가 깨지면(3106 이 나면) 아래 시나리오 자체가 성립하지 않는다.
        // 그때는 이 시험이 아니라 FR-426 의 근거를 다시 봐야 한다.
        assertThat(resCode(deleteJson("/api/v1/expend-groups/" + fixture.expendGroupId(),
                fixture.token()))).isEqualTo(200);
    }

    @Test
    @DisplayName("#22-2 삭제 표시된 유형을 쓰는 달을 처음 열어도 내역이 만들어진다")
    void deletedGroupDoesNotBlockCreation() throws Exception {
        Fixture fixture = prepareWithOwnGroup();
        assertThat(resCode(deleteJson("/api/v1/expend-groups/" + fixture.expendGroupId(),
                fixture.token()))).isEqualTo(200);

        JsonNode response = listMonthly(fixture, 2026, 11);

        // 3103 이 나오면 생성 경로가 참조를 다시 검증한 것이다.
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(response.get("data").get("list")).hasSize(1);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#22-2 사용 안 함으로 돌린 수단도 생성을 막지 않는다")
    void disabledPaymentMethodDoesNotBlockCreation() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                        {"inUse":false}
                        """))).isEqualTo(200);

        JsonNode response = listMonthly(fixture, 2026, 11);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#22-2 삭제 표시된 수단도 생성을 막지 않는다")
    void deletedPaymentMethodDoesNotBlockCreation() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        JsonNode response = listMonthly(fixture, 2026, 11);

        assertThat(resCode(response)).isEqualTo(200);
        assertThat(countMonthly(fixture.member(), 2026, 11)).isEqualTo(1);
    }

    @Test
    @DisplayName("#22-2 죽은 참조의 이름도 그대로 읽힌다 — 행이 보존되기 때문이다")
    void deadReferenceStillResolvesItsName() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        JsonNode item = listMonthly(fixture, 2026, 11).get("data").get("list").get(0);

        // 수단·지출유형은 물리 삭제가 아니라 삭제 표시라 행이 남는다.
        // 그래서 이름이 null 이 되거나 조회가 깨지지 않는다.
        assertThat(item.get("paymentMethodName").asString()).isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#22-2 자동 생성과 달리 사용자가 직접 고르는 4.1 은 여전히 거절한다")
    void userChosenReferenceIsStillValidated() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        String withDeadMethod = """
                {"name":"새 고정지출","paymentMethodId":%d,"expendGroupId":%d,"amount":10000,
                 "paymentDayOfMonth":10,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId());

        // 방향이 반대다. 자동 생성은 통과시키지만 사용자 선택은 막는다.
        assertThat(resCode(postJson("/api/v1/fixed-expenses", fixture.token(), withDeadMethod)))
                .isEqualTo(3003);
    }
}

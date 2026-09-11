package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.5 의 이름 규칙 — quickstart #22-1 (SC-409 의 <b>4.5 몫</b>).
 *
 * <p>SC-409 는 <b>세 다리</b>를 요구한다 — 4.3(설정)·4.5(월별 내역)·4.8(가계부).
 * 4.3 은 {@code fixedexpense/FixedExpenseCurrentNameIT}, 4.8 은 {@code ledger/} 가 맡고
 * 여기가 남은 하나다.
 *
 * <p><b>월별 내역에도 이름 컬럼이 없다.</b> {@code tbl_fixed_expense_monthly} 는
 * {@code payment_method_idx}·{@code expend_group_idx} 참조만 갖고, 이름 셋은 전부 조회
 * 시점에 원본에서 읽는다. 이 시험이 깨지면 누군가 스키마에 이름 컬럼을 추가했다는 뜻이다.
 *
 * <p><b>이미 만들어진 행에도 적용된다</b>는 것이 핵심이다. 004 라면 등록 시점에 이름이
 * 박혀 바뀌지 않는데, 여기서는 행을 만든 뒤에 이름을 바꿔도 재조회에서 새 이름이 나온다.
 */
class MonthlyCurrentNameIT extends AbstractMonthlyIT {

    @Test
    @DisplayName("#22-1 수단 이름을 바꾸고 같은 달을 재조회하면 새 이름이 나온다")
    void renamedPaymentMethodShowsTheNewName() throws Exception {
        Fixture fixture = prepare();
        // 먼저 행을 만들어 둔다. 그 뒤의 이름 변경이 따라오는지가 요점이다.
        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list").get(0)
                .get("paymentMethodName").asString()).isEqualTo("국민카드");

        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                        {"name":"국민체크"}
                        """))).isEqualTo(200);

        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list").get(0)
                .get("paymentMethodName").asString()).isEqualTo("국민체크");
    }

    @Test
    @DisplayName("#22-1 지출유형 이름을 바꿔도 같은 규칙이다")
    void renamedExpendGroupShowsTheNewName() throws Exception {
        Member member = signupAndLogin();
        long methodId = createExpensePaymentMethod(member.token(), "국민카드");
        // 기본 유형은 이름을 바꿀 수 없으므로(3105) 새 유형을 만든다.
        long groupId = createExpendGroup(member.token(), "취미");
        long fixedExpenseId = createFixedExpense(member.token(), "동호회비", methodId,
                groupId, 30000L, 10, START, END);
        Fixture fixture = new Fixture(member, methodId, groupId, fixedExpenseId);
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(renameExpendGroup(member.token(), groupId, "여가"))).isEqualTo(200);

        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list").get(0)
                .get("expendGroupName").asString()).isEqualTo("여가");
    }

    @Test
    @DisplayName("#22-1 고정지출 이름을 바꾸면 월별 내역의 이름도 따라간다")
    void renamedSettingShowsTheNewName() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(patchJson("/api/v1/fixed-expenses/" + fixture.fixedExpenseId(),
                fixture.token(), """
                        {"name":"전세 이자"}
                        """))).isEqualTo(200);

        // fixedExpenseName 은 "관리 테이블의 현재 값"이다(4.5 명세).
        assertThat(listMonthly(fixture, 2026, 11).get("data").get("list").get(0)
                .get("fixedExpenseName").asString()).isEqualTo("전세 이자");
    }

    @Test
    @DisplayName("#22-1 이름이 바뀌어도 그 달 금액·결제일은 그대로다")
    void renameDoesNotTouchTheStoredValues() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                        {"name":"국민체크"}
                        """))).isEqualTo(200);

        JsonNode item = listMonthly(fixture, 2026, 11).get("data").get("list").get(0);
        assertThat(item.get("amount").asLong()).isEqualTo(500000L);
        assertThat(item.get("paymentDate").asString()).isEqualTo("2026-11-25");
    }
}

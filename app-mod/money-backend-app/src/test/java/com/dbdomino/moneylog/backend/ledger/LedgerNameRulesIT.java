package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 한 응답에 이름 규칙이 둘 섞인다 — quickstart #42·#43·#44 (FR-419·FR-425, SC-409 의 4.8 몫).
 *
 * <p><b>#44 가 이 목록에서 가장 헷갈리는 지점이다.</b> 같은 필드({@code paymentMethodName})가
 * 행 종류에 따라 다른 곳에서 온다.
 *
 * <table border="1">
 *   <caption>수단 이름을 바꾼 뒤 같은 달을 재조회하면</caption>
 *   <tr><th>행 종류</th><th>결과</th><th>왜</th></tr>
 *   <tr><td>{@code FIXED}</td><td><b>새 이름</b></td><td>지금 유효한 설정이다</td></tr>
 *   <tr><td>{@code EXPENSE}·{@code INSTALLMENT}·{@code INCOME}</td><td><b>옛 이름</b></td>
 *       <td>이미 일어난 과거 기록이다</td></tr>
 * </table>
 *
 * <p>이 시험이 없으면 누군가 "일관성"을 이유로 한쪽에 맞춘다. 어느 쪽으로 맞춰도
 * 요구사항이 깨진다 — 스냅샷으로 통일하면 스키마에 이름 컬럼이 필요해지고(FR-405),
 * 현재 이름으로 통일하면 003 의 SC-205("과거 기록 보존")가 무너진다.
 */
class LedgerNameRulesIT extends AbstractLedgerIT {

    /** 그 회원의 수단 이름을 바꾼다. */
    private void renamePaymentMethod(Fixture fixture, long paymentMethodId, String name)
            throws Exception {
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + paymentMethodId,
                fixture.token(), """
                        {"name":"%s"}
                        """.formatted(name)))).isEqualTo(200);
    }

    @Test
    @DisplayName("#42 고정지출 행의 이름은 현재 이름이다")
    void fixedRowCarriesCurrentNames() throws Exception {
        Fixture fixture = prepare();

        JsonNode fixed = firstOfType(ledger(fixture), "FIXED");

        assertThat(fixed.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(fixed.get("expendGroupName").asString()).isEqualTo("주거");
        assertThat(fixed.get("fixedExpenseName").asString()).isEqualTo("월세");
    }

    @Test
    @DisplayName("#43 일반 지출 행의 이름은 등록 당시 스냅샷이다")
    void expenseRowCarriesSnapshots() throws Exception {
        Fixture fixture = prepare();

        JsonNode expense = firstOfType(ledger(fixture), "EXPENSE");

        assertThat(expense.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(expense.get("expendGroupName").asString()).isEqualTo("식비");
    }

    @Test
    @DisplayName("#44 수단 이름을 바꾸면 FIXED 행만 새 이름이 된다")
    void onlyFixedRowFollowsTheRename() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);   // 월별 내역을 먼저 만들어 둔다

        renamePaymentMethod(fixture, fixture.expenseMethodId(), "국민체크");

        JsonNode response = ledger(fixture);
        assertThat(firstOfType(response, "FIXED").get("paymentMethodName").asString())
                .as("고정지출은 지금 유효한 설정이라 새 이름이다")
                .isEqualTo("국민체크");
        assertThat(firstOfType(response, "EXPENSE").get("paymentMethodName").asString())
                .as("지출은 과거 기록이라 옛 이름이다")
                .isEqualTo("국민카드");
        assertThat(firstOfType(response, "INSTALLMENT").get("paymentMethodName").asString())
                .isEqualTo("국민카드");
    }

    @Test
    @DisplayName("#44 소득 수단 이름을 바꿔도 INCOME 행은 옛 이름이다")
    void incomeRowKeepsItsSnapshot() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);

        renamePaymentMethod(fixture, fixture.incomeMethodId(), "주거래통장");

        assertThat(firstOfType(ledger(fixture), "INCOME").get("paymentMethodName").asString())
                .isEqualTo("월급통장");
    }

    @Test
    @DisplayName("#44 지출유형 이름을 바꿔도 같은 규칙이다 — FIXED 만 따라간다")
    void expendGroupRenameFollowsTheSameSplit() throws Exception {
        Fixture fixture = prepare();
        // 기본 유형은 이름을 바꿀 수 없으므로(3105) 새 유형으로 고정지출을 하나 더 만든다.
        long hobbyGroup = createExpendGroup(fixture.token(), "취미");
        createFixedExpense(fixture.token(), "동호회비", fixture.expenseMethodId(),
                hobbyGroup, 30000L, 10, "2026-07", "2026-12");
        ledger(fixture);

        assertThat(resCode(renameExpendGroup(fixture.token(), hobbyGroup, "여가"))).isEqualTo(200);

        JsonNode response = ledger(fixture);
        boolean sawRenamed = false;
        for (JsonNode item : response.get("data").get("list")) {
            if ("FIXED".equals(item.get("type").asString())
                    && "동호회비".equals(item.get("fixedExpenseName").asString())) {
                assertThat(item.get("expendGroupName").asString()).isEqualTo("여가");
                sawRenamed = true;
            }
        }
        assertThat(sawRenamed).as("새로 만든 고정지출 행이 목록에 있어야 한다").isTrue();
    }

    @Test
    @DisplayName("#42 소득 행은 지출유형·장소가 null 이다 — 컬럼이 아예 없다")
    void incomeRowHasNoGroupOrPlace() throws Exception {
        Fixture fixture = prepare();

        JsonNode income = firstOfType(ledger(fixture), "INCOME");

        assertThat(income.get("expendGroupId").isNull()).isTrue();
        assertThat(income.get("expendGroupName").isNull()).isTrue();
        assertThat(income.get("place").isNull()).isTrue();
    }

    @Test
    @DisplayName("#42 고정지출 행은 장소가 null 이다 — 고정지출에 장소 개념이 없다")
    void fixedRowHasNoPlace() throws Exception {
        Fixture fixture = prepare();

        assertThat(firstOfType(ledger(fixture), "FIXED").get("place").isNull()).isTrue();
    }

    @Test
    @DisplayName("#43 삭제 표시된 수단을 쓰던 행도 이름이 그대로 읽힌다")
    void deletedReferenceStillResolves() throws Exception {
        Fixture fixture = prepare();
        ledger(fixture);
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.expenseMethodId(),
                fixture.token()))).isEqualTo(200);

        JsonNode response = ledger(fixture);

        // 수단은 물리 삭제가 아니라 삭제 표시라 행이 남는다. FIXED 는 연관을 타는데도
        // 이름이 읽히고, 조회가 3003 으로 죽지 않는다(FR-426).
        assertThat(resCode(response)).isEqualTo(200);
        assertThat(firstOfType(response, "FIXED").get("paymentMethodName").asString())
                .isEqualTo("국민카드");
    }
}

package com.dbdomino.moneylog.backend.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 3.1 지출 등록 — quickstart #1·#4·#5·#12·#13·#14.
 *
 * <p><b>#5 가 이 클래스의 핵심이다.</b> 장소 101자·내용 256자를 애플리케이션이 먼저 잡지
 * 않으면 DB 가 거절하고 그 실패는 {@code 9000}(서버 오류) + HTTP 500 으로 나간다 —
 * 사용자 입력 문제인데 서버 장애처럼 보인다. 컬럼 길이는 {@code place varchar(100)} ·
 * {@code content varchar(255)} 다.
 */
class ExpenseCreateIT extends AbstractExpenseIT {

    @Test
    @DisplayName("#1 사용 중 수단·유형으로 등록하면 이름 2개가 스냅샷으로 저장된다")
    void createStoresBothNameSnapshots() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = create(fixture);

        assertThat(resCode(response)).isEqualTo(200);
        long expenseId = response.get("data").get("expenseId").asLong();

        // 응답은 PK 만 준다(3.1). 스냅샷이 실제로 들어갔는지는 DB 로 본다.
        var row = row(expenseId);
        assertThat(row.get("payment_method_name")).isEqualTo("국민카드");
        assertThat(row.get("expend_group_name")).isEqualTo("식비");
        assertThat(row.get("payment_method_idx")).isEqualTo(fixture.paymentMethodId());
        assertThat(row.get("expend_group_idx")).isEqualTo(fixture.expendGroupId());
    }

    @Test
    @DisplayName("#1 일시불이면 할부 3컬럼이 전부 NULL 이다")
    void lumpSumLeavesInstallmentColumnsNull() throws Exception {
        Fixture fixture = prepare();

        var row = row(createExpense(fixture));

        assertThat(row.get("installment_group_id")).isNull();
        assertThat(row.get("installment_index")).isNull();
        assertThat(row.get("installment_total")).isNull();
    }

    @Test
    @DisplayName("등록 응답은 expenseId 한 칸이다 — 상세는 3.2 로 읽는다")
    void createResponseCarriesOnlyThePk() throws Exception {
        Fixture fixture = prepare();

        JsonNode data = create(fixture).get("data");

        assertThat(data.has("expenseId")).isTrue();
        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("#4 금액이 0 이하면 3201 이다")
    void nonPositiveAmountIs3201() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                0L, "2026-03-15", "편의점", "점심"))).isEqualTo(3201);
        assertThat(resCode(create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                -1000L, "2026-03-15", "편의점", "점심"))).isEqualTo(3201);
    }

    @Test
    @DisplayName("#5 장소가 101자면 3201 이다 — 9000 이 아니다")
    void tooLongPlaceIs3201() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-03-15", "가".repeat(101), "점심");

        // 9000 이 나오면 DB 오류가 그대로 새어 나온 것이다.
        assertThat(resCode(response)).isEqualTo(3201);
    }

    @Test
    @DisplayName("#5 내용이 256자면 3201 이다 — 9000 이 아니다")
    void tooLongContentIs3201() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-03-15", "편의점", "나".repeat(256));

        assertThat(resCode(response)).isEqualTo(3201);
    }

    @Test
    @DisplayName("#5 경계값(장소 100자·내용 255자)은 통과한다")
    void boundaryLengthsAreAccepted() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-03-15", "가".repeat(100), "나".repeat(255));

        // 상한을 하나 낮게 잡은 구현은 여기서만 걸린다.
        assertThat(resCode(response)).isEqualTo(200);
    }

    @Test
    @DisplayName("결제일 형식이 YYYY-MM-DD 가 아니면 3201 이다")
    void malformedDateIs3201() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026/03/15", "편의점", "점심"))).isEqualTo(3201);
        assertThat(resCode(create(fixture, fixture.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-13-01", "편의점", "점심"))).isEqualTo(3201);
    }

    @Test
    @DisplayName("#12 같은 날짜·금액·수단으로 두 건을 등록해도 둘 다 성공한다")
    void duplicatesAreAllowed() throws Exception {
        Fixture fixture = prepare();

        long first = createExpense(fixture);
        long second = createExpense(fixture);

        // 업무 유일 제약을 두지 않는다(FR-309) — 실제로 같은 날 같은 금액을 두 번 쓸 수 있다.
        assertThat(first).isNotEqualTo(second);
        assertThat(countExpenses(fixture.member())).isEqualTo(2);
    }

    @Test
    @DisplayName("#13 사용 안 함 수단으로 등록하면 3003 이다")
    void notInUsePaymentMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                {"inUse":false}
                """))).isEqualTo(200);

        assertThat(resCode(create(fixture))).isEqualTo(3003);
    }

    @Test
    @DisplayName("#13 삭제 표시된 수단으로 등록하면 3003 이다 — SC-310")
    void deletedPaymentMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        assertThat(resCode(create(fixture))).isEqualTo(3003);
    }

    @Test
    @DisplayName("#13 없는 수단·남의 수단도 같은 3003 이다")
    void missingOrOthersPaymentMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        Fixture other = prepare();

        assertThat(resCode(create(fixture, 999999999L, fixture.expendGroupId(),
                12000L, "2026-03-15", "편의점", "점심"))).isEqualTo(3003);
        // 남의 수단을 지정해도 같은 코드다 — 갈리면 존재 여부가 새어 나간다.
        assertThat(resCode(create(fixture, other.paymentMethodId(), fixture.expendGroupId(),
                12000L, "2026-03-15", "편의점", "점심"))).isEqualTo(3003);
    }

    @Test
    @DisplayName("#14 사용 안 함·삭제 표시된 지출유형으로 등록하면 3103 이다")
    void unusableExpendGroupIs3103() throws Exception {
        Fixture fixture = prepare();
        long groupId = fixture.expendGroupId();
        tx.executeWithoutResult(status -> jdbc.update(
                "update moneylog.tbl_user_expend_group set in_use = false where idx = ?", groupId));

        assertThat(resCode(create(fixture))).isEqualTo(3103);
    }

    @Test
    @DisplayName("#14 없는 유형·남의 유형도 같은 3103 이다")
    void missingOrOthersExpendGroupIs3103() throws Exception {
        Fixture fixture = prepare();
        Fixture other = prepare();

        assertThat(resCode(create(fixture, fixture.paymentMethodId(), 999999999L,
                12000L, "2026-03-15", "편의점", "점심"))).isEqualTo(3103);
        assertThat(resCode(create(fixture, fixture.paymentMethodId(), other.expendGroupId(),
                12000L, "2026-03-15", "편의점", "점심"))).isEqualTo(3103);
    }

    @Test
    @DisplayName("필수 필드를 빠뜨리면 9001 이다 — 3201 이 아니다")
    void missingRequiredFieldIs9001() throws Exception {
        Fixture fixture = prepare();

        JsonNode response = postJson(URL, fixture.token(), """
                {"paymentMethodId":%d,"expendGroupId":%d,"amount":12000}
                """.formatted(fixture.paymentMethodId(), fixture.expendGroupId()));

        // 누락은 9001, 값 오류는 3201 — 둘을 갈라 붙였는지 본다.
        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("토큰 없이 등록하면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        assertThat(resCode(postJson(URL, null, """
                {"paymentMethodId":1,"expendGroupId":1,"amount":12000,
                 "paymentDate":"2026-03-15","place":"편의점","content":"점심"}
                """))).isEqualTo(1001);
    }
}

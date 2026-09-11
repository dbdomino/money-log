package com.dbdomino.moneylog.backend.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 행 식별자와 할부 표시 — quickstart #41·#51 (FR-424).
 *
 * <h2>왜 숫자가 아니라 문자열인가</h2>
 *
 * <p>네 출처의 PK 가 서로 다른 테이블에서 나온다. 숫자만으로는 {@code expense.idx = 5} 와
 * {@code income.idx = 5} 가 구분되지 않아 프론트가 목록의 행을 특정할 수 없다.
 *
 * <h2>{@code FIXED} 만 세 조각인 이유</h2>
 *
 * <p>{@code sourceId}(= {@code fixedExpenseId})만으로는 월별 내역 1행이 특정되지 않는다 —
 * 같은 고정지출이 여러 달에 걸쳐 행을 갖기 때문이다. 유니크 제약
 * {@code ux_fixed_expense_monthly (fixed_expense_idx, year, month)} 와 같은 조합이다.
 *
 * <p><b>월별 내역의 PK({@code idx})를 쓰지 않는다.</b> 1행을 특정할 수는 있지만 프론트가
 * 그 값으로 4.6 을 부를 수 없다 — 4.6 의 Path 가
 * {@code /monthly/{year}/{month}/{fixedExpenseId}} 라 세 값이 필요하다.
 */
class LedgerItemIdIT extends AbstractLedgerIT {

    @Test
    @DisplayName("#51 EXPENSE·INSTALLMENT 는 expense:{id} 다")
    void expenseIdsUseTheExpensePrefix() throws Exception {
        Fixture fixture = prepare();
        JsonNode response = ledger(fixture);

        JsonNode plain = firstOfType(response, "EXPENSE");
        JsonNode installment = firstOfType(response, "INSTALLMENT");

        // 할부도 tbl_expense 행이라 접두사가 같다 — type 이 둘을 가른다.
        assertThat(plain.get("ledgerItemId").asString())
                .isEqualTo("expense:" + plain.get("sourceId").asLong());
        assertThat(installment.get("ledgerItemId").asString())
                .isEqualTo("expense:" + installment.get("sourceId").asLong());
    }

    @Test
    @DisplayName("#51 INCOME 은 income:{id} 다")
    void incomeIdUsesTheIncomePrefix() throws Exception {
        Fixture fixture = prepare();

        JsonNode income = firstOfType(ledger(fixture), "INCOME");

        assertThat(income.get("ledgerItemId").asString())
                .isEqualTo("income:" + income.get("sourceId").asLong());
        assertThat(income.get("sourceId").asLong()).isEqualTo(fixture.incomeId());
    }

    @Test
    @DisplayName("#51 FIXED 는 fixed:{id}:{year}:{month} 세 조각이다")
    void fixedIdHasThreeParts() throws Exception {
        Fixture fixture = prepare();

        JsonNode fixed = firstOfType(ledger(fixture), "FIXED");

        assertThat(fixed.get("ledgerItemId").asString())
                .isEqualTo("fixed:%d:%d:%d".formatted(fixture.fixedExpenseId(), YEAR, MONTH));
    }

    @Test
    @DisplayName("#51 FIXED 의 sourceId 는 고정지출 설정 PK 다 — 월별 내역 PK 가 아니다")
    void fixedSourceIdIsTheSettingPk() throws Exception {
        Fixture fixture = prepare();

        JsonNode fixed = firstOfType(ledger(fixture), "FIXED");

        // 이 값으로 4.6 의 Path 를 만든다. 월별 내역의 idx 를 실으면 부를 수 없다.
        assertThat(fixed.get("sourceId").asLong()).isEqualTo(fixture.fixedExpenseId());
    }

    @Test
    @DisplayName("#51 같은 고정지출도 달이 다르면 ledgerItemId 가 다르다")
    void sameSettingDifferentMonthsGetDifferentIds() throws Exception {
        Fixture fixture = prepare();

        String july = firstOfType(ledger(fixture), "FIXED").get("ledgerItemId").asString();
        JsonNode august = getJson(URL + "?year=2026&month=8&type=FIXED", fixture.token());
        String augustId = august.get("data").get("list").get(0).get("ledgerItemId").asString();

        // sourceId 만 쓰면 두 달이 같은 값이 되어 프론트가 행을 구분하지 못한다.
        assertThat(july).isNotEqualTo(augustId);
        assertThat(july).endsWith(":2026:7");
        assertThat(augustId).endsWith(":2026:8");
    }

    @Test
    @DisplayName("#51 한 목록 안에서 ledgerItemId 가 중복되지 않는다")
    void idsAreUniqueWithinTheList() throws Exception {
        Fixture fixture = prepare();

        assertThat(itemIdsOf(ledger(fixture))).doesNotHaveDuplicates().hasSize(4);
    }

    @Test
    @DisplayName("#41 할부 행이 type=INSTALLMENT 이고 할부 3필드를 갖는다")
    void installmentRowCarriesItsGroupInfo() throws Exception {
        Fixture fixture = prepare();

        JsonNode installment = firstOfType(ledger(fixture), "INSTALLMENT");

        assertThat(installment.get("installmentGroupId").asLong())
                .isEqualTo(fixture.installmentGroupId());
        assertThat(installment.get("installmentIndex").asInt()).isEqualTo(1);
        assertThat(installment.get("installmentTotal").asInt()).isEqualTo(3);
    }

    @Test
    @DisplayName("#41 일반 지출 행의 할부 3필드는 전부 null 이다")
    void plainExpenseHasNoInstallmentFields() throws Exception {
        Fixture fixture = prepare();

        JsonNode plain = firstOfType(ledger(fixture), "EXPENSE");

        // 004 가 "셋 다 비거나 셋 다 채워진다"를 유지하므로 하나만 봐도 판정이 서지만,
        // 응답에서도 셋이 함께 비어야 프론트가 그 규칙을 믿을 수 있다.
        assertThat(plain.get("installmentGroupId").isNull()).isTrue();
        assertThat(plain.get("installmentIndex").isNull()).isTrue();
        assertThat(plain.get("installmentTotal").isNull()).isTrue();
    }

    @Test
    @DisplayName("#41 다음 달에는 할부 2회차가 나온다")
    void nextMonthShowsTheSecondSlice() throws Exception {
        Fixture fixture = prepare();

        JsonNode august = getJson(URL + "?year=2026&month=8&type=INSTALLMENT", fixture.token());

        JsonNode item = august.get("data").get("list").get(0);
        assertThat(item.get("installmentIndex").asInt()).isEqualTo(2);
        assertThat(item.get("installmentGroupId").asLong())
                .isEqualTo(fixture.installmentGroupId());
    }
}

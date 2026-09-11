package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 말일 보정 — quickstart #18·#19 (SC-404 · FR-409).
 *
 * <p>설정의 {@code payment_day_of_month} 는 1~31 인데 2월에는 31일이 없다. 보정은
 * <b>월별 내역을 만들 때 한 번</b> 하고 결과를 저장한다.
 *
 * <p><b>저장된 값을 DB 에서 직접 읽어 확인한다.</b> 응답만 보면 조회 때마다 다시 계산하는
 * 구현도 통과해 버리는데, 그러면 4.5 와 4.8 이 각자 계산하다 한쪽만 윤년을 빠뜨렸을 때
 * 같은 달의 결제일이 두 화면에서 다르게 보인다.
 */
class MonthlyPaymentDateIT extends AbstractMonthlyIT {

    /** 저장된 결제일. {@code java.sql.Date} 로 오므로 {@code LocalDate} 로 맞춘다. */
    private LocalDate storedPaymentDate(Fixture fixture, int year, int month) {
        Object value = monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), year, month)
                .get("payment_date");
        return value instanceof Date date ? date.toLocalDate() : (LocalDate) value;
    }

    @Test
    @DisplayName("#18 결제일 31, 대상이 2026-02 면 2026-02-28 이다")
    void februaryClampsToTwentyEighth() throws Exception {
        Fixture fixture = prepareWide(31);

        listMonthly(fixture, 2026, 2);

        assertThat(storedPaymentDate(fixture, 2026, 2)).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("#19 결제일 31, 대상이 2028-02 면 2028-02-29 다 — 윤년")
    void leapFebruaryClampsToTwentyNinth() throws Exception {
        Fixture fixture = prepareWide(31);

        listMonthly(fixture, 2028, 2);

        // 28 로 고정한 구현은 여기서만 걸린다.
        assertThat(storedPaymentDate(fixture, 2028, 2)).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("#18 결제일 25 는 2월에도 그대로다 — 보정이 필요 없다")
    void dayWithinTheMonthIsUnchanged() throws Exception {
        Fixture fixture = prepareWide(25);

        listMonthly(fixture, 2026, 2);

        assertThat(storedPaymentDate(fixture, 2026, 2)).isEqualTo(LocalDate.of(2026, 2, 25));
    }

    @Test
    @DisplayName("결제일 31 은 30일 달에서 30일이 된다")
    void thirtyDayMonthClampsToThirty() throws Exception {
        Fixture fixture = prepareWide(31);

        listMonthly(fixture, 2026, 4);

        assertThat(storedPaymentDate(fixture, 2026, 4)).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("결제일 31 은 31일 달에서 그대로 31일이다")
    void thirtyOneDayMonthKeepsThirtyOne() throws Exception {
        Fixture fixture = prepareWide(31);

        listMonthly(fixture, 2026, 3);

        assertThat(storedPaymentDate(fixture, 2026, 3)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("#18 응답의 결제일도 저장된 값과 같다")
    void responseMatchesTheStoredValue() throws Exception {
        Fixture fixture = prepareWide(31);

        String fromResponse = listMonthly(fixture, 2026, 2)
                .get("data").get("list").get(0).get("paymentDate").asString();

        assertThat(fromResponse).isEqualTo("2026-02-28");
        assertThat(storedPaymentDate(fixture, 2026, 2)).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}

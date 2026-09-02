package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserFixedExpenseMonthly;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 월별 고정지출 내역의 그 달 1건 강제 — quickstart.md §3 시나리오 #11·#19.
 *
 * <p>확인하려는 것: 한 고정지출의 한 연·월에 행이 <b>둘 이상 생길 수 없는가</b>다
 * (FR-053). 이 제약이 없으면 lazy 생성이 조회할 때마다 행을 하나씩 더 만들고,
 * 그 달 합계가 조회 횟수만큼 불어난다.
 *
 * <p>유일 범위가 {@code (fixed_expense_idx, year, month)}인 것도 함께 본다 — 회원이
 * 아니라 <b>고정지출 단위</b>다. 회원 단위였다면 한 달에 고정지출 하나만 가질 수 있다.
 *
 * <p>적용 기간이 해를 넘기는 픽스처를 쓴다(2026-11 ~ 2027-02). 연·월을 따로 다루는
 * 실수가 있으면 여기서 드러난다.
 */
class FixedExpenseMonthlyUniqueIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserFixedExpenseRepository fixedExpenseRepository;

    @Autowired
    private UserFixedExpenseMonthlyRepository monthlyRepository;

    @Test
    @DisplayName("#11 같은 고정지출의 같은 연·월 내역을 두 번 만들면 두 번째가 거부된다")
    void rejectsDuplicateMonthWithinSameFixedExpense() {
        UserFixedExpense fixed = givenFixedExpense();

        inTx(() -> monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 12)));

        assertViolatesConstraint(() ->
                monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 12)),
                "ux_fixed_expense_monthly");
    }

    @Test
    @DisplayName("#11 같은 달이라도 고정지출이 다르면 각각 내역을 가질 수 있다")
    void allowsSameMonthAcrossDifferentFixedExpenses() {
        UserFixedExpense rent = givenFixedExpense();
        UserFixedExpense internet = inTx(() -> {
            UserFixedExpense another = newFixedExpense(
                    rent.getUser(), rent.getPaymentMethod(), rent.getExpendGroup());
            another.setName("인터넷");
            another.setAmount(35_000L);
            return fixedExpenseRepository.saveAndFlush(another);
        });

        inTx(() -> {
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(rent, 2026, 12));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(internet, 2026, 12));
        });

        assertThat(inTx(() -> monthlyRepository.findByUserIdKeyAndYearAndMonth(
                rent.getUser().getIdKey(), 2026, 12))).hasSize(2);
    }

    @Test
    @DisplayName("#11 한 달의 금액만 고쳐도 다른 달은 영향받지 않는다")
    void editingOneMonthLeavesOtherMonthsAlone() {
        UserFixedExpense fixed = givenFixedExpense();

        inTx(() -> {
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 12));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2027, 1));
        });

        // 12월만 사용자가 직접 고친다
        inTx(() -> {
            UserFixedExpenseMonthly december = monthlyRepository
                    .findByFixedExpenseIdxAndYearAndMonth(fixed.getIdx(), 2026, 12).orElseThrow();
            december.setAmount(550_000L);
            december.setModified(true);
            monthlyRepository.saveAndFlush(december);
        });

        UserFixedExpenseMonthly december = inTx(() -> monthlyRepository
                .findByFixedExpenseIdxAndYearAndMonth(fixed.getIdx(), 2026, 12).orElseThrow());
        UserFixedExpenseMonthly january = inTx(() -> monthlyRepository
                .findByFixedExpenseIdxAndYearAndMonth(fixed.getIdx(), 2027, 1).orElseThrow());

        assertThat(december.getAmount()).isEqualTo(550_000L);
        assertThat(december.getModified()).isTrue();
        // 다른 달은 설정값 그대로다 — 손대지 않은 달은 modified 가 거짓이라 설정 변경을 따라간다
        assertThat(january.getAmount()).isEqualTo(500_000L);
        assertThat(january.getModified()).isFalse();
    }

    @Test
    @DisplayName("결제일 31은 그 달 말일로 보정돼 저장된다")
    void paymentDateIsClampedToMonthEnd() {
        UserFixedExpense fixed = givenFixedExpense();

        inTx(() -> {
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 11));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2027, 2));
        });

        // 11월은 30일, 2027년 2월은 28일까지다. 설정의 31일을 그대로 넣으면 저장 자체가 실패한다
        assertThat(inTx(() -> monthlyRepository
                .findByFixedExpenseIdxAndYearAndMonth(fixed.getIdx(), 2026, 11).orElseThrow()
                .getPaymentDate())).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(inTx(() -> monthlyRepository
                .findByFixedExpenseIdxAndYearAndMonth(fixed.getIdx(), 2027, 2).orElseThrow()
                .getPaymentDate())).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    @DisplayName("#19 월 13인 내역은 CHECK 제약에 막힌다")
    void rejectsMonthOutOfRange() {
        UserFixedExpense fixed = givenFixedExpense();

        assertViolatesConstraint(() -> {
            UserFixedExpenseMonthly monthly = newFixedExpenseMonthly(fixed, 2026, 12);
            monthly.setMonth(13);
            monthlyRepository.saveAndFlush(monthly);
        }, "ck_fixed_monthly_month");
    }

    @Test
    @DisplayName("금액 0인 월별 내역은 CHECK 제약에 막힌다")
    void rejectsNonPositiveMonthlyAmount() {
        UserFixedExpense fixed = givenFixedExpense();

        assertViolatesConstraint(() -> {
            UserFixedExpenseMonthly monthly = newFixedExpenseMonthly(fixed, 2026, 12);
            monthly.setAmount(0L);
            monthlyRepository.saveAndFlush(monthly);
        }, "ck_fixed_monthly_amount");
    }

    @Test
    @DisplayName("결제일 32인 고정지출은 CHECK 제약에 막힌다")
    void rejectsPaymentDayOutOfRange() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "주거")));

        assertViolatesConstraint(() -> {
            UserFixedExpense fixed = newFixedExpense(user, method, group);
            fixed.setPaymentDayOfMonth(32);
            fixedExpenseRepository.saveAndFlush(fixed);
        }, "ck_fixed_expense_day");
    }

    @Test
    @DisplayName("종료 연월이 시작 연월보다 앞선 고정지출은 CHECK 제약에 막힌다")
    void rejectsPeriodEndingBeforeItStarts() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "주거")));

        // 2027-02 시작, 2026-11 종료. 연·월을 따로 비교하면 "11 > 2"라 통과해 버린다
        assertViolatesConstraint(() -> {
            UserFixedExpense fixed = newFixedExpense(user, method, group);
            fixed.setStartYear(2027);
            fixed.setStartMonth(2);
            fixed.setEndYear(2026);
            fixed.setEndMonth(11);
            fixedExpenseRepository.saveAndFlush(fixed);
        }, "ck_fixed_expense_period");
    }

    @Test
    @DisplayName("적용 기간 조회는 해를 넘겨도 그 사이 달을 모두 포함한다")
    void applicableQuerySpansYearBoundary() {
        UserFixedExpense fixed = givenFixedExpense();
        Long idKey = fixed.getUser().getIdKey();

        // 2026-11 ~ 2027-02 — 안쪽 4개 달과 바깥쪽 2개 달
        assertThat(applicable(idKey, 2026, 10)).isEmpty();
        assertThat(applicable(idKey, 2026, 11)).hasSize(1);
        assertThat(applicable(idKey, 2026, 12)).hasSize(1);
        assertThat(applicable(idKey, 2027, 1)).hasSize(1);
        assertThat(applicable(idKey, 2027, 2)).hasSize(1);
        assertThat(applicable(idKey, 2027, 3)).isEmpty();

        // Entity 쪽 판정도 같은 답을 낸다 — 두 곳의 식이 갈라지면 여기서 드러난다
        assertThat(fixed.coversYearMonth(2027, 1)).isTrue();
        assertThat(fixed.coversYearMonth(2026, 10)).isFalse();
    }

    private List<UserFixedExpense> applicable(Long idKey, int year, int month) {
        return inTx(() -> fixedExpenseRepository.findApplicableTo(
                idKey, UserFixedExpense.yearMonthValue(year, month)));
    }

    /** 회원·수단·유형·고정지출을 저장하고 고정지출을 돌려준다. */
    private UserFixedExpense givenFixedExpense() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "주거")));
        return inTx(() ->
                fixedExpenseRepository.saveAndFlush(newFixedExpense(user, method, group)));
    }
}

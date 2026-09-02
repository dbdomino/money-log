package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserIncome;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserIncomeRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 이름 스냅샷 보존 — quickstart.md §3 시나리오 #6.
 *
 * <p>확인하려는 것: 지출·수입에 적힌 수단·유형 이름은 <b>등록 당시 값</b>이며,
 * 원본이 나중에 바뀌거나 삭제 표시돼도 따라가지 않는다(FR-042). 3월에 "국민카드"로
 * 적은 지출이 카드 이름을 바꿨다고 소급해 달라지면 그때의 가계부가 아니게 된다.
 *
 * <p>스냅샷이 있어도 <b>참조는 남긴다</b>. 그래서 이 테스트는 원본 행이 삭제 표시된
 * 뒤에도 지출이 그대로 읽히는지까지 본다 — 수단·유형이 물리 삭제였다면 FK RESTRICT가
 * 삭제를 막거나, FK를 아예 못 걸었을 것이다(research §4).
 *
 * <p>소득도 같은 규칙이라 여기서 함께 본다(FR-046). 소득 전용 IT를 따로 두면
 * 같은 픽스처가 두 벌이 되고, 확인하려는 성질은 하나다. {@code ck_income_amount}도
 * 소득을 다루는 유일한 이 클래스에서 확인한다.
 */
class ExpenseSnapshotIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserExpenseRepository expenseRepository;

    @Autowired
    private UserIncomeRepository incomeRepository;

    @Test
    @DisplayName("#6 수단·유형 이름을 바꾸고 삭제 표시해도 지출은 등록 당시 이름을 유지한다")
    void expenseKeepsNamesCapturedAtRegistration() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        UserExpense saved = inTx(() -> expenseRepository.saveAndFlush(
                newExpense(user, method, group, 12_000L, LocalDate.of(2026, 3, 14))));

        // 원본을 바꾸고 삭제 표시한다
        inTx(() -> {
            UserPaymentMethod target = paymentMethodRepository.findById(method.getIdx()).orElseThrow();
            target.setName("신한카드");
            target.setDeleted(true);
            paymentMethodRepository.saveAndFlush(target);

            UserExpendGroup targetGroup = expendGroupRepository.findById(group.getIdx()).orElseThrow();
            targetGroup.setName("외식");
            targetGroup.setDeleted(true);
            expendGroupRepository.saveAndFlush(targetGroup);
        });

        // 연관이 지연 로딩이라 단언을 트랜잭션 안에서 한다. 밖에서 만지면
        // LazyInitializationException 이 나서 정작 확인하려던 것이 가려진다.
        inTx(() -> {
            UserExpense found = expenseRepository.findById(saved.getIdx()).orElseThrow();

            assertThat(found.getPaymentMethodName()).isEqualTo("국민카드");
            assertThat(found.getExpendGroupName()).isEqualTo("식비");
            // 참조도 끊기지 않는다 — 삭제 표시는 UPDATE라 행이 남는다
            assertThat(found.getPaymentMethod().getIdx()).isEqualTo(method.getIdx());
            assertThat(found.getExpendGroup().getIdx()).isEqualTo(group.getIdx());
            // 원본을 따라가면 안 되지만, 원본 자체는 바뀌어 있어야 한다
            // (바뀌지 않았다면 이 시험은 아무것도 확인하지 못한 것이다)
            assertThat(found.getPaymentMethod().getName()).isEqualTo("신한카드");
            assertThat(found.getExpendGroup().getName()).isEqualTo("외식");
            assertThat(found.getPaymentMethod().getDeleted()).isTrue();
            assertThat(found.getExpendGroup().getDeleted()).isTrue();
        });
    }

    @Test
    @DisplayName("#6 수단 이름을 바꿔도 수입은 등록 당시 이름을 유지한다")
    void incomeKeepsPaymentMethodNameCapturedAtRegistration() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() -> {
            UserPaymentMethod account = newPaymentMethod(user);
            account.setName("월급통장");
            account.setType(UserPaymentMethod.TYPE_ACCOUNT);
            account.setPurpose(UserPaymentMethod.PURPOSE_INCOME);
            account.setCardExpiry(null);
            return paymentMethodRepository.saveAndFlush(account);
        });

        UserIncome saved = inTx(() -> incomeRepository.saveAndFlush(
                newIncome(user, method, 3_200_000L, LocalDate.of(2026, 3, 25))));

        inTx(() -> {
            UserPaymentMethod target = paymentMethodRepository.findById(method.getIdx()).orElseThrow();
            target.setName("주거래통장");
            target.setDeleted(true);
            paymentMethodRepository.saveAndFlush(target);
        });

        inTx(() -> {
            UserIncome found = incomeRepository.findById(saved.getIdx()).orElseThrow();

            assertThat(found.getPaymentMethodName()).isEqualTo("월급통장");
            assertThat(found.getPaymentMethod().getName()).isEqualTo("주거래통장");
            // 내용은 선택 항목이라 비어 있어도 저장된다
            assertThat(found.getContent()).isNull();
        });
    }

    @Test
    @DisplayName("기간 조회는 양 끝 날짜를 포함하고 범위 밖은 제외한다")
    void periodQueryIncludesBothBounds() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        inTx(() -> {
            expenseRepository.saveAndFlush(
                    newExpense(user, method, group, 1_000L, LocalDate.of(2026, 2, 28)));
            expenseRepository.saveAndFlush(
                    newExpense(user, method, group, 2_000L, LocalDate.of(2026, 3, 1)));
            expenseRepository.saveAndFlush(
                    newExpense(user, method, group, 3_000L, LocalDate.of(2026, 3, 31)));
            expenseRepository.saveAndFlush(
                    newExpense(user, method, group, 4_000L, LocalDate.of(2026, 4, 1)));
        });

        assertThat(inTx(() -> expenseRepository.findByUserIdKeyAndPaymentDateBetween(
                user.getIdKey(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))))
                .extracting(UserExpense::getAmount)
                .containsExactlyInAnyOrder(2_000L, 3_000L);
    }

    @Test
    @DisplayName("지출유형 사용 이력 확인은 그 유형을 쓴 지출이 생긴 뒤에만 참이 된다")
    void expendGroupUsageIsDetectable() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup used = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));
        UserExpendGroup unused = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "취미")));

        inTx(() -> expenseRepository.saveAndFlush(
                newExpense(user, method, used, 5_000L, LocalDate.of(2026, 3, 14))));

        // 3106 판정 — 삭제 표시(UPDATE)라 FK RESTRICT가 대신 막아주지 못한다
        assertThat(inTx(() -> expenseRepository.existsByExpendGroupIdx(used.getIdx()))).isTrue();
        assertThat(inTx(() -> expenseRepository.existsByExpendGroupIdx(unused.getIdx()))).isFalse();
    }

    @Test
    @DisplayName("금액 0인 지출은 CHECK 제약에 막힌다")
    void rejectsNonPositiveExpenseAmount() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        assertViolatesConstraint(() -> expenseRepository.saveAndFlush(
                newExpense(user, method, group, 0L, LocalDate.of(2026, 3, 14))),
                "ck_expense_amount");
    }

    @Test
    @DisplayName("금액 0인 수입은 CHECK 제약에 막힌다")
    void rejectsNonPositiveIncomeAmount() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));

        assertViolatesConstraint(() -> incomeRepository.saveAndFlush(
                newIncome(user, method, 0L, LocalDate.of(2026, 3, 25))),
                "ck_income_amount");
    }

    private UserIncome newIncome(User user, UserPaymentMethod method,
                                 long amount, LocalDate paymentDate) {
        UserIncome income = new UserIncome();
        income.setUser(user);
        income.setPaymentMethod(method);
        income.setPaymentMethodName(method.getName());
        income.setAmount(amount);
        income.setPaymentDate(paymentDate);
        stampAudit(income, user.getIdKey());
        return income;
    }
}

package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserFixedExpense;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseMonthlyRepository;
import com.dbdomino.moneylog.data.repository.UserFixedExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 고정지출 삭제의 파급 — quickstart.md §3 시나리오 #13.
 *
 * <p>확인하려는 것: 고정지출 관리 행을 지우면 그 고정지출의 월별 내역이
 * <b>지난 달 것까지 전부</b> 함께 사라지는가다(FR-059). 고정지출을 지웠다는 것은
 * 그 항목 자체를 없앤다는 뜻이라 과거 내역만 남겨 두면 출처 없는 지출이 된다.
 *
 * <p>이 동작은 <b>DB의 {@code ON DELETE CASCADE}</b>가 한다. 애플리케이션이 자식을
 * 먼저 지우는 방식이면 삭제 경로가 늘어날 때마다 그 순서를 다시 적어야 하고, 한 곳만
 * 빠뜨려도 FK 위반으로 삭제가 막힌다. Hibernate는 {@code @JoinColumn}만으로는
 * {@code CASCADE}를 DDL에 내지 않아 {@code @OnDelete}가 필요한데, 그것이 실제로
 * 붙었는지를 여기서 확인한다.
 *
 * <p>반대로 수단·지출유형 참조는 {@code RESTRICT}다. 그래서 이 테스트는 고정지출을
 * 지운 뒤에도 수단·유형 행이 남아 있는지를 함께 본다 — CASCADE가 옆으로 번지면
 * 다른 지출의 참조까지 끊긴다.
 */
class FixedExpenseCascadeIT extends AbstractSchemaIT {

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
    @DisplayName("#13 고정지출을 지우면 그 월별 내역이 지난 달 포함 전부 사라진다")
    void deletingFixedExpenseRemovesAllItsMonthlyRows() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "주거")));
        UserFixedExpense fixed = inTx(() ->
                fixedExpenseRepository.saveAndFlush(newFixedExpense(user, method, group)));

        // 적용 기간 전체 4개 달을 만든다 — 지난 달·이번 달 구분 없이 전부 사라져야 한다
        inTx(() -> {
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 11));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2026, 12));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2027, 1));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(fixed, 2027, 2));
        });

        assertThat(countMonthlyRows(fixed)).isEqualTo(4);

        inTx(() -> fixedExpenseRepository.deleteById(fixed.getIdx()));

        assertThat(countMonthlyRows(fixed)).isZero();
        assertThat(inTx(() -> fixedExpenseRepository.findById(fixed.getIdx()))).isEmpty();

        // CASCADE 가 옆으로 번지지 않는다 — 수단·유형은 RESTRICT 참조라 그대로 남는다
        assertThat(inTx(() -> paymentMethodRepository.findById(method.getIdx()))).isPresent();
        assertThat(inTx(() -> expendGroupRepository.findById(group.getIdx()))).isPresent();
    }

    @Test
    @DisplayName("#13 한 고정지출을 지워도 다른 고정지출의 월별 내역은 남는다")
    void deletingOneFixedExpenseLeavesOthersIntact() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "주거")));

        UserFixedExpense rent = inTx(() ->
                fixedExpenseRepository.saveAndFlush(newFixedExpense(user, method, group)));
        UserFixedExpense internet = inTx(() -> {
            UserFixedExpense another = newFixedExpense(user, method, group);
            another.setName("인터넷");
            another.setAmount(35_000L);
            return fixedExpenseRepository.saveAndFlush(another);
        });

        inTx(() -> {
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(rent, 2026, 12));
            monthlyRepository.saveAndFlush(newFixedExpenseMonthly(internet, 2026, 12));
        });

        inTx(() -> fixedExpenseRepository.deleteById(rent.getIdx()));

        List<?> remaining = inTx(() ->
                monthlyRepository.findByUserIdKeyAndYearAndMonth(user.getIdKey(), 2026, 12));

        assertThat(remaining).hasSize(1);
        assertThat(countMonthlyRows(internet)).isEqualTo(1);
    }

    private int countMonthlyRows(UserFixedExpense fixed) {
        return inTx(() -> jdbc.queryForObject(
                "SELECT count(*) FROM tbl_fixed_expense_monthly WHERE fixed_expense_idx = ?",
                Integer.class, fixed.getIdx()));
    }
}

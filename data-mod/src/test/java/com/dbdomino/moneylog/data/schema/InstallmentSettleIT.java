package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserExpense;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserExpenseRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 할부 중도상환 — quickstart.md §3 시나리오 #10.
 *
 * <p>확인하려는 것: 중도상환이 {@code installment_group_id}가 같고 결제일이 오늘보다
 * <b>뒤인</b> 회차만 지우는가다(FR-045). 이미 결제된 회차(오늘·과거)는 남아야 한다 —
 * 함께 지우면 지난달 가계부 금액이 소급해서 줄어든다.
 *
 * <p>경계가 핵심이라 <b>오늘 결제일인 회차</b>를 일부러 끼워 넣는다. 조건이
 * {@code >=}로 잘못 적히면 이 회차가 사라지면서 실패한다. 날짜를 오늘 기준으로
 * 만드는 것도 그 때문이다 — 고정 날짜로는 언젠가 전부 과거가 되어 검사가 헛돈다.
 *
 * <p>중도상환은 물리 삭제다. 수단·지출유형과 달리 지출은 삭제 표시로 남기지 않는다
 * (FR-047).
 */
class InstallmentSettleIT extends AbstractSchemaIT {

    /** 검증용 할부 개월 수. */
    private static final int MONTHS = 12;

    /** 첫 회차를 오늘로부터 2개월 전에 둔다 — 과거 2회차 + 오늘 1회차 + 미래 9회차가 된다. */
    private static final int MONTHS_BEFORE_TODAY = 2;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserExpenseRepository expenseRepository;

    @Test
    @DisplayName("#10 결제일이 오늘보다 뒤인 회차만 지워지고 오늘·과거 회차는 남는다")
    void settlementRemovesOnlyFutureInstallments() {
        LocalDate today = LocalDate.now();
        Long groupId = givenTwelveMonthInstallment(today);

        long removed = inTx(() ->
                expenseRepository.deleteByInstallmentGroupIdAndPaymentDateAfter(groupId, today));

        List<UserExpense> remaining = inTx(() ->
                expenseRepository.findByInstallmentGroupIdOrderByInstallmentIndexAsc(groupId));

        assertThat(removed).isEqualTo(MONTHS - MONTHS_BEFORE_TODAY - 1);
        // 1·2회차는 과거, 3회차가 오늘이다. 셋 다 이미 결제된 것이라 남는다
        assertThat(remaining).extracting(UserExpense::getInstallmentIndex)
                .containsExactly(1, 2, 3);
        assertThat(remaining).extracting(UserExpense::getPaymentDate)
                .allSatisfy(paymentDate -> assertThat(paymentDate).isBeforeOrEqualTo(today));
        // 오늘 결제일인 회차가 실제로 남았는지 — 경계가 >= 로 잘못 적히면 여기서 걸린다
        assertThat(remaining).extracting(UserExpense::getPaymentDate).contains(today);
    }

    @Test
    @DisplayName("#10 중도상환은 다른 할부의 회차를 건드리지 않는다")
    void settlementIsScopedToOneInstallmentGroup() {
        LocalDate today = LocalDate.now();
        Long target = givenTwelveMonthInstallment(today);
        Long other = givenTwelveMonthInstallment(today);

        inTx(() -> expenseRepository.deleteByInstallmentGroupIdAndPaymentDateAfter(target, today));

        assertThat(inTx(() ->
                expenseRepository.findByInstallmentGroupIdOrderByInstallmentIndexAsc(other)))
                .hasSize(MONTHS);
    }

    /**
     * 12회차 할부 1건을 저장하고 그 그룹 식별자를 돌려준다.
     *
     * <p>회차별 결제일은 {@code today}를 기준으로 잡는다. 회원·수단·유형도 매번 새로
     * 만든다 — 두 할부를 비교하는 검사에서 서로의 정리에 영향을 받지 않는다.
     */
    private Long givenTwelveMonthInstallment(LocalDate today) {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "쇼핑")));
        Long groupId = nextInstallmentGroupId();

        inTx(() -> {
            for (int index = 1; index <= MONTHS; index++) {
                UserExpense row = newExpense(user, method, group, 10_000L,
                        today.plusMonths(index - 1L - MONTHS_BEFORE_TODAY));
                row.setContent("냉장고 " + index + "/" + MONTHS + "회차");
                row.setInstallmentGroupId(groupId);
                row.setInstallmentIndex(index);
                row.setInstallmentTotal(MONTHS);
                expenseRepository.saveAndFlush(row);
            }
        });

        return groupId;
    }
}

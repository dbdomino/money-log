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
 * 할부 저장 구조 — quickstart.md §3 시나리오 #9.
 *
 * <p>확인하려는 것: 12개월 할부가 <b>12개의 행</b>이 되고, 그 행들이 같은
 * {@code installment_group_id}를 공유하며 1~12 순번을 갖는가다(FR-043·044).
 * 한 행에 개월 수만 적어 두는 구조였다면 회차별 결제일이 다른 것도, 중도상환으로
 * 남은 회차만 지우는 것도 표현할 수 없다.
 *
 * <p>같은 테이블이 <b>일시불도</b> 담는다. 할부 3개 컬럼이 모두 비면 일시불이므로,
 * 세 컬럼이 nullable인 것과 그 상태로도 저장되는 것을 함께 본다.
 *
 * <p>{@code installment_group_id}는 시퀀스 {@code seq_installment_group}이 발급한다.
 * 이 테스트가 실제 시퀀스를 타므로 시퀀스가 실재하지 않으면 여기서 먼저 드러난다.
 */
class InstallmentIT extends AbstractSchemaIT {

    /** 검증용 할부 개월 수. 12로 두면 회차별 결제일이 이듬해까지 넘어가는 것도 함께 본다. */
    private static final int MONTHS = 12;

    /** 첫 회차 결제일. 12회차가 이듬해로 넘어가므로 연 경계를 함께 밟는다. */
    private static final LocalDate FIRST_PAYMENT_DATE = LocalDate.of(2026, 3, 14);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Autowired
    private UserExpenseRepository expenseRepository;

    @Test
    @DisplayName("#9 12개월 할부는 같은 그룹 식별자를 공유하는 12행이 되고 순번이 1~12다")
    void twelveMonthInstallmentBecomesTwelveRowsSharingOneGroupId() {
        Fixture fixture = givenFixture();
        Long groupId = nextInstallmentGroupId();

        // 한 트랜잭션에 전부 저장한다 — 일부만 남으면 개월 수가 맞지 않는 할부가 된다(FR-044)
        inTx(() -> {
            for (int index = 1; index <= MONTHS; index++) {
                expenseRepository.saveAndFlush(newInstallmentRow(fixture, groupId, index));
            }
        });

        List<UserExpense> rows = inTx(() ->
                expenseRepository.findByInstallmentGroupIdOrderByInstallmentIndexAsc(groupId));

        assertThat(rows).hasSize(MONTHS);
        assertThat(rows).extracting(UserExpense::getInstallmentIndex)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(rows).extracting(UserExpense::getInstallmentTotal)
                .containsOnly(MONTHS);
        assertThat(rows).extracting(UserExpense::getInstallmentGroupId)
                .containsOnly(groupId);
        assertThat(rows).allMatch(UserExpense::isInstallment);
        // 회차별 결제일이 한 달씩 밀리며 이듬해까지 넘어간다
        assertThat(rows.get(0).getPaymentDate()).isEqualTo(FIRST_PAYMENT_DATE);
        assertThat(rows.get(MONTHS - 1).getPaymentDate())
                .isEqualTo(FIRST_PAYMENT_DATE.plusMonths(MONTHS - 1L));
    }

    @Test
    @DisplayName("#9 할부 그룹 식별자는 시퀀스가 발급하므로 할부마다 다르다")
    void installmentGroupIdsAreDistinctPerInstallment() {
        assertThat(nextInstallmentGroupId()).isNotEqualTo(nextInstallmentGroupId());
    }

    @Test
    @DisplayName("할부 3개 컬럼이 모두 비어 있는 일시불 행도 같은 테이블에 저장된다")
    void lumpSumRowStoresWithAllInstallmentColumnsNull() {
        Fixture fixture = givenFixture();

        UserExpense saved = inTx(() -> expenseRepository.saveAndFlush(
                newExpense(fixture.user, fixture.method, fixture.group, 30_000L, FIRST_PAYMENT_DATE)));

        UserExpense found = inTx(() -> expenseRepository.findById(saved.getIdx()).orElseThrow());

        assertThat(found.getInstallmentGroupId()).isNull();
        assertThat(found.getInstallmentIndex()).isNull();
        assertThat(found.getInstallmentTotal()).isNull();
        assertThat(found.isInstallment()).isFalse();
    }

    @Test
    @DisplayName("할부 회차 번호 0은 CHECK 제약에 막힌다")
    void rejectsInstallmentIndexBelowOne() {
        Fixture fixture = givenFixture();
        Long groupId = nextInstallmentGroupId();

        assertViolatesConstraint(() -> {
            UserExpense row = newInstallmentRow(fixture, groupId, 1);
            row.setInstallmentIndex(0);
            expenseRepository.saveAndFlush(row);
        }, "ck_expense_installment_index");
    }

    @Test
    @DisplayName("총 할부 개월 수 1은 CHECK 제약에 막힌다")
    void rejectsInstallmentTotalBelowTwo() {
        Fixture fixture = givenFixture();
        Long groupId = nextInstallmentGroupId();

        // 1개월 할부는 일시불과 같다. 할부로 적히면 중도상환·통계가 헛돈다
        assertViolatesConstraint(() -> {
            UserExpense row = newInstallmentRow(fixture, groupId, 1);
            row.setInstallmentTotal(1);
            expenseRepository.saveAndFlush(row);
        }, "ck_expense_installment_total");
    }

    /** 한 회차 행을 만든다. 결제일은 첫 회차부터 한 달씩 민다. */
    private UserExpense newInstallmentRow(Fixture fixture, Long groupId, int index) {
        UserExpense row = newExpense(fixture.user, fixture.method, fixture.group,
                10_000L, FIRST_PAYMENT_DATE.plusMonths(index - 1L));
        row.setContent("노트북 " + index + "/" + MONTHS + "회차");
        row.setInstallmentGroupId(groupId);
        row.setInstallmentIndex(index);
        row.setInstallmentTotal(MONTHS);
        return row;
    }

    private Fixture givenFixture() {
        User user = inTx(() -> userRepository.save(newUser()));
        UserPaymentMethod method = inTx(() ->
                paymentMethodRepository.saveAndFlush(newPaymentMethod(user)));
        UserExpendGroup group = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "쇼핑")));
        return new Fixture(user, method, group);
    }

    /** 지출 1건을 만들려면 회원·수단·유형 셋이 먼저 있어야 한다. 셋을 묶어 나른다. */
    private record Fixture(User user, UserPaymentMethod method, UserExpendGroup group) {
    }
}

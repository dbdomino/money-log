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
import java.time.LocalDate;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 월별 고정지출 내역의 동시 생성 — quickstart.md §3 시나리오 #12.
 *
 * <p>확인하려는 것: 두 요청이 <b>같은 순간에</b> 같은 연·월 내역을 만들려 해도 행이
 * 하나만 남고, 진 쪽이 예외로 끝나지 않는가다(FR-054).
 *
 * <p>왜 문제가 되는가 — 월별 내역은 그 달을 처음 조회할 때 만들어진다. 월별 내역
 * 조회(4.5)와 월별 가계부 목록(4.8)이 동시에 열리면 두 트랜잭션이 같은 순간에
 * "행이 없음"을 보고 둘 다 INSERT를 시도한다. 애플리케이션의 "있으면 건너뛴다"
 * 검사는 이 창을 닫지 못한다 — 검사와 INSERT 사이에 다른 트랜잭션이 끼어든다.
 *
 * <p>유일 제약만 걸면 두 번째가 <b>예외</b>로 끝나 사용자 화면이 실패한다. 충돌을
 * 정상 흐름으로 흡수하려면 {@code INSERT ... ON CONFLICT DO NOTHING}이 필요하고,
 * 그 구문은 유일 제약이 있어야 성립한다. 둘은 한 쌍이다.
 *
 * <p><b>트랜잭션을 클래스에 걸지 않는다.</b> 한 트랜잭션 안에서는 자기 자신과 경합할
 * 수 없어 이 시험이 성립하지 않는다. 상위 클래스가 이미 그렇게 돼 있다.
 */
class FixedExpenseMonthlyConcurrencyIT extends AbstractSchemaIT {

    private static final int YEAR = 2026;
    private static final int MONTH = 12;

    /** 스레드가 서로를 기다리다 영영 멈추는 일이 없도록 상한을 둔다. */
    private static final int TIMEOUT_SECONDS = 20;

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
    @DisplayName("#12 별도 트랜잭션에서 같은 달을 두 번 만들어도 두 번째는 조용히 아무 일도 하지 않는다")
    void secondInsertIsAbsorbedByOnConflictDoNothing() {
        UserFixedExpense fixed = givenFixedExpense();

        int first = inTx(() -> insertMonthly(fixed));
        int second = inTx(() -> insertMonthly(fixed));

        assertThat(first).isEqualTo(1);
        // 예외가 아니라 0행이다 — 화면이 실패하지 않는다
        assertThat(second).isZero();
        assertThat(inTx(() -> monthlyRepository.findByUserIdKeyAndYearAndMonth(
                fixed.getUser().getIdKey(), YEAR, MONTH))).hasSize(1);
    }

    @Test
    @DisplayName("#12 두 트랜잭션이 동시에 같은 달을 만들어도 행은 하나만 남는다")
    void concurrentInsertsLeaveExactlyOneRow() throws Exception {
        UserFixedExpense fixed = givenFixedExpense();

        // 두 스레드를 같은 지점에서 풀어 실제로 겹치게 한다. 순차 실행이면
        // 두 번째가 이미 커밋된 행을 보게 되어 경합 자체가 일어나지 않는다.
        CyclicBarrier startTogether = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> left = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return inTx(() -> insertMonthly(fixed));
            });
            Future<Integer> right = pool.submit(() -> {
                startTogether.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return inTx(() -> insertMonthly(fixed));
            });

            int inserted = left.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    + right.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 둘 다 예외 없이 끝나고, 실제로 들어간 행은 하나뿐이다
            assertThat(inserted).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        assertThat(inTx(() -> monthlyRepository.findByUserIdKeyAndYearAndMonth(
                fixed.getUser().getIdKey(), YEAR, MONTH))).hasSize(1);
    }

    /** 그 달 내역을 {@code ON CONFLICT DO NOTHING}으로 만든다. 실제로 들어간 행 수를 돌려준다. */
    private int insertMonthly(UserFixedExpense fixed) {
        LocalDate paymentDate = clampToMonthEnd(YEAR, MONTH, fixed.getPaymentDayOfMonth());
        return monthlyRepository.insertIfAbsent(
                fixed.getUser().getIdKey(),
                fixed.getIdx(),
                YEAR,
                MONTH,
                fixed.getAmount(),
                paymentDate,
                fixed.getContent(),
                fixed.getPaymentMethod().getIdx(),
                fixed.getExpendGroup().getIdx(),
                PLACEHOLDER_AUDITOR_ID_KEY);
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

package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 수단 값 제약과 목록 필터.
 *
 * <p>확인하려는 것: {@code type}과 {@code purpose}는 정해진 값만 들어갈 수 있고,
 * "사용 중인 수단 목록"(2.6)이 용도·사용 여부·삭제 표시 <b>세 조건을 모두</b>
 * 거는가다(FR-032). 셋 중 하나라도 빠지면 지출 입력 화면에 소득 수단이나 삭제된
 * 수단이 섞여 나온다.
 */
class PaymentMethodConstraintIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @AfterEach
    void tearDown() {
        cleanUpUsers();
    }

    @Test
    @DisplayName("허용되지 않은 종류 값은 CHECK 제약에 막힌다")
    void rejectsUnsupportedType() {
        User user = inTx(() -> userRepository.save(newUser()));

        assertViolatesConstraint(() -> {
            UserPaymentMethod method = newPaymentMethod(user);
            method.setType("BITCOIN");
            paymentMethodRepository.saveAndFlush(method);
        });
    }

    @Test
    @DisplayName("허용되지 않은 용도 값은 CHECK 제약에 막힌다")
    void rejectsUnsupportedPurpose() {
        User user = inTx(() -> userRepository.save(newUser()));

        assertViolatesConstraint(() -> {
            UserPaymentMethod method = newPaymentMethod(user);
            method.setPurpose("BOTH");
            paymentMethodRepository.saveAndFlush(method);
        });
    }

    @Test
    @DisplayName("사용 중인 수단 목록은 용도·사용 여부·삭제 표시를 모두 건다")
    void activeListFiltersByPurposeInUseAndDeleted() {
        User user = inTx(() -> userRepository.save(newUser()));

        inTx(() -> {
            // 걸려야 하는 것 하나
            paymentMethodRepository.saveAndFlush(
                    named(newPaymentMethod(user), "국민카드"));

            // 용도가 다르다
            UserPaymentMethod income = named(newPaymentMethod(user), "월급통장");
            income.setType(UserPaymentMethod.TYPE_ACCOUNT);
            income.setPurpose(UserPaymentMethod.PURPOSE_INCOME);
            income.setCardExpiry(null);
            paymentMethodRepository.saveAndFlush(income);

            // 사용하지 않는다
            UserPaymentMethod unused = named(newPaymentMethod(user), "안쓰는카드");
            unused.setInUse(false);
            paymentMethodRepository.saveAndFlush(unused);

            // 삭제 표시됐다
            UserPaymentMethod removed = named(newPaymentMethod(user), "지운카드");
            removed.setDeleted(true);
            paymentMethodRepository.saveAndFlush(removed);
        });

        List<UserPaymentMethod> active = inTx(() ->
                paymentMethodRepository.findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalse(
                        user.getIdKey(), UserPaymentMethod.PURPOSE_EXPENSE));

        assertThat(active).extracting(UserPaymentMethod::getName).containsExactly("국민카드");

        // 관리 목록에는 넷 다 보인다 — 삭제는 표시일 뿐 행이 사라지지 않는다
        assertThat(inTx(() -> paymentMethodRepository.findByUserIdKeyOrderByIdxAsc(user.getIdKey())))
                .hasSize(4);
    }

    private UserPaymentMethod newPaymentMethod(User user) {
        UserPaymentMethod method = new UserPaymentMethod();
        method.setUser(user);
        method.setName("국민카드");
        method.setType(UserPaymentMethod.TYPE_CARD);
        method.setPurpose(UserPaymentMethod.PURPOSE_EXPENSE);
        method.setCardExpiry("2028-12");
        stampAudit(method, user.getIdKey());
        return method;
    }

    private UserPaymentMethod named(UserPaymentMethod method, String name) {
        method.setName(name);
        return method;
    }
}

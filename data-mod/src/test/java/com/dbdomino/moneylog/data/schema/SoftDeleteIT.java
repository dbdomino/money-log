package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.entity.UserPaymentMethod;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserPaymentMethodRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 기준 데이터의 삭제 표시 — FR-031·FR-037.
 *
 * <p>확인하려는 것: 수단과 지출유형은 삭제해도 <b>행이 남는다</b>. 이게 성립해야
 * 과거 지출·소득이 참조를 유지할 수 있고, 목표금액이 삭제된 유형을 계속 가리킬 수
 * 있으며(FR-038), 마스터 참조에 FK RESTRICT를 걸어도 삭제가 막히지 않는다.
 *
 * <p>이 두 저장 단위가 물리 삭제였다면 고정지출·목표금액·통계의 유형 참조에 FK를
 * 걸 수 없어 전부 논리 참조로 두어야 했다(research §4).
 */
class SoftDeleteIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Test
    @DisplayName("수단을 삭제 표시해도 행과 이름이 그대로 남는다")
    void paymentMethodSurvivesSoftDelete() {
        User user = inTx(() -> userRepository.save(newUser()));

        UserPaymentMethod saved = inTx(() -> {
            UserPaymentMethod method = new UserPaymentMethod();
            method.setUser(user);
            method.setName("국민카드");
            method.setType(UserPaymentMethod.TYPE_CARD);
            method.setPurpose(UserPaymentMethod.PURPOSE_EXPENSE);
            method.setCardExpiry("2028-12");
            stampAudit(method, user.getIdKey());
            return paymentMethodRepository.saveAndFlush(method);
        });

        inTx(() -> {
            UserPaymentMethod target = paymentMethodRepository.findById(saved.getIdx()).orElseThrow();
            target.setDeleted(true);
            paymentMethodRepository.saveAndFlush(target);
        });

        UserPaymentMethod found = inTx(() ->
                paymentMethodRepository.findById(saved.getIdx()).orElseThrow());

        assertThat(found.getDeleted()).isTrue();
        assertThat(found.getName()).isEqualTo("국민카드");
        assertThat(found.getCardExpiry()).isEqualTo("2028-12");

        // 사용 중 목록에서는 빠진다
        assertThat(inTx(() ->
                paymentMethodRepository.findByUserIdKeyAndPurposeAndInUseTrueAndDeletedFalse(
                        user.getIdKey(), UserPaymentMethod.PURPOSE_EXPENSE)))
                .isEmpty();
    }

    @Test
    @DisplayName("지출유형을 삭제 표시해도 행과 아이콘 파일명이 그대로 남는다")
    void expendGroupSurvivesSoftDelete() {
        User user = inTx(() -> userRepository.save(newUser()));

        UserExpendGroup saved = inTx(() -> {
            UserExpendGroup group = new UserExpendGroup();
            group.setUser(user);
            group.setName("식비");
            group.setDefaultGroup(true);
            group.setIconFilename(user.getUserId() + "_식비.png");
            stampAudit(group, user.getIdKey());
            return expendGroupRepository.saveAndFlush(group);
        });

        inTx(() -> {
            UserExpendGroup target = expendGroupRepository.findById(saved.getIdx()).orElseThrow();
            target.setDeleted(true);
            expendGroupRepository.saveAndFlush(target);
        });

        UserExpendGroup found = inTx(() ->
                expendGroupRepository.findById(saved.getIdx()).orElseThrow());

        assertThat(found.getDeleted()).isTrue();
        assertThat(found.getName()).isEqualTo("식비");
        assertThat(found.getDefaultGroup()).isTrue();
        // 아이콘 파일은 지우지 않으므로 파일명도 남는다
        assertThat(found.getIconFilename()).isEqualTo(user.getUserId() + "_식비.png");

        // 사용 중 목록에서는 빠진다
        assertThat(inTx(() ->
                expendGroupRepository.findByUserIdKeyAndInUseTrueAndDeletedFalse(user.getIdKey())))
                .isEmpty();
    }
}

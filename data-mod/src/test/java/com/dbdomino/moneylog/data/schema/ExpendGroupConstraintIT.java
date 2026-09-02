package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import com.dbdomino.moneylog.data.repository.UserExpendGroupRepository;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 지출유형 이름 유일성 — quickstart.md §3 시나리오 #7.
 *
 * <p>확인하려는 것: 이름은 <b>같은 회원 안에서만</b> 유일하다(FR-035). 회원이 다르면
 * 같은 이름을 쓸 수 있어야 한다 — 가입 시 모두에게 "식비"가 생기므로 전역 유일이면
 * 두 번째 회원의 가입이 막힌다.
 *
 * <p>삭제 표시된 유형의 이름도 계속 점유된다. 부분 유니크로 삭제분을 빼면 이름을
 * 재사용할 수 있게 되는데, 아이콘 파일명이 {@code {user_id}_{유형이름}}이라 기존
 * 파일을 덮어쓰게 된다(research §5).
 */
class ExpendGroupConstraintIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserExpendGroupRepository expendGroupRepository;

    @Test
    @DisplayName("#7 같은 회원이 같은 이름의 지출유형을 두 번 만들면 두 번째가 거부된다")
    void rejectsDuplicateNameWithinSameUser() {
        User user = inTx(() -> userRepository.save(newUser()));

        inTx(() -> expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")));

        assertViolatesConstraint(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "식비")), "ux_user_expend_group_name");
    }

    @Test
    @DisplayName("#7 회원이 다르면 같은 이름의 지출유형을 각자 가질 수 있다")
    void allowsSameNameAcrossDifferentUsers() {
        User first = inTx(() -> userRepository.save(newUser()));
        User second = inTx(() -> userRepository.save(newUser()));

        inTx(() -> expendGroupRepository.saveAndFlush(newExpendGroup(first, "식비")));
        inTx(() -> expendGroupRepository.saveAndFlush(newExpendGroup(second, "식비")));

        assertThat(inTx(() -> expendGroupRepository.existsByUserIdKeyAndName(first.getIdKey(), "식비")))
                .isTrue();
        assertThat(inTx(() -> expendGroupRepository.existsByUserIdKeyAndName(second.getIdKey(), "식비")))
                .isTrue();
    }

    @Test
    @DisplayName("삭제 표시된 유형의 이름도 점유된 채로 남는다")
    void keepsNameReservedAfterSoftDelete() {
        User user = inTx(() -> userRepository.save(newUser()));

        UserExpendGroup saved = inTx(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "취미")));

        inTx(() -> {
            UserExpendGroup target = expendGroupRepository.findById(saved.getIdx()).orElseThrow();
            target.setDeleted(true);
            expendGroupRepository.saveAndFlush(target);
        });

        // 삭제 표시했더라도 같은 이름을 다시 만들 수 없다
        assertViolatesConstraint(() ->
                expendGroupRepository.saveAndFlush(newExpendGroup(user, "취미")), "ux_user_expend_group_name");
    }
}

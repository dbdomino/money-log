package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 회원 권한 값 제약 — quickstart.md §3 시나리오 #17.
 *
 * <p>권한은 {@code 1}(관리자)과 {@code 3}(일반) 둘뿐이다(FR-013). 그 사이의 값이
 * 들어갈 수 있으면 권한 판정이 조용히 어긋나므로 DB가 막아야 한다.
 */
class UserCheckConstraintIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("#17 허용되지 않은 권한 값(2)은 CHECK 제약에 막힌다")
    void rejectsUnsupportedRole() {
        assertViolatesConstraint(() -> {
            User user = newUser();
            user.setRole((short) 2);
            userRepository.saveAndFlush(user);
        }, "ck_user_role");
    }

    @Test
    @DisplayName("관리자(1)와 일반(3)은 저장된다")
    void acceptsAdminAndMemberRoles() {
        User admin = inTx(() -> {
            User user = newUser();
            user.setRole(User.ROLE_ADMIN);
            return userRepository.saveAndFlush(user);
        });

        User member = inTx(() -> {
            User user = newUser();
            user.setRole(User.ROLE_MEMBER);
            return userRepository.saveAndFlush(user);
        });

        assertThat(admin.getRole()).isEqualTo(User.ROLE_ADMIN);
        assertThat(member.getRole()).isEqualTo(User.ROLE_MEMBER);
    }
}

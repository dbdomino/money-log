package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 회원 유일성 제약 — quickstart.md §3 시나리오 #1·#2·#3.
 *
 * <p>확인하려는 것: 아이디는 전역 유일이고(FR-010), 이메일은 <b>값이 있을 때만</b>
 * 유일하다(FR-012). 이메일이 비어 있는 회원은 여럿이어야 한다 — 선택 항목이기
 * 때문이다. 이 "빈 값은 여러 건 허용"이 조건 없는 UNIQUE로는 표현되지 않아
 * 부분 유니크 인덱스 {@code ux_user_email}이 필요하다.
 */
class UserConstraintIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        cleanUpUsers();
    }

    @Test
    @DisplayName("#1 같은 user_id 로 회원을 두 번 저장하면 두 번째가 거부된다")
    void rejectsDuplicateUserId() {
        String duplicated = TEST_USER_PREFIX + "dup_id";

        inTx(() -> userRepository.save(newUser(duplicated)));

        assertViolatesConstraint(() -> userRepository.saveAndFlush(newUser(duplicated)));
    }

    @Test
    @DisplayName("#2 이메일이 비어 있는 회원은 여러 명 존재할 수 있다")
    void allowsMultipleUsersWithoutEmail() {
        inTx(() -> {
            User first = newUser();
            first.setEmail(null);
            return userRepository.saveAndFlush(first);
        });

        inTx(() -> {
            User second = newUser();
            second.setEmail(null);
            return userRepository.saveAndFlush(second);
        });

        assertThat(countTestUsers()).isEqualTo(2);
    }

    @Test
    @DisplayName("#3 같은 이메일 값으로 회원을 두 번 저장하면 두 번째가 거부된다")
    void rejectsDuplicateEmail() {
        String duplicated = TEST_USER_PREFIX + "dup@example.com";

        inTx(() -> {
            User first = newUser();
            first.setEmail(duplicated);
            return userRepository.saveAndFlush(first);
        });

        assertViolatesConstraint(() -> {
            User second = newUser();
            second.setEmail(duplicated);
            userRepository.saveAndFlush(second);
        });
    }

    private int countTestUsers() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM tbl_user WHERE user_id LIKE ?", Integer.class,
                TEST_USER_PREFIX + "%");
        return count == null ? 0 : count;
    }
}

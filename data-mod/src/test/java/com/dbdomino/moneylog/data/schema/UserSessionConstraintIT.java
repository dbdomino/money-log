package com.dbdomino.moneylog.data.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.data.entity.User;
import com.dbdomino.moneylog.data.entity.UserSession;
import com.dbdomino.moneylog.data.repository.UserRepository;
import com.dbdomino.moneylog.data.repository.UserSessionRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 회원 세션 제약 — quickstart.md §3 시나리오 #4·#5.
 *
 * <p>확인하려는 것: 회원당 폐기되지 않은 세션은 <b>동시에 1건뿐</b>이고(FR-017),
 * 폐기는 행을 지우는 것이 아니라 표시만 바꾸는 것이다(FR-016).
 *
 * <p>이 단일성은 애플리케이션 검사로는 부족하다. 같은 회원이 두 기기에서 동시에
 * 로그인하면 "기존 세션 확인 → 폐기 → 새 세션 저장" 사이에 다른 요청이 끼어들 수
 * 있다. DB의 부분 유니크 인덱스가 마지막 방어선이다.
 */
class UserSessionConstraintIT extends AbstractSchemaIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Test
    @DisplayName("#4 한 회원에게 폐기되지 않은 세션이 둘이면 두 번째가 거부된다")
    void rejectsSecondActiveSession() {
        User user = inTx(() -> userRepository.save(newUser()));

        inTx(() -> sessionRepository.saveAndFlush(newSession(user)));

        assertViolatesConstraint(() -> sessionRepository.saveAndFlush(newSession(user)), "ux_user_session_active");
    }

    @Test
    @DisplayName("#5 기존 세션을 폐기하면 새 세션을 만들 수 있고 활성 세션은 1건으로 유지된다")
    void allowsNewSessionAfterRevokingPrevious() {
        User user = inTx(() -> userRepository.save(newUser()));

        UserSession first = inTx(() -> sessionRepository.saveAndFlush(newSession(user)));

        inTx(() -> {
            UserSession target = sessionRepository.findById(first.getIdx()).orElseThrow();
            target.revoke();
            sessionRepository.saveAndFlush(target);
        });

        UserSession second = inTx(() -> sessionRepository.saveAndFlush(newSession(user)));

        // 폐기된 행은 남아 있다 — 지우지 않는다
        assertThat(countSessions(user)).isEqualTo(2);

        // 활성 세션은 새로 만든 1건뿐이다
        UserSession active = inTx(() ->
                sessionRepository.findByUserIdKeyAndRevokedFalse(user.getIdKey()).orElseThrow());
        assertThat(active.getIdx()).isEqualTo(second.getIdx());

        // 폐기된 세션은 두 해시가 비어 있다
        UserSession revoked = inTx(() -> sessionRepository.findById(first.getIdx()).orElseThrow());
        assertThat(revoked.getRevoked()).isTrue();
        assertThat(revoked.getAccessTokenHash()).isNull();
        assertThat(revoked.getRefreshTokenHash()).isNull();
    }

    private UserSession newSession(User user) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionId(UUID.randomUUID());
        session.setAccessTokenHash("access-hash-" + UUID.randomUUID());
        session.setRefreshTokenHash("refresh-hash-" + UUID.randomUUID());
        session.setAccessExpiresAt(OffsetDateTime.now().plusDays(1));
        session.setRefreshExpiresAt(OffsetDateTime.now().plusDays(7));
        stampAudit(session, user.getIdKey());
        return session;
    }

    private int countSessions(User user) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM tbl_user_session WHERE id_key = ?",
                Integer.class, user.getIdKey());
        return count == null ? 0 : count;
    }
}

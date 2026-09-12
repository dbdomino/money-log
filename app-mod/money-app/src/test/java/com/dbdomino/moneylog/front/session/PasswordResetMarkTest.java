package com.dbdomino.moneylog.front.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.front.client.BackendClientFixture;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 비밀번호 재설정 표식. 008 이 만드는 유일한 상태다.
 *
 * <p>마지막 시험이 <b>반사로</b> 확인한다 — 새 비밀번호를 담는 통로가 아예 없다는 것이다.
 * 통로가 있으면 언젠가 누군가 담고, 담긴 평문은 세션이 살아 있는 동안 서버에 머문다.
 */
class PasswordResetMarkTest {

    private final PasswordResetMark mark = new PasswordResetMark();

    @AfterEach
    void tearDown() {
        BackendClientFixture.unbindRequest();
    }

    @Test
    @DisplayName("담은 값을 그대로 읽는다")
    void 담고_읽는다() {
        BackendClientFixture.bindRequest();

        mark.mark("hong", "홍길동");

        assertThat(mark.exists()).isTrue();
        assertThat(mark.memberId()).isEqualTo("hong");
        assertThat(mark.nickname()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("지우면 표식이 사라진다")
    void 지운다() {
        BackendClientFixture.bindRequest();
        mark.mark("hong", "홍길동");

        mark.clear();

        // 남겨 두면 뒤로 가기로 같은 화면에 돌아와 다시 바꿀 수 있다.
        assertThat(mark.exists()).isFalse();
        assertThat(mark.memberId()).isNull();
        assertThat(mark.nickname()).isNull();
    }

    @Test
    @DisplayName("요청 밖에서 읽어도 터지지 않는다")
    void 요청_밖에서도_터지지_않는다() {
        // 스케줄러나 기동 시점처럼 요청이 없는 자리에서 불려도 화면 모듈이 멈추면 안 된다.
        assertThat(mark.exists()).isFalse();
        assertThat(mark.memberId()).isNull();
    }

    @Test
    @DisplayName("새 비밀번호를 담는 통로가 없다")
    void 비밀번호를_담는_통로가_없다() {
        // 세션에 평문이 머무는 시간을 만들지 않는다. 그 값은 저장 요청에만 실려 나간다.
        for (Method method : PasswordResetMark.class.getDeclaredMethods()) {
            assertThat(method.getName().toLowerCase())
                    .as("표식에 비밀번호를 담는 메서드가 생기면 여기서 걸린다")
                    .doesNotContain("password");
        }
    }
}

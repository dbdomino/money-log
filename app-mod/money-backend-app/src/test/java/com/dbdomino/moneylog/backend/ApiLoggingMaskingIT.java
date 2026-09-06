package com.dbdomino.moneylog.backend;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dbdomino.moneylog.data.entity.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * AOP 로그가 비밀번호·토큰을 가리는가.
 *
 * <p>quickstart.md §3 의 시나리오 #34 에 대응한다(FR-128 · SC-111).
 *
 * <p>로그 파일을 눈으로 확인하는 대신 {@code ListAppender} 로 실제 로그 이벤트를 받아
 * 검사한다. 마스킹은 한 번 확인하고 끝낼 성질이 아니다 — 새 API 가 새 필드를 받을 때마다
 * 깨질 수 있어 회귀로 걸어 둬야 한다.
 */
class ApiLoggingMaskingIT extends AbstractApiIT {

    private ListAppender<ILoggingEvent> appender;
    private Logger aspectLogger;

    @BeforeEach
    void attachAppender() {
        aspectLogger = (Logger) LoggerFactory.getLogger(
                "com.dbdomino.moneylog.common.logging.ApiLoggingAspect");
        aspectLogger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        aspectLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("#34 로그인·갱신 로그에 password·Authorization·토큰 원문이 남지 않는다")
    void secretsNeverReachTheLog() throws Exception {
        User user = createMember();

        JsonNode login = postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD));
        String accessToken = login.get("data").get("accessToken").asString();
        String refreshToken = login.get("data").get("refreshToken").asString();

        // 토큰을 헤더로 싣는 요청과 Body 로 싣는 요청을 모두 지나가게 한다.
        getJson("/api/v1/auth/validate", accessToken);
        postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(refreshToken));

        String logged = String.join("\n", messages());
        assertThat(logged).as("AOP 로그가 남아야 검사가 의미를 갖는다").contains("/api/v1/auth/login");

        assertThat(logged).doesNotContain(TEST_PASSWORD);
        assertThat(logged).doesNotContain(accessToken);
        assertThat(logged).doesNotContain(refreshToken);
        // 가린 흔적이 보여야 한다 — 필드를 통째로 빼먹은 것과 구분된다.
        assertThat(logged).contains("password=***");
        assertThat(logged).contains("Authorization=***");
        assertThat(logged).contains("refreshToken=***");
    }

    @Test
    @DisplayName("#34 응답 로그는 resCode 만 남긴다 — 본문을 통째로 찍지 않는다")
    void responseLogCarriesOnlyResCode() throws Exception {
        User user = createMember();

        postJson("/api/v1/auth/login", """
                {"memberId":"%s","password":"%s"}
                """.formatted(user.getUserId(), TEST_PASSWORD));

        List<String> exitLogs = messages().stream().filter(line -> line.startsWith("<--")).toList();
        assertThat(exitLogs).isNotEmpty();
        assertThat(exitLogs).allSatisfy(line -> {
            assertThat(line).contains("resCode=200");
            // 본문을 찍으면 여기에 JWT 가 그대로 들어온다.
            assertThat(line).doesNotContain("eyJ");
        });
    }

    @Test
    @DisplayName("#34 비밀번호 재설정의 newPasswordConfirm 도 가려진다 — 이름 완전 일치 방식의 구멍이었다")
    void resetPasswordConfirmIsMaskedToo() throws Exception {
        User user = createMember();
        String newPassword = "Reset1234!";

        postJson("/api/v1/auth/reset-password", """
                {"memberId":"%s","nickname":"%s","newPassword":"%s","newPasswordConfirm":"%s"}
                """.formatted(user.getUserId(), user.getNickname(), newPassword, newPassword));

        String logged = String.join("\n", messages());
        assertThat(logged).contains("/api/v1/auth/reset-password");
        // 목록에 이름을 하나씩 적는 방식이면 이런 변형이 계속 새어 나간다.
        assertThat(logged).doesNotContain(newPassword);
        assertThat(logged).contains("newPassword=***");
        assertThat(logged).contains("newPasswordConfirm=***");
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}

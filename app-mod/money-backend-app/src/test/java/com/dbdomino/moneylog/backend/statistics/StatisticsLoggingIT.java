package com.dbdomino.moneylog.backend.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 006 의 컨트롤러 둘에 AOP 요청~응답 로깅이 걸리는가 (헌장 원칙 IV · api-contract §10).
 *
 * <p>포인트컷이 {@code within(@RestController *)} 라 <b>자동으로</b> 걸린다 — 이 시험은
 * 그 자동이 실제로 성립하는지를 확인한다. 컨트롤러에 진입/종료 로그를 손으로 쓰지 않았기
 * 때문에, 포인트컷이 006 을 비켜 가면 <b>006 의 6건만 로그가 통째로 없어지고</b> 아무도
 * 알아채지 못한다.
 *
 * <p><b>006 에는 로깅 제외 대상이 없다.</b> 003 의 아이콘·004 의 엑셀처럼 바이너리를
 * 돌려주는 API 가 없고 파일 업로드도, 마스킹할 민감정보도 없다.
 */
class StatisticsLoggingIT extends AbstractStatisticsIT {

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

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("목표금액 API 가 AOP 로그를 남긴다")
    void logsExpendTargetApis() throws Exception {
        Fixture fixture = prepare();

        putDefaultTarget(fixture.token(), fixture.foodGroupId(), 400_000L);
        getJson("/api/v1/expend-targets?year=" + FIXED_YEAR + "&month=" + FIXED_MONTH
                + "&offset=0&limit=10", fixture.token());

        assertThat(messages()).anyMatch(m -> m.contains("/api/v1/expend-targets"));
    }

    @Test
    @DisplayName("통계 조회·저장이 AOP 로그를 남긴다")
    void logsStatisticsApis() throws Exception {
        Fixture fixture = prepare();

        statistics(fixture, FIXED_YEAR, FIXED_MONTH);
        save(fixture, lastMonth().getYear(), lastMonth().getMonthValue());

        assertThat(messages()).anyMatch(m -> m.contains("/api/v1/statistics/monthly"));
        assertThat(messages()).anyMatch(m -> m.contains("/api/v1/statistics/monthly/save"));
    }

    /** 006 에는 바이너리 응답이 없으므로 로그에 {@code binary(...)} 자리가 나올 일이 없다. */
    @Test
    @DisplayName("006 의 로그에 바이너리 표기가 나오지 않는다")
    void neverLogsBinaryPlaceholder() throws Exception {
        Fixture fixture = prepare();

        statistics(fixture, FIXED_YEAR, FIXED_MONTH, "live");

        assertThat(messages()).noneMatch(m -> m.contains("binary("));
    }
}

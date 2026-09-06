package com.dbdomino.moneylog.data.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 감사 활성화. {@link com.dbdomino.moneylog.data.entity.BaseAuditEntity}의
 * 네 컬럼을 채운다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * {@code created_at}/{@code updated_at}에 넣을 시각을 공급한다.
     *
     * <p>기본 제공자({@code CurrentDateTimeProvider})는 {@code LocalDateTime}을
     * 내놓는데, 감사 필드 타입인 {@code OffsetDateTime}으로는 변환되지 않아
     * {@code Cannot convert unsupported date type} 오류가 난다. 컬럼이
     * {@code TIMESTAMPTZ}라 시간대를 가진 타입을 그대로 유지하는 편이 맞으므로,
     * 필드 타입을 낮추는 대신 제공자를 바꾼다.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    // created_by/updated_by 를 공급하는 AuditorAware 는 이 모듈에 두지 않는다.
    //
    // 값의 출처가 "현재 요청의 인증 주체"라 SecurityContext 를 봐야 하는데, data-mod 는
    // 웹 계층을 모르는 라이브러리 모듈이다. 운영 앱은 money-backend-app 의
    // BackendAuditorAware 가, data-mod 의 테스트는 DataModTestApplication 의 테스트 전용
    // 빈이 각각 공급한다. 여기에 하나 더 두면 빈이 둘이 되어 기동이 실패한다.
}

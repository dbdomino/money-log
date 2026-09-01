package com.dbdomino.moneylog.data.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
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

    /**
     * {@code created_by}/{@code updated_by}에 넣을 회원 {@code id_key}를 공급한다.
     *
     * <p><b>임시 구현이다.</b> 인증 필터가 아직 없어 {@code SecurityContext}에서
     * 꺼낼 값이 없으므로 빈 {@code Optional}을 돌려준다. 두 컬럼은 NOT NULL이라
     * (회원 테이블 제외) 이 상태에서는 호출자가 값을 직접 넣어야 저장된다.
     *
     * <p>TODO 백엔드 Phase 1(회원·인증) 구현 시: 인증 필터가
     * {@code SecurityContext}에 실어 둔 {@code id_key}를 꺼내 돌려주도록 교체한다.
     * 로그인 없이 도는 경로(회원가입 등)는 여전히 빈 값이 될 수 있다.
     * {@link com.dbdomino.moneylog.data.entity.BaseAuditEntity}의 {@code @Setter}와
     * {@code AbstractSchemaIT.stampAudit()}이 같은 임시 조치이므로 함께 걷어낸다.
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return Optional::empty;
    }
}

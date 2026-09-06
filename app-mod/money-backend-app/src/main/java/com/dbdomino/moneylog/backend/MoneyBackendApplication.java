package com.dbdomino.moneylog.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 백엔드 API 애플리케이션.
 *
 * <p>{@code scanBasePackages}가 {@code ...backend}로 한정되어 있어 {@code data-mod}의
 * {@code com.dbdomino.moneylog.data} 패키지는 컴포넌트 스캔에 걸리지 않는다. Entity와
 * Repository는 그래서 {@code @EntityScan}·{@code @EnableJpaRepositories}로 따로
 * 지정한다 — 이게 없으면 Entity를 만들어도 테이블이 생기지 않는다.
 *
 * <p>{@code data-mod}의 설정 클래스({@code JpaAuditingConfig} 등)는 아래
 * {@code scanBasePackages}에 {@code ...data.config}를 함께 넣어 잡는다.
 *
 * <p>{@code common-mod}의 {@code com.dbdomino.moneylog.common}도 같은 이유로 넣는다.
 * 이게 없으면 {@code GlobalExceptionHandler}({@code @RestControllerAdvice})와
 * {@code ApiLoggingAspect}({@code @Aspect})가 빈으로 등록되지 않아, 클래스를 만들어도
 * 예외는 Spring 기본 500 JSON 으로 나가고 AOP 로그는 한 줄도 찍히지 않는다 —
 * 헌장 원칙 III·IV 가 런타임에 성립하지 않는 상태가 된다. 두 빈은
 * {@code moneylog.common.web.enabled=true}(이 앱의 {@code application.yml})일 때만 켜진다.
 */
@SpringBootApplication(scanBasePackages = {
        "com.dbdomino.moneylog.backend",
        "com.dbdomino.moneylog.common",
        "com.dbdomino.moneylog.data.config"
})
@EntityScan("com.dbdomino.moneylog.data.entity")
@EnableJpaRepositories("com.dbdomino.moneylog.data.repository")
public class MoneyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyBackendApplication.class, args);
	}
}

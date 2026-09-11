package com.dbdomino.moneylog.front;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 화면 모듈(money-app)의 기동 클래스.
 *
 * <p>스캔 범위를 {@code ...front} 로 좁힌 것이 요점이다. {@code com.dbdomino.moneylog} 전체를
 * 스캔하면 common-mod 의 {@code GlobalExceptionHandler}({@code @RestControllerAdvice})와
 * {@code ApiLoggingAspect} 가 후보로 들어온다. {@code @RestControllerAdvice} 가 프론트에 붙으면
 * 화면 오류가 JSON 으로 나간다 — 사용자는 빈 화면 대신 중괄호를 본다.
 *
 * <p>common-mod 는 의존에 남아 있지만 에러코드 Enum 을 읽기 위한 것이고 빈으로 올리지 않는다.
 */
@SpringBootApplication(scanBasePackages = "com.dbdomino.moneylog.front")
public class MoneyAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyAppApplication.class, args);
    }
}

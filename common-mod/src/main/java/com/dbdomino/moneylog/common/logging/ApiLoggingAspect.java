package com.dbdomino.moneylog.common.logging;

import com.dbdomino.moneylog.common.api.RestResponseDto;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringJoiner;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 요청~응답 AOP 로깅(헌장 원칙 IV).
 *
 * <p>Controller 마다 진입·종료 로그를 손으로 쓰지 않는다. 손으로 쓰면 새 API 가 늘 때마다
 * 빠뜨릴 자리가 생기고, 마스킹도 한 곳에서 보장할 수 없다.
 *
 * <p>남기는 것: HTTP 메서드·URI·요청 인자(마스킹)·응답 {@code resCode}·처리 시간.
 * 예외로 끝난 요청도 소요 시간과 예외 종류를 남긴다 — 실패 응답의 변환은
 * {@code GlobalExceptionHandler}가 하므로 여기서 응답을 만들지 않는다.
 *
 * <p>서블릿 필터에서 Body 를 읽는 방식을 쓰지 않는 이유는
 * {@code HttpServletRequest}의 Body 가 한 번만 읽히기 때문이다. 그 방식은
 * {@code ContentCachingRequestWrapper}로 전 요청 Body 를 메모리에 복사해야 하는데,
 * AOP 는 이미 역직렬화된 DTO 를 받으므로 그 비용이 없다.
 *
 * <p>{@code moneylog.common.web.enabled=true}일 때만 등록된다 —
 * {@code money-app}(프론트)의 스캔 범위가 {@code com.dbdomino.moneylog} 전체라
 * 조건이 없으면 프론트의 모든 Controller 에도 이 Aspect 가 붙는다.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "moneylog.common.web.enabled", havingValue = "true")
public class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);

    /** 마스킹해서 남길 요청 헤더. 값 자체는 {@code SensitiveMasker}가 가린다. */
    private static final String AUTHORIZATION = "Authorization";

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restController() {
    }

    @Around("restController()")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        HttpServletRequest request = currentRequest();
        String endpoint = request == null
                ? signature
                : request.getMethod() + " " + request.getRequestURI();

        log.info("--> {} {} args={} headers={}", endpoint, signature,
                describeArgs(joinPoint.getArgs()), describeHeaders(request));

        long startedAt = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.info("<-- {} resCode={} {}ms", endpoint, resCodeOf(result), elapsedMs(startedAt));
            return result;
        } catch (Throwable e) {
            // 응답 변환은 GlobalExceptionHandler 의 몫이다. 여기서는 시간만 닫고 다시 던진다.
            log.info("<-- {} threw={} {}ms", endpoint, e.getClass().getSimpleName(), elapsedMs(startedAt));
            throw e;
        }
    }

    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        Arrays.stream(args)
                .filter(ApiLoggingAspect::isLoggable)
                .forEach(arg -> joiner.add(SensitiveMasker.describe(arg)));
        return joiner.toString();
    }

    /** 서블릿·바인딩 객체는 찍어도 읽을 것이 없고 크기만 크다. */
    private static boolean isLoggable(Object arg) {
        return !(arg instanceof jakarta.servlet.ServletRequest)
                && !(arg instanceof jakarta.servlet.ServletResponse)
                && !(arg instanceof org.springframework.validation.BindingResult);
    }

    private static String describeHeaders(HttpServletRequest request) {
        if (request == null) {
            return "{}";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (AUTHORIZATION.equalsIgnoreCase(name)) {
                joiner.add(name + "=" + SensitiveMasker.MASK);
            }
        }
        return joiner.toString();
    }

    /** 응답에서 {@code resCode}만 꺼낸다. 본문 전체를 찍으면 토큰이 그대로 남는다. */
    private static Object resCodeOf(Object result) {
        Object body = result instanceof ResponseEntity<?> entity ? entity.getBody() : result;
        if (body instanceof RestResponseDto<?> response) {
            return response.getResCode();
        }
        return body == null ? "-" : "?";
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}

package com.dbdomino.moneylog.common.aop;

import com.dbdomino.moneylog.common.util.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Objects;

/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 * 컨트롤러 수행시 로그를 남기는 기능
 */

@Aspect
@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class LoggingAspect {
    private final LoggerUtil loggerUtil;
    /*
@Aspect	AOP로 정의하는 클래스를 지정함
@Pointcut	AOP기능을 메소드, Annotation 등 어디에 적용시킬지 지점을 설정, 지점을 설정하기 위한 수식들이 매우 많음
@Before	메소드 실행하기 이전
@After	메소드가 성공적으로 실행 후 (예외 발생 되더라도 실행 됨)
@AfterReturning	메소드가 정상적으로 종료될때
@AfterThrowing	메소드에서 예외가 발생할때
@Around	Before + After 모두 제어 (에외 발생 되더라도 실행 됨)
     */

    @Pointcut("within(com.dbdomino.moneylog.controller..*)")
    public void onCut() {}

    @Pointcut("within(com.dbdomino.moneylog.common.exception.ControllerExceptionHandler)")
    public void onExceptionCut() { }

    @Before("com.dbdomino.moneylog.common.aop.LoggingAspect.onCut()") // 지정한 패턴에 해당하는 메소드가 실행되기 전에, interceptor와 같이 동작하는 것
    public void requestLogging(JoinPoint joinPoint) {
        // HttpServletRequest  가져옴
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull((RequestContextHolder.getRequestAttributes())))
                .getRequest();

        String method = request.getMethod(); // HTTP 메서드(GET, POST, PUT 등)를 반환.
        String uri = request.getRequestURI(); // 요청 URI를 반환 (예: /api/resource).
        String params = request.getQueryString();  // 요청 URL의 쿼리 문자열을 반환 (예: id=123&name=John).

        log.info("===== REQUEST LOG =====\n" +
                " {} {} {}\n" +
                "HEADERS :{}\n" +
                " BODY : {}\n" +
                "===== REQUEST END =====",method, uri, params, getHeadersAsString(request), getBodyAsString(request));

    }

    @AfterReturning(value = "com.dbdomino.moneylog.common.aop.LoggingAspect.onCut()", returning = "returnObj")  // onCut() 매서드 종료시 수행
    public void afterReturnLogging(JoinPoint joinPoint, Object returnObj) {
        // HttpServletRequest  가져옴
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull((RequestContextHolder.getRequestAttributes())))
                .getRequest();

        String method = request.getMethod(); // HTTP 메서드(GET, POST, PUT 등)를 반환.
        String uri = request.getRequestURI(); // 요청 URI를 반환 (예: /api/resource).
        String params = request.getQueryString();  // 요청 URL의 쿼리 문자열을 반환 (예: id=123&name=John).

        log.info("===== RESPONSE LOG =====\n" +
                " {} {} {}\n" +
                " {} \n" +
                "===== RESPONSE END =====",method, uri, params, loggerUtil.getResponseBody(returnObj));
    }

    @AfterReturning(value = "com.dbdomino.moneylog.common.aop.LoggingAspect.onExceptionCut()", returning = "returnExceptionObj") // onExceptionCut() 메서드 종료시 수행
    public void afterExceptionReturnLogging(JoinPoint joinPoint, Object returnExceptionObj) {
        // HttpServletRequest  가져옴
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull((RequestContextHolder.getRequestAttributes())))
                .getRequest();

        String method = request.getMethod(); // HTTP 메서드(GET, POST, PUT 등)를 반환.
        String uri = request.getRequestURI(); // 요청 URI를 반환 (예: /api/resource).
        String params = request.getQueryString();  // 요청 URL의 쿼리 문자열을 반환 (예: id=123&name=John).

        log.info("===== RESPONSE LOG =====\n" +
                " {} {} {}\n" +
                " {} \n" +
                "===== RESPONSE END =====",method, uri, params, loggerUtil.getResponseBody(returnExceptionObj));
    }

    public String getHeadersAsString(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        StringBuilder headers = new StringBuilder();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.append(headerName).append(": ").append(headerValue).append("\n");
        }

        return headers.toString().trim();
    }

    public String getBodyAsString(HttpServletRequest request) {
        StringBuilder body = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace(); // 로깅 필요
        }

        return body.toString().trim();
    }



}

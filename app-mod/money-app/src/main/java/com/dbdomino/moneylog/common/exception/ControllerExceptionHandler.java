package com.dbdomino.moneylog.common.exception;

import com.dbdomino.moneylog.common.util.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/** @since  2024-12-28
 * @author dbdomino
 * @version 1.0
 *
 * 컬트롤러에서 예외 발처리를 로깅하는 클래스
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ControllerExceptionHandler {
    private final LoggerUtil loggerUtil;

    // 공통 예외처리 메서드
    private void exceptionRequestLogging(Exception exception, HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String params = request.getQueryString();
        String msg = exception.getMessage();

        log.error("====== EXCEPTION LOG ======\n" +
                "                METHOD : {}\n" +
                "                URI    : {}\n" +
                "                PARAMS : {}\n" +
                "                HEADERS: {}\n" +
                "                BODY   : {}\n" +
                "                ERROR  : {}\n" +
                "====== END ======" +
                method, uri, params, loggerUtil.logRequestHeaders(request), loggerUtil.getRequestBody(request), msg);
    }
}

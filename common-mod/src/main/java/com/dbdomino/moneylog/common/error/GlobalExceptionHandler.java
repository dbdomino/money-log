package com.dbdomino.moneylog.common.error;

import com.dbdomino.moneylog.common.api.RestResponseDto;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리. Controller 가 try-catch 로 응답을 제각각 만들지 않게 한다(헌장 원칙 III).
 *
 * <p>이 클래스가 정하는 것은 <b>HTTP 상태와 resCode 의 대응</b> 하나다.
 *
 * <table border="1">
 *   <caption>변환 규칙</caption>
 *   <tr><th>예외</th><th>HTTP</th><th>resCode</th></tr>
 *   <tr><td>{@link BusinessException}</td><td>200</td><td>그 코드</td></tr>
 *   <tr><td>검증·형식 오류</td><td>200</td><td>9001</td></tr>
 *   <tr><td>업로드 크기 초과</td><td>200</td><td>3102</td></tr>
 *   <tr><td>그 밖의 모든 예외</td><td>500</td><td>9000</td></tr>
 * </table>
 *
 * <p><b>비즈니스 실패가 HTTP 200인 것이 이 계약의 특이점이다</b>(002 SC-101). 클라이언트가
 * 상태 코드로 분기하지 않고 {@code resCode} 하나만 보게 하려는 것이다.
 *
 * <p>{@code moneylog.common.web.enabled=true}일 때만 등록된다. {@code money-app}(프론트)의
 * 컴포넌트 스캔 범위가 {@code com.dbdomino.moneylog} 전체라, 조건이 없으면 프론트에도
 * 등록되어 그쪽의 {@code ControllerExceptionHandler}와 advice 두 개가 공존한다.
 */
@RestControllerAdvice
@ConditionalOnProperty(name = "moneylog.common.web.enabled", havingValue = "true")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 비즈니스·검증 실패. HTTP 200 + 그 코드. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestResponseDto<Map<String, String>>> handleBusiness(BusinessException e) {
        log.warn("business failure resCode={} message={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(RestResponseDto.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 요청 값 문제. HTTP 200 + {@code 9001}.
     *
     * <p>Bean Validation 실패, 읽을 수 없는 Body, 누락된 Query, 타입 불일치, 허용하지 않는
     * 메서드를 한 자리에서 다룬다 — 클라이언트가 할 조치가 "요청을 고친다"로 같다.
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<RestResponseDto<Map<String, String>>> handleBadRequest(Exception e) {
        log.warn("bad request type={} message={}", e.getClass().getSimpleName(), e.getMessage());
        return ResponseEntity.ok(RestResponseDto.fail(ErrorCode.BAD_REQUEST));
    }

    /**
     * 업로드 크기 초과. HTTP 200 + {@code 3102}.
     *
     * <p>{@code spring.servlet.multipart.max-file-size} 가 1MB 를 넘는 요청을
     * <b>Controller 에 닿기 전에</b> 잘라 낸다. 그래서 애플리케이션의 크기 검사
     * ({@code ExpendGroupIconService})는 그 요청을 아예 보지 못한다.
     *
     * <p>이 매핑이 없으면 아래 {@code Exception} 갈래를 타 {@code 9000} + HTTP 500 이 나가
     * SC-211("1MB 초과 100% {@code 3102}")이 깨진다. 같은 파일이 요청 크기에 따라 다른
     * 코드로 거절되는 셈이라, 클라이언트는 "왜 어떤 큰 파일은 3102 고 어떤 것은 서버 오류인지"
     * 를 설명할 수 없다.
     *
     * <p>이 프로젝트에서 파일을 받는 API 는 2.7·2.11 뿐이므로 크기 초과는 곧 아이콘 문제다.
     * 다른 업로드가 생기면 여기서 어느 API 인지 갈라야 한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestResponseDto<Map<String, String>>> handleUploadTooLarge(
            MaxUploadSizeExceededException e) {
        log.warn("upload too large message={}", e.getMessage());
        return ResponseEntity.ok(RestResponseDto.fail(ErrorCode.EXPEND_GROUP_ICON_INVALID));
    }

    /**
     * 그 밖의 모든 예외. <b>여기만 HTTP 500</b>이다.
     *
     * <p>스택트레이스를 남기는 것도 여기뿐이다. 예상한 실패(위 두 핸들러)는 코드와 경로만
     * 남기면 되고, 예상하지 못한 실패만 원인을 추적할 재료가 필요하다.
     *
     * <p>예외 메시지를 응답에 싣지 않는다. SQL 조각·파일 경로·내부 클래스 이름이 그대로
     * 나가면 그 자체가 정보 노출이다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponseDto<Map<String, String>>> handleUnexpected(Exception e) {
        log.error("unexpected failure", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResponseDto.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}

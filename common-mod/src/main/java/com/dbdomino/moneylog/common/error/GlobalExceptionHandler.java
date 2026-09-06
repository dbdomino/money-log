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
 *   <tr><td>서블릿 한도 초과 업로드</td><td>200</td><td>9001</td></tr>
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

    /**
     * 비즈니스·검증 실패. HTTP 200 + 그 코드.
     *
     * <p>{@link DetailedBusinessException} 이면 {@code data} 자리에 그 예외가 들고 온
     * 객체를 <b>통째로</b> 넣는다. {@code { message }} 한 칸으로는 담을 수 없는 실패가
     * 하나 있어서다 — 004 의 엑셀 업로드({@code 3502})는 행별 오류 <b>목록</b>을 함께
     * 줘야 프론트가 위치를 짚어 줄 수 있다.
     *
     * <p>그 상세의 실제 타입은 앱 모듈이 소유하고 여기서는 알지 못한다(원칙 I) —
     * 직렬화는 Jackson 이 실제 타입을 보고 한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestResponseDto<?>> handleBusiness(BusinessException e) {
        log.warn("business failure resCode={} message={}", e.getCode(), e.getMessage());
        if (e instanceof DetailedBusinessException detailed) {
            return ResponseEntity.ok(
                    RestResponseDto.failWith(detailed.getErrorCode(), detailed.getDetails()));
        }
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
     * 서블릿 한도를 넘은 업로드. HTTP 200 + {@code 9001}.
     *
     * <p>{@code spring.servlet.multipart.max-file-size} 를 넘는 요청은 <b>Controller 에
     * 닿기 전에</b> 잘려 나가므로 애플리케이션의 크기 검사가 그 요청을 아예 보지 못한다.
     * 이 매핑이 없으면 아래 {@code Exception} 갈래를 타 {@code 9000} + HTTP 500 이 나가는데,
     * 사용자 입력 문제인데 서버 장애처럼 보인다.
     *
     * <h2>{@code 3102}(아이콘)에서 {@code 9001}(잘못된 요청)로 바꾼 이유</h2>
     *
     * <p>003 이 이 매핑을 넣을 때는 <b>파일을 받는 API 가 아이콘(2.7·2.11) 하나뿐</b>이라
     * "크기 초과 = 아이콘 문제"가 성립했다. 004 의 엑셀 업로드(3.12)가 생기면서 그 전제가
     * 깨졌다 — 그대로 두면 <b>엑셀이 너무 커도 "아이콘 형식·크기 오류"가 나간다</b>.
     *
     * <p>API 별로 가르지 않는다. 경로를 보고 갈라야 하는데 그 경로 문자열은 앱 모듈이
     * 소유하고(원칙 I 의 단방향 의존) 여기서 다시 적으면 두 곳에 복제된다. 게다가
     * <b>004 에는 "바이트 크기 초과"에 배정된 코드가 없다</b> — {@code 3503}(xlsx 아님)·
     * {@code 3504}(300행 초과)·{@code 3505}(빈 파일)는 전부 내용 기준이다. 두 API 에
     * 공통으로 맞는 답은 "요청이 잘못됐다"뿐이다.
     *
     * <p><b>003 의 SC-211("1MB 초과 100% {@code 3102}")은 그대로 지켜진다.</b> 서블릿
     * 한도가 10MB 로 올라가 1MB 초과 아이콘은 이제 Controller 까지 들어오고,
     * {@code ExpendGroupIconService} 의 애플리케이션 검사가 {@code 3102} 로 거절한다.
     * 이 갈래에 닿는 아이콘은 10MB 를 넘는 것뿐이다.
     *
     * <p>어느 API 인지에 따라 다른 코드가 필요해지면, 그 판정은 여기가 아니라 <b>앱 모듈의
     * advice</b> 에 둔다 — 경로를 아는 쪽이 거기다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestResponseDto<Map<String, String>>> handleUploadTooLarge(
            MaxUploadSizeExceededException e) {
        log.warn("upload too large message={}", e.getMessage());
        return ResponseEntity.ok(RestResponseDto.fail(ErrorCode.BAD_REQUEST,
                "업로드할 수 있는 크기를 넘었습니다."));
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

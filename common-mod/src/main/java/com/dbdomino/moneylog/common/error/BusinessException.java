package com.dbdomino.moneylog.common.error;

/**
 * 비즈니스·검증 실패. {@link ErrorCode}를 들고 다닌다.
 *
 * <p>헌장 원칙 III — 실패는 커스텀 예외로 던지고 {@code RuntimeException}에 문자열만
 * 담아 던지지 않는다. 코드마다 예외 클래스를 따로 만들지도 않는다. 40여 개가 전부
 * "코드를 하나 들고 전역 핸들러까지 올라간다"는 같은 일을 하기 때문이다.
 *
 * <p>이 예외는 {@link GlobalExceptionHandler}에서 <b>HTTP 200</b> + 4자리
 * {@code resCode}로 변환된다. 예외를 던졌는데 200이 나가는 것이 낯설지만 명세가 정한
 * 계약이다(002 SC-101).
 *
 * <p>스택트레이스를 채우지 않는다. 이 예외는 흐름 제어용이고 초당 여러 번 발생할 수
 * 있는데(로그인 실패 등), 스택 수집은 그 자체로 비싸고 남길 정보도 아니다.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    /** 기본 문구 대신 상황에 맞는 문구를 실을 때 쓴다. 코드는 그대로다. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** 응답 {@code resCode}에 실리는 값. */
    public int getCode() {
        return errorCode.code();
    }
}

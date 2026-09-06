package com.dbdomino.moneylog.common.api;

import com.dbdomino.moneylog.common.error.ErrorCode;
import java.util.Map;

/**
 * 표준 API 응답. 성공 {@code resCode}는 {@code 200}, 실패는 4자리 에러코드.
 *
 * <pre>{@code { "resCode": 200, "data": { } }}</pre>
 *
 * <p>실패 코드는 {@link ErrorCode}로만 만든다. 정수 리터럴을 호출부에 흩으면 명세와
 * 어긋난 코드가 조용히 나가고, 어느 API가 어떤 코드를 쓰는지 세는 방법도 사라진다.
 */
public final class RestResponseDto<T> {

    private final int resCode;
    private final T data;

    private RestResponseDto(int resCode, T data) {
        this.resCode = resCode;
        this.data = data;
    }

    public static <T> RestResponseDto<T> ok(T data) {
        return new RestResponseDto<>(200, data);
    }

    /** 실패 응답. {@code data}는 {@code { "message": ... }} 한 칸이다. */
    public static RestResponseDto<Map<String, String>> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.message());
    }

    /** 기본 문구 대신 상황에 맞는 문구를 실을 때 쓴다. */
    public static RestResponseDto<Map<String, String>> fail(ErrorCode errorCode, String message) {
        return new RestResponseDto<>(errorCode.code(), Map.of("message", message));
    }

    public int getResCode() {
        return resCode;
    }

    public T getData() {
        return data;
    }
}

package com.dbdomino.moneylog.common.api;

import java.util.Map;

/**
 * 표준 API 응답. 성공 {@code resCode}는 {@code 200}, 실패는 4자리 에러코드.
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

    public static RestResponseDto<Map<String, String>> fail(int resCode, String message) {
        return new RestResponseDto<>(resCode, Map.of("message", message));
    }

    public int getResCode() {
        return resCode;
    }

    public T getData() {
        return data;
    }
}

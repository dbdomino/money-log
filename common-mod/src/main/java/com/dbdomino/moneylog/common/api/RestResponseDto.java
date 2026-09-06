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

    /**
     * 실패 응답의 {@code data} 에 <b>메시지 말고 더 실을 것이 있을 때</b> 쓴다.
     *
     * <p>004 의 엑셀 업로드({@code 3502})가 행별 오류 목록을 함께 줘야 프론트가 "표의 N행
     * M열을 고치세요"를 안내할 수 있다. {@code Map<String,String>} 으로는 배열을 담을 수 없다.
     *
     * <p>{@code data} 를 <b>통째로</b> 대신하므로 넘기는 객체가 {@code message} 도 함께
     * 들고 있어야 한다 — 담지 않은 것은 응답에 나타나지 않는다.
     */
    public static <T> RestResponseDto<T> failWith(ErrorCode errorCode, T data) {
        return new RestResponseDto<>(errorCode.code(), data);
    }

    public int getResCode() {
        return resCode;
    }

    public T getData() {
        return data;
    }
}

package com.dbdomino.moneylog.front.client;

/**
 * 백엔드에 닿지 못했다. 연결 거부·타임아웃, 그리고 <b>봉투가 아닌 응답</b>이 여기로 온다.
 *
 * <p><b>코드를 들고 있지 않은 것이 요점이다.</b> {@code 9000}(서버 오류)을 빌려 쓰면
 * "백엔드가 {@code 9000} 을 줬다"와 "백엔드에 닿지 못했다"가 구분되지 않는다. 두 상황은
 * 사용자에게 할 말이 다르고(전자는 잠시 후 재시도, 후자는 서버가 떠 있는지 확인) 운영자가
 * 볼 곳도 다르다. 화면 모듈은 새 코드를 정의하지 않으므로(FR-607) 코드 없이 올린다.
 */
public class BackendUnavailableException extends RuntimeException {

    public BackendUnavailableException(String message) {
        super(message);
    }

    public BackendUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

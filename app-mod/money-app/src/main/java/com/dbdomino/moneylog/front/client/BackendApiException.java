package com.dbdomino.moneylog.front.client;

/**
 * 백엔드가 실패 봉투로 답했다. {@code resCode} 와 안내 문구를 <b>그대로</b> 들고 올라간다.
 *
 * <p>화면 모듈은 코드를 새로 정의하지도, 바꾸지도 않는다(FR-607). 백엔드가 {@code 3003}
 * 을 줬으면 화면까지 {@code 3003} 으로 도착해야 한다 — 중간에서 뭉개면 어느 화면이 어떤
 * 실패를 만났는지 로그로 되짚을 수 없다.
 *
 * <p>실패를 반환값이 아니라 예외로 올리는 이유는 <b>확인을 빠뜨린 코드도 컴파일되기
 * 때문</b>이다. 값으로 돌려주면 어느 화면 하나가 검사를 잊었을 때 실패가 성공으로 읽히고
 * 빈 화면이 정상처럼 뜬다.
 */
public class BackendApiException extends RuntimeException {

    private final int resCode;

    public BackendApiException(int resCode, String message) {
        super(message);
        this.resCode = resCode;
    }

    public int getResCode() {
        return resCode;
    }
}

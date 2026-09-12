package com.dbdomino.moneylog.front.session;

/**
 * 로그인 상태가 끝났다. 세션은 이미 버려졌고 사용자는 로그인 화면으로 가야 한다.
 *
 * <p>{@code BackendApiException} 과 나누는 이유는 <b>착지가 다르기 때문</b>이다. 업무 실패는
 * 오류 화면에 문구를 띄우고 끝나지만, 이쪽은 화면을 바꿔야 한다. 같은 예외로 묶으면 코드마다
 * 착지를 고르는 분기가 화면 31개에 흩어진다.
 *
 * <p>코드를 들고 있는 것은 <b>안내 문구가 갈리기 때문</b>이다. 다른 곳에서 로그인해 밀려난
 * 경우와 재발급 토큰이 만료된 경우와 계정이 정지된 경우는 처리가 같아도 사용자가 다음에
 * 할 일이 다르다.
 */
public class SessionExpiredException extends RuntimeException {

    private final int resCode;

    public SessionExpiredException(int resCode, String message) {
        super(message);
        this.resCode = resCode;
    }

    public int getResCode() {
        return resCode;
    }
}

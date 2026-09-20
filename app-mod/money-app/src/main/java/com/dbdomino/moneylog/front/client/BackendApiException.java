package com.dbdomino.moneylog.front.client;

import tools.jackson.databind.JsonNode;

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
 *
 * <h2>구조를 가진 실패가 하나 있다 (010)</h2>
 *
 * <p>007 은 실패를 <b>코드와 문구</b>로 모형화했는데, 백엔드 계약에는 처음부터 구조를 가진
 * 실패가 하나 있었다 — 엑셀 일괄 등록의 행 오류({@code 3502})다. 행 번호·열·사유가 배열로
 * 응답 본문에 함께 오는데, 문구 하나만 꺼내고 나머지를 버리면 <b>"몇 행의 무엇을 고쳐야
 * 하는지"가 화면에 닿지 않는다.</b>
 *
 * <p><b>화면이 따로 부를 수도 없다</b> — 실패한 그 요청의 응답에만 들어 있는 값이다. 그래서
 * 봉투의 {@code data} 를 통째로 실어 올린다.
 *
 * <p><b>기존 두 값은 그대로 두고 더하기만 했다.</b> {@link #payload()} 를 쓰지 않는 화면은
 * 지금과 똑같이 동작한다 — 008·009 의 시험이 그대로 통과하는 것으로 확인한다.
 *
 * <p>문구를 이어 붙여 넘기지 않는 이유는, 백엔드가 구조로 준 것을 화면이 글로 풀어 버리면
 * <b>행 번호로 정렬하거나 표로 보이는 길이 막히기 때문</b>이다.
 */
public class BackendApiException extends RuntimeException {

    private final int resCode;

    /**
     * 실패 봉투의 {@code data} 전체. 없으면 {@code null}.
     *
     * <p><b>해석하지 않은 채로 들고 있다.</b> 이 통로는 안쪽이 무엇인지 알 수 없고, 아는
     * 것은 그 값을 쓸 화면뿐이다. 여기서 타입을 정하면 화면이 늘 때마다 이 클래스를 고쳐야
     * 한다.
     */
    private final transient JsonNode payload;

    public BackendApiException(int resCode, String message) {
        this(resCode, message, null);
    }

    public BackendApiException(int resCode, String message, JsonNode payload) {
        super(message);
        this.resCode = resCode;
        this.payload = payload;
    }

    public int getResCode() {
        return resCode;
    }

    /**
     * 실패 봉투의 {@code data} 를 그대로 돌려준다. 없으면 {@code null}.
     *
     * <p>꺼내 쓰는 쪽이 자기 모양으로 읽는다. 쓰지 않는 화면은 이 값을 보지 않으며, 그것이
     * 지금까지의 동작이다.
     */
    public JsonNode payload() {
        return payload;
    }

    /** 봉투에 값이 함께 왔는가. 구조를 가진 실패인지 화면이 먼저 가리는 자리다. */
    public boolean hasPayload() {
        return payload != null && !payload.isNull();
    }
}

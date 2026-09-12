package com.dbdomino.moneylog.front.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

/**
 * 백엔드 응답 봉투 {@code { resCode, data }} 의 <b>역직렬화 전용</b> 타입.
 *
 * <p>common-mod 의 {@code RestResponseDto} 를 쓰지 않는다. 그 클래스는 생성자가 private
 * 이고 정적 팩터리로만 만들어지며 {@code @JsonCreator} 도 없어 Jackson 이 인스턴스를 만들
 * 방법이 없다. 애초에 그것은 백엔드가 <b>응답을 만들 때</b>의 타입이고, 화면 모듈은 반대
 * 방향이다 — 같은 모양이라고 같은 클래스를 쓸 수 있는 것이 아니다.
 *
 * <p>{@code data} 를 {@link JsonNode} 로 받아 두고 호출부가 원하는 타입으로 나중에
 * 변환한다. 봉투를 읽는 시점에는 안쪽이 무엇인지 알 수 없기 때문이다.
 */
public record ApiEnvelope(
        @JsonProperty("resCode") int resCode,
        @JsonProperty("data") JsonNode data) {

    /** 성공 코드. 실패는 정수 4자리다(헌장 원칙 III). */
    public static final int SUCCESS = 200;

    public boolean isSuccess() {
        return resCode == SUCCESS;
    }

    /**
     * 실패 응답의 안내 문구. 백엔드가 준 문구를 그대로 돌려준다.
     *
     * <p>없으면 {@code null} 이다. 화면 모듈이 대신 문구를 지어내지 않는다 — 지어내면
     * 백엔드가 문구를 고쳐도 화면이 옛 문구를 계속 보여 준다.
     */
    public String message() {
        if (data == null || !data.has("message")) {
            return null;
        }
        return data.get("message").asString();
    }
}

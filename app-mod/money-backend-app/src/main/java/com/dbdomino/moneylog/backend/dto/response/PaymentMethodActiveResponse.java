package com.dbdomino.moneylog.backend.dto.response;

/**
 * 사용 중 수단 1건(2.6). 지출·소득 입력 화면이 고를 수 있는 것만 담는다.
 *
 * <p><b>{@code purpose}·{@code inUse}·{@code deleted} 가 없다.</b> 셋 다 필터가 이미
 * 정해 놓은 값이라 실어도 읽을 것이 없다 — {@code purpose} 는 요청 경로가 지시했고,
 * {@code inUse} 는 항상 {@code true}, {@code deleted} 는 항상 {@code false} 다.
 * 필드가 아예 없으므로 필터를 잘못 고쳐도 "삭제된 수단인데 {@code deleted=false} 로
 * 나가는" 응답을 만들 수 없다.
 *
 * <p>관리 화면(2.2·2.3)이 쓰는 넓은 타입은 {@link PaymentMethodResponse} 다.
 *
 * @param paymentMethodId 수단 대리키({@code idx})
 * @param name            수단 이름. 입력 화면의 선택지에 그대로 보인다
 * @param type            {@code CARD} 또는 {@code ACCOUNT}. 화면이 아이콘을 가를 때 쓴다
 * @param cardExpiry      카드 유효기간 {@code YYYY-MM}. 계좌면 {@code null}
 */
public record PaymentMethodActiveResponse(Long paymentMethodId, String name, String type,
                                          String cardExpiry) {
}

package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 수단 목록의 응답 껍데기. 관리 목록(2.2)과 사용 중 목록(2.6)이 함께 쓴다.
 *
 * <p><b>{@code list} 하나만 담는다.</b> {@code offset}·{@code limit}·{@code totalCount} 를
 * 싣지 않는다(FR-217) — 본인 보유 수단은 수가 제한적이라 페이징을 두지 않기로 했고,
 * 페이징 없는 목록에 "전체 건수"를 붙이면 의미 없는 필드가 생긴다.
 *
 * <p>002 의 관리자 회원 목록(1.13)이 페이징 3필드를 싣는 것과 형태가 다른 것은
 * <b>의도된 차이</b>다. 회원 목록은 전체 회원을 보는 관리자 화면이라 수가 늘지만
 * 수단은 그렇지 않다.
 *
 * <h2>항목 타입을 고정하지 않는 이유</h2>
 *
 * <p>두 목록의 <b>항목 필드가 다르다.</b> 2.2 는 관리 화면이라 {@code purpose}·
 * {@code inUse}·{@code deleted} 까지 내려보내지만, 2.6 은 입력 화면이 고를 것만 주므로
 * {@code paymentMethodId}·{@code name}·{@code type}·{@code cardExpiry} 넷뿐이다.
 * 2.6 에서 {@code deleted} 는 항상 {@code false}, {@code inUse} 는 항상 {@code true} 라
 * 실어 봐야 읽을 것이 없다 — 필터가 이미 그 값을 정해 놓았다.
 *
 * <p>그래서 껍데기만 공유하고 항목 타입은 API 가 고른다. 넓은 타입 하나로 합치면 2.6 이
 * 명세에 없는 필드를 내보내게 되고, 껍데기를 둘로 나누면 "{@code list} 하나만"이라는
 * 규칙이 두 곳에 적히게 된다.
 *
 * @param <T> 항목 타입 — {@link PaymentMethodResponse}(2.2) 또는
 *            {@link PaymentMethodActiveResponse}(2.6)
 * @param list 수단 목록. 정렬은 등록 순({@code idx} 오름차순)으로 고정한다
 */
public record PaymentMethodListResponse<T>(List<T> list) {
}

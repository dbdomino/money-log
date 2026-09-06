package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 수단 목록(2.2 관리 목록 · 2.6 사용 중 목록)의 응답.
 *
 * <p><b>{@code list} 하나만 담는다.</b> {@code offset}·{@code limit}·{@code totalCount} 를
 * 싣지 않는다(FR-217) — 본인 보유 수단은 수가 제한적이라 페이징을 두지 않기로 했고,
 * 페이징 없는 목록에 "전체 건수"를 붙이면 의미 없는 필드가 생긴다.
 *
 * <p>002 의 관리자 회원 목록(1.13)이 페이징 3필드를 싣는 것과 형태가 다른 것은
 * <b>의도된 차이</b>다. 회원 목록은 전체 회원을 보는 관리자 화면이라 수가 늘지만
 * 수단은 그렇지 않다.
 *
 * @param list 수단 목록. 정렬은 등록 순({@code idx} 오름차순)으로 고정한다
 */
public record PaymentMethodListResponse(List<PaymentMethodResponse> list) {
}

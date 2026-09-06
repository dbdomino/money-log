package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 지출유형 목록의 응답 껍데기. 관리 목록(2.8)과 사용 중 목록(2.13)이 함께 쓴다.
 *
 * <p><b>{@code list} 하나만 담는다.</b> {@code offset}·{@code limit}·{@code totalCount} 를
 * 싣지 않는다(FR-217) — 본인 지출유형은 기본 10종에서 몇 개 늘어나는 정도라 페이징을
 * 두지 않기로 했다.
 *
 * <p>항목 타입을 고정하지 않는 이유는 {@link PaymentMethodListResponse} 와 같다 — 관리
 * 화면(2.8)과 입력 화면(2.13)이 필요로 하는 필드가 다르다. 2.13 은 {@code inUse} 와
 * {@code deleted} 를 싣지 않는데, 필터가 이미 두 값을 정해 놓았기 때문이다.
 *
 * @param <T> 항목 타입 — 2.13 은 {@link ExpendGroupActiveResponse}
 * @param list 지출유형 목록. 정렬은 등록 순({@code idx} 오름차순)으로 고정한다
 */
public record ExpendGroupListResponse<T>(List<T> list) {
}

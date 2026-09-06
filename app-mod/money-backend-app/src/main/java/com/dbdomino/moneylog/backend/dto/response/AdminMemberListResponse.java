package com.dbdomino.moneylog.backend.dto.response;

import java.util.List;

/**
 * 1.13 관리자 회원 목록 응답.
 *
 * <p>목록은 {@code data.list}(object 배열)로 통일한다(헌장 · FR-120).
 *
 * <p><b>{@code page}·{@code totalPages} 를 싣지 않는다.</b> {@code offset}/{@code limit}
 * 모델과 {@code page} 모델을 한 응답에 섞으면 프론트가 어느 쪽을 신뢰할지 갈린다.
 * 페이지 환산이 필요하면 이 세 값으로 화면이 계산한다.
 *
 * @param list       회원 목록
 * @param offset     이번 조회에서 건너뛴 건수. 요청값과 같다
 * @param limit      이번 조회에서 가져올 최대 건수. 요청값과 같다
 * @param totalCount 검색 조건에 걸린 <b>전체</b> 건수. 현재 페이지 건수가 아니다
 */
public record AdminMemberListResponse(List<AdminMemberResponse> list, int offset, int limit,
                                      long totalCount) {
}

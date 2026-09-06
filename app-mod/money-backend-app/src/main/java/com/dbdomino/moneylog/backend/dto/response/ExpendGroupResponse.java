package com.dbdomino.moneylog.backend.dto.response;

/**
 * 지출유형 1건. 등록(2.7)·관리 목록(2.8)·상세(2.9)·수정(2.11)이 함께 쓴다.
 *
 * <p><b>{@code deleted} 를 싣는다</b>(FR-223). 관리 목록이 삭제 표시된 유형까지 돌려주므로
 * 이 필드가 없으면 화면이 살아 있는 유형과 삭제된 유형을 구분할 수 없다.
 *
 * <p>사용 중 목록(2.13)은 이 타입을 쓰지 않는다({@link ExpendGroupActiveResponse}) —
 * 그 목록에서는 {@code inUse}·{@code deleted} 가 필터로 이미 정해져 있다.
 *
 * <p>Entity 를 그대로 내보내지 않는다(헌장 원칙 II). {@code icon_filename} 도 나가지
 * 않는다 — API 가 주는 것은 파일명이 아니라 조회 경로({@code iconUrl})다.
 *
 * @param expendGroupId 지출유형 대리키({@code idx})
 * @param name          유형 이름
 * @param inUse         사용 여부. {@code false} 면 사용 중 목록(2.13)에서 빠진다
 * @param iconUrl       아이콘 조회 경로 {@code /api/v1/expend-groups/icons/{filename}}.
 *                      아이콘이 없으면 {@code null} 이며 <b>필드를 생략하지 않는다</b>(SC-209)
 * @param defaultGroup  가입 시 자동 생성된 기본 유형 여부. {@code true} 면 이름 변경
 *                      ({@code 3105})과 삭제({@code 3107})가 막힌다
 * @param deleted       삭제 표시 여부. {@code true} 여도 관리 목록(2.8)과 상세(2.9)에는 남는다
 */
public record ExpendGroupResponse(Long expendGroupId, String name, boolean inUse, String iconUrl,
                                  boolean defaultGroup, boolean deleted) {
}

package com.dbdomino.moneylog.backend.dto.response;

/**
 * 사용 중 지출유형 1건(2.13). 지출 등록·고정지출 화면이 고를 수 있는 것만 담는다.
 *
 * <p><b>{@code inUse}·{@code deleted} 가 없다.</b> 필터가 이미 두 값을 정해 놓은
 * 목록이라 실어도 읽을 것이 없다. 필드가 아예 없으므로 필터를 잘못 고쳐도 "사용하지 않는
 * 유형인데 {@code inUse=true} 로 나가는" 응답을 만들 수 없다.
 *
 * <p><b>{@code defaultGroup} 은 싣는다.</b> 이 값이 {@code true} 면 이름 변경({@code 3105})과
 * 삭제({@code 3107})가 막히므로, 화면이 미리 그 버튼을 잠글 수 있어야 한다 — 눌러 보고
 * 실패로 알게 하는 것과 다르다.
 *
 * @param expendGroupId 지출유형 대리키({@code idx})
 * @param name          유형 이름
 * @param iconUrl       아이콘 조회 경로 {@code /api/v1/expend-groups/icons/{filename}}.
 *                      아이콘이 없으면 {@code null} 이며 <b>필드 자체를 생략하지 않는다</b>
 *                      (SC-209) — 프론트가 {@code 'iconUrl' in obj} 로 분기하면 생략과
 *                      {@code null} 이 다른 결과를 낸다
 * @param defaultGroup  가입 시 자동 생성된 기본 유형 여부
 */
public record ExpendGroupActiveResponse(Long expendGroupId, String name, String iconUrl,
                                        boolean defaultGroup) {
}

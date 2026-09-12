package com.dbdomino.moneylog.front.expendgroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 백엔드 지출유형 응답을 <b>받는 자리</b>. 화면이 쓰는 값은 {@link ExpendGroupView} 다.
 *
 * <p>둘을 나눈 이유는 <b>아이콘 주소가 화면까지 가지 않게</b> 하기 위해서다. 하나로 두면
 * 주소가 화면 값에 남고, 남은 값은 언젠가 이미지에 걸린다 — 그 순간 브라우저가 백엔드를
 * 직접 부르고 인증이 붙지 않아 실패한다.
 *
 * @param expendGroupId 식별자
 * @param name 유형 이름
 * @param inUse 사용 여부
 * @param iconUrl 아이콘을 받을 <b>백엔드 주소</b>. 아이콘이 없으면 {@code null}
 * @param defaultGroup 기본 유형 여부
 * @param deleted 삭제 표시 여부
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpendGroupResponse(
        Long expendGroupId,
        String name,
        Boolean inUse,
        String iconUrl,
        Boolean defaultGroup,
        Boolean deleted) {
}

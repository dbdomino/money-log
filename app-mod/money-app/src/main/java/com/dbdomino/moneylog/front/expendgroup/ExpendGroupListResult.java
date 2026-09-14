package com.dbdomino.moneylog.front.expendgroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 백엔드 지출유형 목록 응답.
 *
 * <p><b>쪽 정보가 없다.</b> 이 목록은 조회 구간을 받지 않고 본인 것을 전부 돌려준다.
 *
 * <p>요소가 {@link ExpendGroupResponse} 인 이유는 <b>아이콘 주소가 아직 주소이기 때문</b>이다.
 * 파일명으로 바꾸는 일은 화면에 싣기 직전에 한다 — 주소가 화면 값에 남으면 언젠가 이미지에
 * 걸리고, 그 순간 브라우저가 백엔드를 직접 부른다.
 *
 * @param list 기본·직접 만든 유형 전체. <b>삭제 표시된 것까지 함께</b> 온다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpendGroupListResult(List<ExpendGroupResponse> list) {

    /** 행이 없으면 빈 목록으로 다룬다. */
    public List<ExpendGroupResponse> rows() {
        return list == null ? List.of() : list;
    }
}

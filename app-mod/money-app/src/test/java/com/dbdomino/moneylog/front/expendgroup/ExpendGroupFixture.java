package com.dbdomino.moneylog.front.expendgroup;

import java.util.List;

/**
 * 지출유형 시험이 함께 쓰는 자료.
 *
 * <p>목록에 <b>기본·직접 만듦·아이콘 없음·삭제됨</b>을 함께 담는다. 화면이 이 넷을 다르게
 * 다루는 것이 이 기능의 요점이라, 시험마다 다른 자료를 세우면 어느 시험이 무엇을 보는지
 * 흐려진다.
 */
final class ExpendGroupFixture {

    /** 백엔드가 주는 아이콘 주소. 화면에는 이 값이 아니라 파일명만 실린다. */
    static final String ICON_URL = "/api/v1/expend-groups/icons/1_1.png";

    private ExpendGroupFixture() {
    }

    /** 가입 때 생긴 기본 유형. 이름을 바꿀 수 없고 지울 수 없다. */
    static ExpendGroupResponse defaultGroup() {
        return new ExpendGroupResponse(1L, "식비", true, ICON_URL, true, false);
    }

    /** 직접 만든 유형. 아이콘이 있고 모든 동작을 열 수 있다. */
    static ExpendGroupResponse custom() {
        return new ExpendGroupResponse(2L, "취미", true, "/api/v1/expend-groups/icons/1_2.jpg",
                false, false);
    }

    /** 아이콘이 없는 유형. 이미지 자리가 깨지지 않아야 한다. */
    static ExpendGroupResponse withoutIcon() {
        return new ExpendGroupResponse(3L, "기타", false, null, false, false);
    }

    /** 삭제 표시된 유형. 상세만 열 수 있고 아이콘은 계속 보인다. */
    static ExpendGroupResponse deleted() {
        return new ExpendGroupResponse(4L, "없앤유형", true,
                "/api/v1/expend-groups/icons/1_4.gif", false, true);
    }

    static ExpendGroupListResult page() {
        return new ExpendGroupListResult(
                List.of(defaultGroup(), custom(), withoutIcon(), deleted()));
    }
}

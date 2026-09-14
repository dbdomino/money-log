package com.dbdomino.moneylog.front.expendgroup;

import com.dbdomino.moneylog.front.support.IconUrls;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 화면이 보여 주는 지출유형. 목록(2.5)과 모달 셋(2.6~2.8)이 같은 모양을 쓴다.
 *
 * <h2>아이콘은 주소가 아니라 파일명으로 들고 있다</h2>
 *
 * <p>백엔드는 아이콘을 받을 <b>주소</b>를 준다. 그것을 그대로 들고 있으면 그 값이 이미지에
 * 걸릴 길이 생기고, 그 순간 브라우저가 백엔드를 직접 부른다 — 인증이 붙지 않아 전부 실패한다.
 * 파일명만 들고 있으면 화면이 쓸 수 있는 주소는 007 의 중계 경로 하나뿐이다.
 *
 * <p>꺼내는 일은 {@link IconUrls} 가 하고, 이 타입은 <b>꺼낸 결과</b>를 담는다.
 *
 * <h2>기본 유형 여부가 화면 동작의 근거다</h2>
 *
 * <p>기본 유형은 이름을 바꿀 수 없고 지울 수 없다. 목록 응답이 그것을 알려 주므로 화면이
 * 미리 막을 수 있다 — 고칠 수 없는 칸을 열어 두고 저장에서 실패시킬 이유가 없다.
 *
 * @param expendGroupId 식별자. 주소와 요청 경로에만 쓴다
 * @param name 유형 이름
 * @param inUse 사용 여부. <b>되돌릴 수 있는</b> 상태다
 * @param iconFilename 아이콘 파일명. 아이콘이 없거나 주소가 틀에 맞지 않으면 {@code null}
 * @param defaultGroup 기본 유형 여부. 이름 칸을 잠그고 삭제 버튼을 감추는 근거다
 * @param deleted 삭제 표시 여부. <b>되돌릴 수 없는</b> 상태다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpendGroupView(
        Long expendGroupId,
        String name,
        Boolean inUse,
        String iconFilename,
        Boolean defaultGroup,
        Boolean deleted) {

    /**
     * 백엔드 응답에서 화면 값으로 옮긴다. <b>아이콘 주소를 파일명으로 바꾸는 곳이 여기다.</b>
     *
     * <p>응답을 그대로 역직렬화하지 않고 한 번 거치게 한 이유는, 주소가 이 타입에 들어올
     * 길을 아예 없애기 위해서다.
     */
    public static ExpendGroupView from(ExpendGroupResponse response) {
        return new ExpendGroupView(
                response.expendGroupId(),
                response.name(),
                response.inUse(),
                IconUrls.filenameOf(response.iconUrl()),
                response.defaultGroup(),
                response.deleted());
    }

    /** 사용 여부를 화면에 보일 말로 바꾼다. 값이 없으면 사용 중으로 본다. */
    public String inUseLabel() {
        return inUse == null || inUse ? "사용" : "사용 안 함";
    }

    /** 삭제 여부를 화면에 보일 말로 바꾼다. */
    public String statusLabel() {
        return isDeleted() ? "삭제됨" : "정상";
    }

    /** 기본 유형인지 화면에 보일 말로 바꾼다. */
    public String originLabel() {
        return isDefaultGroup() ? "기본" : "직접 만듦";
    }

    /**
     * 기본 유형인가. 이름 칸을 잠그고 삭제 버튼을 그리지 않는 근거다.
     *
     * <p>화면이 미리 막아도 <b>백엔드 실패를 함께 다룬다</b> — 주소를 직접 쳐서 올 수 있고,
     * 화면이 막는 것과 서버가 막는 것은 서로를 대신하지 않는다.
     */
    public boolean isDefaultGroup() {
        return defaultGroup != null && defaultGroup;
    }

    /** 삭제 표시됐는가. 목록이 이 값으로 수정·삭제 버튼을 그릴지 정한다. */
    public boolean isDeleted() {
        return deleted != null && deleted;
    }

    /** 이미지를 걸 수 있는가. 없으면 템플릿이 대체 표시를 그린다. */
    public boolean hasIcon() {
        return IconUrls.hasIcon(iconFilename);
    }
}

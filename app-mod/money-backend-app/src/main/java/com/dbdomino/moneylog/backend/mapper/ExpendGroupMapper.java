package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.ExpendGroupActiveResponse;
import com.dbdomino.moneylog.backend.dto.response.ExpendGroupResponse;
import com.dbdomino.moneylog.data.entity.UserExpendGroup;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * {@code UserExpendGroup} Entity → 응답 DTO 변환.
 *
 * <h2>{@code iconUrl} 조립을 여기서 한다</h2>
 *
 * <p>DB 에 들어 있는 것은 <b>파일명 하나</b>({@code icon_filename})이고 API 가 내보내는 것은
 * <b>경로</b>({@code /api/v1/expend-groups/icons/{filename}})다. 그 사이를 잇는 일을
 * Service 에 두지 않는다 — Service 가 HTTP 경로 문자열을 알 이유가 없고, 응답을 만드는
 * 지점이 Mapper 다(research.md §5).
 *
 * <p>DB 에 경로 전체를 저장하지 않는 이유도 같다. Base URL 이 바뀌면 저장된 전 행을 고쳐야
 * 하는데, 파일명만 두면 조립 규칙 한 줄만 바꾸면 된다.
 *
 * <p><b>파일명이 {@code null} 이면 {@code iconUrl} 도 {@code null} 이며 필드를 생략하지
 * 않는다</b>(SC-209). 레코드 필드라 Jackson 이 값을 빼는 설정({@code NON_NULL})을 걸지 않는
 * 한 항상 나간다 — 이 프로젝트는 그 설정을 쓰지 않는다.
 */
@Mapper(componentModel = "spring")
public interface ExpendGroupMapper {

    /**
     * 아이콘 조회 API 의 경로 앞부분. 2.10 의 URL 과 <b>같은 문자열이어야 한다</b> —
     * 갈리면 목록이 알려 준 주소로 아이콘을 받을 수 없다.
     */
    String ICON_URL_PREFIX = "/api/v1/expend-groups/icons/";

    /** 등록(2.7)·상세(2.9)·수정(2.11)의 넓은 항목. {@code deleted} 까지 싣는다. */
    @Mapping(target = "expendGroupId", source = "idx")
    @Mapping(target = "iconUrl", source = "iconFilename", qualifiedByName = "toIconUrl")
    ExpendGroupResponse toResponse(UserExpendGroup entity);

    /** 관리 목록(2.8). 순서는 넘겨받은 그대로 유지한다 — 정렬은 Repository 가 정한다. */
    List<ExpendGroupResponse> toResponses(List<UserExpendGroup> entities);

    /** 사용 중 목록(2.13)의 좁은 항목. {@code inUse}·{@code deleted} 는 대상에 없어 버려진다. */
    @Mapping(target = "expendGroupId", source = "idx")
    @Mapping(target = "iconUrl", source = "iconFilename", qualifiedByName = "toIconUrl")
    ExpendGroupActiveResponse toActiveResponse(UserExpendGroup entity);

    /** 사용 중 목록(2.13). 순서는 넘겨받은 그대로 유지한다 — 정렬은 Repository 가 정한다. */
    List<ExpendGroupActiveResponse> toActiveResponses(List<UserExpendGroup> entities);

    /** 파일명 → 조회 경로. 파일명이 없으면 경로도 없다. */
    @Named("toIconUrl")
    default String toIconUrl(String iconFilename) {
        return iconFilename == null || iconFilename.isBlank()
                ? null
                : ICON_URL_PREFIX + iconFilename;
    }
}

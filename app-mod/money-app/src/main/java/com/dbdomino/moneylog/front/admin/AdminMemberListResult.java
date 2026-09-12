package com.dbdomino.moneylog.front.admin;

import com.dbdomino.moneylog.front.member.MemberView;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 백엔드 {@code AdminMemberList} 성공 응답.
 *
 * <p>전체 쪽 수는 <b>{@code totalCount} 로 환산한다</b>. 현재 쪽의 행 수로 계산하면 마지막
 * 쪽에서 쪽 수가 줄어들어, 사용자는 3쪽에 있다가 갑자기 2쪽짜리 목록을 보게 된다.
 *
 * @param list 이 쪽의 회원들
 * @param offset 이번 조회에서 건너뛴 건수. 지금 몇 쪽인지 환산하는 데 쓴다
 * @param limit 이번 조회에서 가져온 최대 건수
 * @param totalCount 검색 조건에 맞는 <b>전체</b> 건수. 이 쪽의 행 수가 아니다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminMemberListResult(
        List<MemberView> list,
        int offset,
        int limit,
        long totalCount) {

    /** 행이 없으면 빈 목록으로 다룬다. 템플릿이 null 을 가리는 분기를 갖지 않게 한다. */
    public List<MemberView> rows() {
        return list == null ? List.of() : list;
    }
}

package com.dbdomino.moneylog.front.admin.form;

import com.dbdomino.moneylog.front.support.PhoneNumbers;
import com.dbdomino.moneylog.front.web.Paging;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.8 회원 목록의 검색 조건. 아이디·닉네임 두 칸과 쪽 번호다.
 *
 * <h2>왜 두 칸인가</h2>
 *
 * <p>백엔드 목록 API 가 두 값을 받아 <b>둘 다 맞는 회원</b>만 돌려준다. 프로토타입처럼 칸
 * 하나에 받아 두 조건에 똑같이 실으면 "아이디에도 있고 닉네임에도 있는" 회원만 걸려서,
 * 사용자가 기대한 "아이디 <b>또는</b> 닉네임"과 정반대로 동작한다. 그리고 그 오작동은
 * <b>결과가 비는 형태</b>로 나타나 원인을 짐작하기 어렵다.
 *
 * <h2>검색 폼은 쪽 번호를 싣지 않는다</h2>
 *
 * <p>새 검색은 언제나 쪽 번호 없는 요청이 되고 기본값이 첫 쪽이다. 비교도 저장도 필요 없다.
 * 싣고 다니면 3쪽에서 검색했을 때 결과가 한 쪽뿐인데도 3쪽을 요구해 빈 화면이 뜬다.
 *
 * @param memberId 아이디 부분 일치. 비어 있으면 아이디로 좁히지 않는다
 * @param nickname 닉네임 부분 일치. 비어 있으면 닉네임으로 좁히지 않는다
 * @param page 0 부터 세는 쪽 번호. 쪽 이동 링크만 싣는다
 */
public record AdminMemberSearch(String memberId, String nickname, int page) {

    /** 주소에 실려 온 값으로 만든다. 쪽 번호가 없거나 음수면 첫 쪽이다. */
    public static AdminMemberSearch of(String memberId, String nickname, Integer page) {
        int resolved = page == null || page < 0 ? 0 : page;
        return new AdminMemberSearch(trimToNull(memberId), trimToNull(nickname), resolved);
    }

    public Paging paging() {
        return Paging.of(page, Paging.DEFAULT_LIMIT);
    }

    /**
     * 백엔드 목록 조회에 실을 Query.
     *
     * <p>조회 구간은 007 의 환산기가 만든다 — 화면이 직접 계산하면 개수의 배수가 아닌 값이
     * 새어 나가 목록이 통째로 실패한다. 비어 있는 검색어는 싣지 않는다.
     */
    public Map<String, Object> toQuery() {
        Map<String, Object> query = new LinkedHashMap<>(paging().toQuery());
        if (memberId != null) {
            query.put("memberId", memberId);
        }
        if (nickname != null) {
            query.put("nickname", nickname);
        }
        return query;
    }

    /** 검색어가 하나라도 걸려 있는가. 화면이 "검색 중"임을 알릴 때 본다. */
    public boolean hasKeyword() {
        return memberId != null || nickname != null;
    }

    private static String trimToNull(String value) {
        if (PhoneNumbers.isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}

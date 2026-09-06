package com.dbdomino.moneylog.backend.target;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * 5.1~5.4 목표금액 통합 테스트의 공통 바탕 (US1).
 *
 * <p><b>고정 연월을 쓴다.</b> 목표금액은 "지금이 언제인가"와 무관하다 — 미래 월 판정은
 * 통계 저장(5.6)에만 있고 그 검증은 {@code statistics} 패키지가 맡는다.
 */
abstract class AbstractTargetIT extends AbstractApiIT {

    protected static final String URL = "/api/v1/expend-targets";

    /** 조회·저장 대상 연·월. 허용 범위(2000~2100) 한가운데 값이다. */
    protected static final int YEAR = 2026;

    /** @see #YEAR */
    protected static final int MONTH = 7;

    /** 가입이 만든 기본 유형 둘을 들고 있는 회원. */
    protected record Fixture(Member member, long foodGroupId, long transportGroupId) {

        String token() {
            return member.token();
        }
    }

    protected Fixture prepare() throws Exception {
        Member member = signupAndLogin();
        return new Fixture(member, defaultGroupId(member, "식비"),
                defaultGroupId(member, "교통"));
    }

    /** 5.2 단건 조회. */
    protected JsonNode getTarget(Fixture fixture, long expendGroupId) throws Exception {
        return getJson(URL + "/" + YEAR + "/" + MONTH + "/" + expendGroupId, fixture.token());
    }

    /** 5.1 목록. {@code offset}·{@code limit} 은 필수다. */
    protected JsonNode listTargets(Fixture fixture, int offset, int limit) throws Exception {
        return getJson(URL + "?year=" + YEAR + "&month=" + MONTH
                + "&offset=" + offset + "&limit=" + limit, fixture.token());
    }

    /** 5.3 기본 목표 저장. */
    protected JsonNode putDefault(Fixture fixture, long expendGroupId, long amount)
            throws Exception {
        return putDefaultTarget(fixture.token(), expendGroupId, amount);
    }

    /** 5.4 월별 목표 저장. */
    protected JsonNode putMonthly(Fixture fixture, long expendGroupId, long amount)
            throws Exception {
        return putMonthlyTarget(fixture.token(), YEAR, MONTH, expendGroupId, amount);
    }

    /** 그 회원의 기본 목표 행 수. upsert 가 행을 늘리지 않는지 보는 데 쓴다. */
    protected int countDefaultTargets(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_expend_target_default t
                  join moneylog.tbl_user u on u.id_key = t.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }

    /** 그 회원의 월별 목표 행 수. */
    protected int countMonthlyTargets(Member member) {
        Integer count = jdbc.queryForObject("""
                select count(*) from moneylog.tbl_expend_target_monthly t
                  join moneylog.tbl_user u on u.id_key = t.id_key
                 where u.user_id = ?
                """, Integer.class, member.memberId());
        return count == null ? 0 : count;
    }

    /** 5.1 응답에서 그 유형의 줄. 없으면 {@code null} 이다. */
    protected JsonNode rowOf(JsonNode response, long expendGroupId) {
        for (JsonNode item : response.get("data").get("list")) {
            if (item.get("expendGroupId").asLong() == expendGroupId) {
                return item;
            }
        }
        return null;
    }

    /** 5.1 응답의 유형 PK 목록. 순서와 포함 여부를 함께 본다. */
    protected List<Long> groupIdsOf(JsonNode response) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : response.get("data").get("list")) {
            ids.add(item.get("expendGroupId").asLong());
        }
        return ids;
    }
}

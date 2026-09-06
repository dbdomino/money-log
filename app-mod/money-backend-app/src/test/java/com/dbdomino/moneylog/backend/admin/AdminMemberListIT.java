package com.dbdomino.moneylog.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import com.dbdomino.moneylog.data.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US4 — 회원 목록(1.13)의 응답 규격과 페이징 검증.
 *
 * <p>quickstart.md §3 의 시나리오 #22·#23·#24 에 대응한다(FR-120).
 */
class AdminMemberListIT extends AbstractApiIT {

    @Test
    @DisplayName("#22 data.list 는 객체 배열이고 offset·limit·totalCount 가 함께 온다. page 는 없다")
    void listFollowsPagingContract() throws Exception {
        User admin = createAdmin();
        createMember();
        createMember();
        Tokens tokens = login(admin);

        JsonNode response = getJson("/api/v1/admin/members?offset=0&limit=2", tokens.accessToken());

        assertThat(resCode(response)).isEqualTo(200);
        JsonNode data = response.get("data");
        assertThat(data.get("list").isArray()).isTrue();
        assertThat(data.get("list").size()).isLessThanOrEqualTo(2);
        assertThat(data.get("offset").asInt()).isZero();
        assertThat(data.get("limit").asInt()).isEqualTo(2);
        // 현재 페이지 건수가 아니라 조건에 걸린 전체 건수다.
        assertThat(data.get("totalCount").asLong()).isGreaterThanOrEqualTo(3);
        // offset/limit 모델과 page 모델을 섞지 않는다.
        assertThat(data.has("page")).isFalse();
        assertThat(data.has("totalPages")).isFalse();
        // 목록 항목에는 정지 여부가 있어야 관리자가 쓸 수 있다.
        JsonNode first = data.get("list").get(0);
        assertThat(first.has("active")).isTrue();
        assertThat(first.has("pw")).isFalse();
    }

    @Test
    @DisplayName("#23 offset 이 limit 의 배수가 아니면 9001 이다")
    void offsetMustBeMultipleOfLimit() throws Exception {
        Tokens tokens = login(createAdmin());

        JsonNode response = getJson("/api/v1/admin/members?offset=3&limit=2", tokens.accessToken());

        assertThat(resCode(response)).isEqualTo(9001);
    }

    @Test
    @DisplayName("#24 offset·limit 을 빠뜨리면 9001 이다 — 기본값으로 통과하지 않는다")
    void missingPagingParametersAreRejected() throws Exception {
        Tokens tokens = login(createAdmin());
        String accessToken = tokens.accessToken();

        // Pageable 자동 바인딩이었다면 기본값(page=0,size=20)으로 채워져 200 이 나갔을 자리다.
        assertThat(resCode(getJson("/api/v1/admin/members", accessToken))).isEqualTo(9001);
        assertThat(resCode(getJson("/api/v1/admin/members?offset=0", accessToken))).isEqualTo(9001);
        assertThat(resCode(getJson("/api/v1/admin/members?limit=10", accessToken))).isEqualTo(9001);
        assertThat(resCode(getJson("/api/v1/admin/members?offset=0&limit=0", accessToken)))
                .isEqualTo(9001);
        assertThat(resCode(getJson("/api/v1/admin/members?offset=-10&limit=10", accessToken)))
                .isEqualTo(9001);
    }

    @Test
    @DisplayName("#22 memberId·nickname 을 둘 다 주면 AND 로 걸리고 totalCount 도 그 조건을 따른다")
    void searchCombinesConditionsWithAnd() throws Exception {
        User admin = createAdmin();
        User target = createMember();
        createMember();
        Tokens tokens = login(admin);

        JsonNode matched = getJson("/api/v1/admin/members?offset=0&limit=10&memberId="
                + target.getUserId() + "&nickname=" + target.getNickname(), tokens.accessToken());
        JsonNode mismatched = getJson("/api/v1/admin/members?offset=0&limit=10&memberId="
                + target.getUserId() + "&nickname=없는닉네임", tokens.accessToken());

        assertThat(matched.get("data").get("totalCount").asLong()).isEqualTo(1);
        assertThat(matched.get("data").get("list").get(0).get("memberId").asString())
                .isEqualTo(target.getUserId());
        // 한쪽만 맞으면 걸리지 않는다 — OR 이었다면 1건이 나왔을 자리다.
        assertThat(mismatched.get("data").get("totalCount").asLong()).isZero();
        assertThat(mismatched.get("data").get("list")).isEmpty();
    }
}

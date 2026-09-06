package com.dbdomino.moneylog.backend.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 2.7 지출유형 이름 유일성 — quickstart #15·#16·#17.
 *
 * <p>유일성의 범위는 <b>회원 안</b>이다. 유니크 제약이 {@code (id_key, name)} 이므로 다른
 * 회원끼리는 같은 이름을 쓸 수 있다 — "식비"는 모두가 갖는 기본 유형이라 그래야만 한다.
 *
 * <p>#17 이 이 클래스의 핵심이다. 제약 {@code ux_user_expend_group_name} 에 <b>{@code WHERE}
 * 절이 없어</b> 삭제 표시된 행도 이름을 계속 차지한다(FR-209). 애플리케이션 선검사가
 * {@code deleted=false} 만 세면 통과시켰다가 DB 에서 터진다.
 */
class ExpendGroupNameUniqueIT extends AbstractExpendGroupIT {

    @Test
    @DisplayName("#15 같은 회원이 같은 이름으로 두 번 등록하면 3101 이다")
    void duplicateNameForSameMemberIs3101() throws Exception {
        String token = signupAndLogin().token();

        assertThat(resCode(createGroup(token, "취미", true))).isEqualTo(200);

        assertThat(resCode(createGroup(token, "취미", true))).isEqualTo(3101);
    }

    @Test
    @DisplayName("#15 가입 시 생긴 기본 유형과 같은 이름도 3101 이다")
    void nameClashingWithDefaultGroupIs3101() throws Exception {
        String token = signupAndLogin().token();

        assertThat(resCode(createGroup(token, "식비", true))).isEqualTo(3101);
    }

    @Test
    @DisplayName("#16 다른 회원이 같은 이름으로 등록하면 둘 다 성공한다")
    void sameNameForDifferentMembersSucceeds() throws Exception {
        String first = signupAndLogin().token();
        String second = signupAndLogin().token();

        assertThat(resCode(createGroup(first, "취미", true))).isEqualTo(200);
        // 제약이 (id_key, name) 이라 회원이 다르면 충돌하지 않는다.
        assertThat(resCode(createGroup(second, "취미", true))).isEqualTo(200);
    }

    @Test
    @DisplayName("#17 삭제 표시 후 같은 이름으로 재등록해도 3101 이다")
    void nameStaysTakenAfterSoftDelete() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));

        assertThat(resCode(deleteJson(URL + "/" + id, token))).isEqualTo(200);

        // 삭제는 표시일 뿐이라 행이 남아 이름을 계속 차지한다.
        assertThat(resCode(createGroup(token, "취미", true))).isEqualTo(3101);
    }

    @Test
    @DisplayName("2.11 로 남의 이름이 아닌 자기 다른 유형의 이름으로 바꾸면 3101 이다")
    void renamingToAnExistingNameIs3101() throws Exception {
        String token = signupAndLogin().token();
        idOf(createGroup(token, "취미", true));
        long other = idOf(createGroup(token, "여행", true));

        assertThat(resCode(updateGroup(token, other, "취미", null))).isEqualTo(3101);
    }

    @Test
    @DisplayName("자기 이름 그대로 보내는 수정은 3101 이 아니다")
    void renamingToItsOwnNameIsAllowed() throws Exception {
        String token = signupAndLogin().token();
        long id = idOf(createGroup(token, "취미", true));

        // 자기 자신을 중복으로 세면 "이름을 그대로 두고 사용 여부만 바꾸는" 수정이 막힌다.
        assertThat(resCode(updateGroup(token, id, "취미", false))).isEqualTo(200);
    }

    @Test
    @DisplayName("등록 응답에 defaultGroup=false·deleted=false·iconUrl=null 이 실린다")
    void createdGroupIsUserOwnedAndNotDefault() throws Exception {
        String token = signupAndLogin().token();

        JsonNode data = createGroup(token, "취미", true).get("data");

        assertThat(data.get("defaultGroup").asBoolean()).isFalse();
        assertThat(data.get("deleted").asBoolean()).isFalse();
        assertThat(data.has("iconUrl")).isTrue();
        assertThat(data.get("iconUrl").isNull()).isTrue();
    }

    @Test
    @DisplayName("필수 폼 필드를 빠뜨리면 9001 이다")
    void missingRequiredFieldIs9001() throws Exception {
        String token = signupAndLogin().token();

        assertThat(resCode(createGroup(token, null, true))).isEqualTo(9001);
        assertThat(resCode(createGroup(token, "취미", null))).isEqualTo(9001);
    }
}

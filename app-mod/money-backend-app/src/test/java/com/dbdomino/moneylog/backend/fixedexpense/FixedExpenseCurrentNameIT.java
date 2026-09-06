package com.dbdomino.moneylog.backend.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.3 설정 조회의 이름 규칙 — quickstart #8·#9 (SC-409 의 4.3 몫).
 *
 * <p><b>#9 가 004 와의 차이를 드러낸다.</b> 004 의 지출은 등록 당시 이름을 <b>스냅샷</b>으로
 * 저장해 나중에 원본이 바뀌어도 따라가지 않는다. 005 의 설정은 반대로 <b>조회 시점 현재
 * 이름</b>이다.
 *
 * <table border="1">
 *   <caption>성격이 다르다</caption>
 *   <tr><th></th><th>004 지출·소득</th><th>005 고정지출 설정</th></tr>
 *   <tr><td>무엇인가</td><td><b>과거 기록</b> — 그때 그 이름으로 남아야 한다</td>
 *       <td><b>지금 유효한 설정</b> — 현재 이름이 맞다</td></tr>
 *   <tr><td>이름 컬럼</td><td>있다(스냅샷)</td><td><b>없다</b></td></tr>
 * </table>
 *
 * <p>월세 수단을 "국민카드"에서 "국민체크"로 바꿨다면 고정지출 설정은 <b>지금 그 카드로
 * 나가는 설정</b>이므로 새 이름이 맞다. 반면 3월에 이미 쓴 지출은 그때 이름으로 남아야 한다.
 *
 * <p>{@code tbl_fixed_expense} 에 이름 컬럼이 <b>아예 없다</b>는 것이 이 규칙의 근거다.
 * 이 시험이 깨지면 누군가 스키마에 이름 컬럼을 추가했다는 뜻이다.
 */
class FixedExpenseCurrentNameIT extends AbstractFixedExpenseIT {

    @Test
    @DisplayName("#8 조회의 이름 두 개가 조회 시점 현재 이름이다")
    void getReturnsCurrentNames() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        JsonNode data = getJson(URL + "/" + id, fixture.token()).get("data");

        assertThat(data.get("paymentMethodName").asString()).isEqualTo("국민카드");
        assertThat(data.get("expendGroupName").asString()).isEqualTo("주거");
    }

    @Test
    @DisplayName("#9 수단 이름을 바꾸고 같은 설정을 재조회하면 새 이름이 나온다")
    void renamedPaymentMethodShowsTheNewName() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                        {"name":"국민체크"}
                        """))).isEqualTo(200);

        // 004 의 지출이라면 여기서 "국민카드"가 나와야 맞다. 005 는 반대다.
        assertThat(getJson(URL + "/" + id, fixture.token())
                .get("data").get("paymentMethodName").asString()).isEqualTo("국민체크");
    }

    @Test
    @DisplayName("#9 지출유형 이름을 바꿔도 같은 규칙이다")
    void renamedExpendGroupShowsTheNewName() throws Exception {
        Fixture fixture = prepare();
        // 기본 유형은 이름을 바꿀 수 없으므로(3105) 새 유형을 만들어 쓴다.
        long groupId = createExpendGroup(fixture.token(), "취미");
        long id = createFixedExpense(fixture.token(), "동호회비", fixture.paymentMethodId(),
                groupId, 30000L, 25, START, END);

        assertThat(resCode(renameExpendGroup(fixture.token(), groupId, "여가"))).isEqualTo(200);

        assertThat(getJson(URL + "/" + id, fixture.token())
                .get("data").get("expendGroupName").asString()).isEqualTo("여가");
    }

    @Test
    @DisplayName("#9 목록(4.2)에서도 새 이름으로 바뀐다")
    void listAlsoFollowsTheRename() throws Exception {
        Fixture fixture = prepare();
        createDefaultFixedExpense(fixture);

        assertThat(resCode(patchJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token(), """
                        {"name":"국민체크"}
                        """))).isEqualTo(200);

        assertThat(getJson(URL + "?offset=0&limit=10", fixture.token())
                .get("data").get("list").get(0).get("paymentMethodName").asString())
                .isEqualTo("국민체크");
    }

    @Test
    @DisplayName("삭제 표시된 수단을 쓰던 설정도 조회된다 — 이름이 읽힌다")
    void deletedReferenceStillResolvesItsName() throws Exception {
        Fixture fixture = prepare();
        long id = createDefaultFixedExpense(fixture);

        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + fixture.paymentMethodId(),
                fixture.token()))).isEqualTo(200);

        // 수단은 물리 삭제가 아니라 삭제 표시라 행이 남고, 이름도 그대로 읽힌다.
        // 여기서 실패하면 조회 경로가 사용 가능 여부를 다시 묻고 있다는 뜻이다.
        JsonNode data = getJson(URL + "/" + id, fixture.token()).get("data");
        assertThat(resCode(getJson(URL + "/" + id, fixture.token()))).isEqualTo(200);
        assertThat(data.get("paymentMethodName").asString()).isEqualTo("국민카드");
    }
}

package com.dbdomino.moneylog.backend.monthly;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 4.6 의 거절 — quickstart #25·#26·#27·#28 (FR-411 · api-contract §6).
 *
 * <h2>판정 순서가 결과 코드를 바꾼다</h2>
 *
 * <pre>{@code
 * 1. fixedExpenseId 로 설정 조회 (본인 소유?)  없음·타인 → 3402
 * 2. Path 의 year·month 범위                  오류    → 3403
 * 3. 그 연·월의 월별 내역이 있는가             없음    → 3405
 * 4. 값 검증                                          → 3401
 * }</pre>
 *
 * <p>순서를 지키지 않으면 <b>남의 설정의 존재가 코드 차이로 새어 나간다</b> — 예를 들어
 * 값 검증을 먼저 하면 "남의 설정 + 잘못된 값"이 {@code 3402} 가 아니라 {@code 3401} 로
 * 나가 그 ID 가 실재함이 드러난다.
 *
 * <h2>#25 가 lazy 생성 모델의 대가다</h2>
 *
 * <p>열어 본 적 없는 달은 고칠 수 없다({@code 3405}). 행이 없으니 UPDATE 할 대상이 없다 —
 * 사용자는 먼저 4.5 로 그 달을 열거나 4.9 로 재작성해야 한다.
 */
class MonthlyUpdateRejectIT extends AbstractMonthlyIT {

    private String updateUrl(long fixedExpenseId, int year, int month) {
        return LIST_URL + "/" + year + "/" + month + "/" + fixedExpenseId;
    }

    private JsonNode update(Fixture fixture, int year, int month, String body) throws Exception {
        return patchJson(updateUrl(fixture.fixedExpenseId(), year, month), fixture.token(), body);
    }

    @Test
    @DisplayName("#25 아직 조회되지 않은 달을 고치려 하면 3405 다")
    void unopenedMonthIs3405() throws Exception {
        Fixture fixture = prepare();
        // 2026-11 은 적용 기간 안이지만 아직 한 번도 열지 않았다.
        assertThat(countMonthly(fixture.member(), 2026, 11)).isZero();

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(3405);
    }

    @Test
    @DisplayName("#25 먼저 4.5 로 열면 같은 요청이 성공한다")
    void openingTheMonthFirstMakesItWork() throws Exception {
        Fixture fixture = prepare();
        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(3405);

        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"amount":300000}
                """))).isEqualTo(200);
    }

    @Test
    @DisplayName("#25 적용 기간 밖의 달도 3405 다 — 행이 없는 것은 같다")
    void outsideThePeriodIsAlso3405() throws Exception {
        Fixture fixture = prepare();

        // 기간이 2026-11 ~ 2027-02 라 2026-10 은 아무리 열어도 행이 생기지 않는다.
        listMonthly(fixture, 2026, 10);
        assertThat(resCode(update(fixture, 2026, 10, """
                {"amount":300000}
                """))).isEqualTo(3405);
    }

    @Test
    @DisplayName("#26 빈 Body 는 3401 이다 — omit 규칙의 예외다")
    void emptyBodyIs3401() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        // PATCH omit 규칙상 "아무것도 안 바꾼다"로 읽히지만 설계 명세가 거절한다 —
        // 의미 없는 요청이 성공으로 흘러 modified=true 만 세우는 것을 막는다.
        assertThat(resCode(update(fixture, 2026, 11, "{}"))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#26 거절된 빈 Body 는 modified 를 세우지 않는다")
    void rejectedEmptyBodyLeavesModifiedFalse() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, "{}"))).isEqualTo(3401);

        assertThat(monthlyRowOf(fixture.member(), fixture.fixedExpenseId(), 2026, 11)
                .get("modified")).isEqualTo(false);
    }

    @Test
    @DisplayName("#27 paymentDate 가 Path 의 연·월과 다른 달이면 3401 이다")
    void paymentDateFromAnotherMonthIs3401() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        // 11월 행에 12월 날짜를 넣으면 그 행이 어느 달 것인지가 무너진다.
        assertThat(resCode(update(fixture, 2026, 11, """
                {"paymentDate":"2026-12-05"}
                """))).isEqualTo(3401);
        assertThat(resCode(update(fixture, 2026, 11, """
                {"paymentDate":"2027-11-05"}
                """))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#27 같은 달 안의 날짜는 통과한다")
    void paymentDateWithinTheSameMonthIsAccepted() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, """
                {"paymentDate":"2026-11-01"}
                """))).isEqualTo(200);
        assertThat(resCode(update(fixture, 2026, 11, """
                {"paymentDate":"2026-11-30"}
                """))).isEqualTo(200);
    }

    @Test
    @DisplayName("#28 purpose=INCOME 수단이면 3401 이다 — 3003 이 아니다")
    void incomePurposeMethodIs3401() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long incomeMethod = createIncomePaymentMethod(fixture.token(), "월급통장");

        // 4.1 등록과 같은 규칙이다 — 사용자가 자기 목록에서 고른 수단이라 존재를 감출
        // 이유가 없고, 취할 조치도 "지출용 수단을 고른다"로 다르다.
        assertThat(resCode(update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(incomeMethod)))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#28 사용 안 함·삭제 표시된 수단은 3003 이다 — 자동 생성과 방향이 반대다")
    void deadPaymentMethodIs3003() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long deleted = createExpensePaymentMethod(fixture.token(), "옛 카드");
        assertThat(resCode(deleteJson("/api/v1/payment-methods/" + deleted, fixture.token())))
                .isEqualTo(200);
        long disabled = createExpensePaymentMethod(fixture.token(), "잠깐 안 씀");
        assertThat(resCode(patchJson("/api/v1/payment-methods/" + disabled, fixture.token(), """
                {"inUse":false}
                """))).isEqualTo(200);

        // 자동 생성(4.5·4.8·4.9)은 죽은 참조를 그대로 복사하지만(FR-426), 여기는
        // 사용자가 직접 고르는 경로라 죽은 수단으로 갈아타는 것을 막아야 한다.
        //
        // 코드는 3401 이 아니라 3003 이다 — "없음·타인·사용 불가"를 한 코드로 묶는
        // 004 FR-325 의 규칙을 따르며 4.1 등록과도 같다. 사용자가 취할 조치가 셋 다
        // "다른 수단을 고른다"로 같기 때문이다. 용도 불일치만 3401 인 것은 조치가
        // "지출용 수단을 고른다"로 다르기 때문이다.
        assertThat(resCode(update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(deleted)))).isEqualTo(3003);
        assertThat(resCode(update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(disabled)))).isEqualTo(3003);
    }

    @Test
    @DisplayName("#28 4.1 등록과 같은 코드를 낸다 — 두 경로의 수단 판정이 같다")
    void theSameMethodVerdictAsCreate() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);
        long incomeMethod = createIncomePaymentMethod(fixture.token(), "월급통장");

        String createWithIncomeMethod = """
                {"name":"새 고정지출","paymentMethodId":%d,"expendGroupId":%d,"amount":10000,
                 "paymentDayOfMonth":10,"content":"x",
                 "startYear":2026,"startMonth":11,"endYear":2027,"endMonth":2}
                """.formatted(incomeMethod, fixture.expendGroupId());

        // 같은 수단을 4.1 과 4.6 에 각각 보내면 같은 코드가 나와야 한다. 갈리면
        // 프론트가 경로마다 다른 분기를 짜게 된다.
        assertThat(resCode(postJson("/api/v1/fixed-expenses", fixture.token(),
                createWithIncomeMethod))).isEqualTo(3401);
        assertThat(resCode(update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(incomeMethod)))).isEqualTo(3401);
    }

    @Test
    @DisplayName("#28 남의 수단이면 3003 이다 — 존재를 감춘다")
    void othersPaymentMethodIs3003() throws Exception {
        Fixture other = prepare();
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        // 용도 불일치(3401)와 다르다. 남의 것은 "그 수단을 쓸 수 없다"이고 그 코드가 3003 이다.
        assertThat(resCode(update(fixture, 2026, 11,
                "{\"paymentMethodId\":%d}".formatted(other.paymentMethodId())))).isEqualTo(3003);
    }

    @Test
    @DisplayName("금액이 0 이하면 3401 이다")
    void nonPositiveAmountIs3401() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(update(fixture, 2026, 11, "{\"amount\":0}"))).isEqualTo(3401);
        assertThat(resCode(update(fixture, 2026, 11, "{\"amount\":-1000}"))).isEqualTo(3401);
    }

    @Test
    @DisplayName("남의 설정이면 3402 다 — 소유자 판정이 가장 먼저다")
    void othersSettingIs3402() throws Exception {
        Fixture owner = prepare();
        listMonthly(owner, 2026, 11);
        Fixture stranger = prepare();

        // 값도 잘못됐지만 3401 이 아니라 3402 다. 순서가 뒤집히면 남의 설정이 실재함이
        // 코드 차이로 새어 나간다.
        assertThat(resCode(patchJson(updateUrl(owner.fixedExpenseId(), 2026, 11),
                stranger.token(), "{\"amount\":0}"))).isEqualTo(3402);
    }

    @Test
    @DisplayName("없는 설정 ID 도 같은 3402 다")
    void missingSettingIs3402() throws Exception {
        Fixture fixture = prepare();

        assertThat(resCode(patchJson(updateUrl(999999999L, 2026, 11), fixture.token(), """
                {"amount":300000}
                """))).isEqualTo(3402);
    }

    @Test
    @DisplayName("Path 의 연·월이 범위 밖이면 3403 이다 — 3405 보다 먼저다")
    void invalidPathYearMonthIs3403() throws Exception {
        Fixture fixture = prepare();

        // 그 달 행이 없기도 하지만 범위 판정이 먼저라 3405 가 아니라 3403 이다.
        assertThat(resCode(update(fixture, 2026, 13, """
                {"amount":300000}
                """))).isEqualTo(3403);
        assertThat(resCode(update(fixture, 2026, 0, """
                {"amount":300000}
                """))).isEqualTo(3403);
        assertThat(resCode(update(fixture, 20026, 11, """
                {"amount":300000}
                """))).isEqualTo(3403);
    }

    @Test
    @DisplayName("토큰 없이 부르면 1001 이다")
    void withoutTokenIs1001() throws Exception {
        Fixture fixture = prepare();
        listMonthly(fixture, 2026, 11);

        assertThat(resCode(patchJson(updateUrl(fixture.fixedExpenseId(), 2026, 11), null, """
                {"amount":300000}
                """))).isEqualTo(1001);
    }
}

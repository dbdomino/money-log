package com.dbdomino.moneylog.front.ledger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;

/**
 * 가계부 시험 여덟 개가 함께 쓰는 기본 세움.
 *
 * <p>화면 하나에 백엔드 호출이 최대 넷이다 — 목록 · 대상 조회 · 수단 목록 · 지출유형 목록.
 * 시험마다 이 넷을 세우면 <b>어느 시험이 무엇을 보는지가 세움에 묻힌다.</b> 기본값을 여기
 * 모으고, 각 시험은 자기가 보는 것만 다시 세운다.
 */
final class LedgerTestSupport {

    private LedgerTestSupport() {
    }

    /** 로그인 판정과 선택 목록 둘을 세운다. 목록은 부르는 쪽이 정한다. */
    static void stubCommon(BackendApiClient client) {
        when(client.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        when(client.get(eq(LedgerPageModel.ACTIVE_PAYMENT_PATH),
                eq(PaymentMethodListResult.class), eq(LedgerPageModel.PURPOSE_EXPENSE)))
                .thenReturn(LedgerFixture.expensePaymentMethods());
        when(client.get(eq(LedgerPageModel.ACTIVE_PAYMENT_PATH),
                eq(PaymentMethodListResult.class), eq(LedgerPageModel.PURPOSE_INCOME)))
                .thenReturn(LedgerFixture.incomePaymentMethods());
        when(client.getByQuery(eq(LedgerPageModel.ACTIVE_EXPEND_GROUP_PATH), any(),
                eq(ExpendGroupListResult.class)))
                .thenReturn(LedgerFixture.expendGroups());
    }

    /** 월별 목록 응답을 세운다. */
    static void stubMonth(BackendApiClient client, LedgerMonth month) {
        when(client.getByQuery(eq(LedgerPageModel.LEDGER_PATH), any(), eq(LedgerMonth.class)))
                .thenReturn(month);
    }

    /** 네 종류가 섞인 기본 한 달. */
    static void stubDefaultMonth(BackendApiClient client) {
        stubMonth(client, LedgerFixture.month());
    }

    /** 표 본문만 잘라낸다. 도구줄·모달의 문자열이 섞이지 않게 한다. */
    static String tableBody(String html) {
        int start = html.indexOf("<tbody>");
        int end = html.indexOf("</tbody>", start);
        return start < 0 || end < 0 ? "" : html.substring(start, end);
    }
}

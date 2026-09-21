package com.dbdomino.moneylog.front.fixedexpense;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;

/**
 * 고정지출 시험이 함께 쓰는 기본 세움.
 *
 * <p>화면 하나에 백엔드 호출이 최대 넷이다 — 목록 · 대상 조회 · 수단 목록 · 지출유형 목록.
 * 시험마다 이 넷을 세우면 <b>어느 시험이 무엇을 보는지가 세움에 묻힌다.</b> 기본값을 여기
 * 모으고, 각 시험은 자기가 보는 것만 다시 세운다. 010 에서 같은 정리를 했다.
 */
final class FixedExpenseTestSupport {

    private FixedExpenseTestSupport() {
    }

    /** 로그인 판정과 선택 목록 둘을 세운다. 목록은 부르는 쪽이 정한다. */
    static void stubCommon(BackendApiClient client) {
        when(client.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        when(client.get(eq(FixedExpensePageModel.ACTIVE_PAYMENT_PATH),
                eq(PaymentMethodListResult.class), eq(FixedExpensePageModel.PURPOSE_EXPENSE)))
                .thenReturn(FixedExpenseFixture.activePaymentMethods());
        when(client.getByQuery(eq(FixedExpensePageModel.ACTIVE_EXPEND_GROUP_PATH), any(),
                eq(ExpendGroupListResult.class)))
                .thenReturn(FixedExpenseFixture.activeExpendGroups());
    }

    /** 설정 목록 응답을 세운다. */
    static void stubList(BackendApiClient client, FixedExpenseListResult result) {
        when(client.getByQuery(eq(FixedExpensePageModel.LIST_PATH), any(),
                eq(FixedExpenseListResult.class)))
                .thenReturn(result);
    }

    /** 첫 쪽 기본 자료. */
    static void stubFirstPage(BackendApiClient client) {
        stubList(client, FixedExpenseFixture.firstPage());
    }

    /** 표 본문만 잘라낸다. 도구줄·모달의 문자열이 섞이지 않게 한다. */
    static String tableBody(String html) {
        int start = html.indexOf("<tbody>");
        int end = html.indexOf("</tbody>", start);
        return start < 0 || end < 0 ? "" : html.substring(start, end);
    }

    /** 표 본문에서 그 문구를 담은 행 하나만 잘라낸다. */
    static String rowContaining(String tableBody, String text) {
        int at = tableBody.indexOf(text);
        if (at < 0) {
            return "";
        }
        int start = tableBody.lastIndexOf("<tr", at);
        int end = tableBody.indexOf("</tr>", at);
        return tableBody.substring(start, end);
    }

    /**
     * 모달 본문만 잘라낸다.
     *
     * <p><b>둘 중 먼저 오는 것에서 끊는다.</b> 폼의 끝을 먼저 찾으면 폼이 없는 상세 본문이
     * <b>뒤에 있는 수정 폼의 끝</b>까지 딸려 와, 「상세에는 입력 칸이 없다」를 보는 시험이
     * 수정 폼을 읽게 된다.
     */
    static String modalBody(String html, String bodyClass) {
        int start = html.indexOf("class=\"" + bodyClass + "\"");
        if (start < 0) {
            return "";
        }
        int end = firstOf(html, start, "</form>", "</div></div>");
        return end < 0 ? html.substring(start) : html.substring(start, end);
    }

    /** {@code start} 뒤에서 가장 먼저 나오는 조각의 자리. 없으면 -1. */
    private static int firstOf(String html, int start, String... marks) {
        int found = -1;
        for (String mark : marks) {
            int at = html.indexOf(mark, start);
            if (at >= 0 && (found < 0 || at < found)) {
                found = at;
            }
        }
        return found;
    }
}

package com.dbdomino.moneylog.front.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupResponse;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodView;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 수단 목록과 지출유형 목록이 <b>같은 규칙으로</b> 상태를 보이는가.
 *
 * <p>두 화면을 각자 시험하면 <b>둘이 같은 말을 쓰는지</b>를 아무도 보지 않게 된다. 화면마다
 * 다른 말을 쓰면 사용자가 같은 상태를 다른 것으로 읽고, 되돌릴 수 있는 것과 없는 것을
 * 구분하지 못한다.
 *
 * <p>이것이 US3 의 일이며, 두 화면이 서야 비교할 수 있어 US1·US2 뒤에 온다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListStatusConsistencyTest {

    private static final String PAYMENTS_URL = "/payments";

    /** 화면 주소와 백엔드 경로가 다르다. 수단만 그렇고 지출유형은 같다. */
    private static final String PAYMENTS_PATH = "/payment-methods";
    private static final String EXPEND_GROUPS_URL = "/expend-groups";
    private static final String EXPEND_GROUPS_PATH = "/expend-groups";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));

        // 두 목록에 같은 성격의 행을 담는다 — 사용 중 · 사용 안 함 · 삭제됨.
        when(backendApiClient.getByQuery(eq(PAYMENTS_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenReturn(new PaymentMethodListResult(List.of(
                        new PaymentMethodView(1L, "쓰는카드", PaymentMethodView.TYPE_CARD,
                                PaymentMethodView.PURPOSE_EXPENSE, true, "2027-05", false),
                        new PaymentMethodView(2L, "안쓰는통장", PaymentMethodView.TYPE_ACCOUNT,
                                PaymentMethodView.PURPOSE_INCOME, false, null, false),
                        new PaymentMethodView(3L, "지운카드", PaymentMethodView.TYPE_CARD,
                                PaymentMethodView.PURPOSE_EXPENSE, true, null, true))));

        when(backendApiClient.getByQuery(eq(EXPEND_GROUPS_PATH), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(new ExpendGroupListResult(List.of(
                        new ExpendGroupResponse(1L, "쓰는유형", true, null, false, false),
                        new ExpendGroupResponse(2L, "안쓰는유형", false, null, false, false),
                        new ExpendGroupResponse(3L, "지운유형", true, null, false, true))));
    }

    @Test
    @DisplayName("두 화면이 사용 여부를 같은 말로 보인다")
    void 사용_여부가_같은_말이다() throws Exception {
        String payments = render(PAYMENTS_URL);
        String groups = render(EXPEND_GROUPS_URL);

        for (String html : List.of(payments, groups)) {
            assertThat(html).contains("사용 안 함");
            assertThat(html).contains(">사용<");
        }
    }

    @Test
    @DisplayName("두 화면이 삭제 여부를 같은 말로 보인다")
    void 삭제_여부가_같은_말이다() throws Exception {
        String payments = render(PAYMENTS_URL);
        String groups = render(EXPEND_GROUPS_URL);

        for (String html : List.of(payments, groups)) {
            assertThat(html).contains("삭제됨");
            assertThat(html).contains("정상");
        }
    }

    @Test
    @DisplayName("사용 열과 상태 열이 서로 다른 열이다")
    void 사용과_상태가_다른_열이다() throws Exception {
        // 둘 다 "지금 고를 수 없다"로 보이지만 사용 안 함은 되돌릴 수 있고 삭제됨은 없다.
        // 한 열에 합치면 사용자가 그 차이를 알 수 없고, 되돌릴 수 없는 일을 가볍게 누른다.
        for (String url : List.of(PAYMENTS_URL, EXPEND_GROUPS_URL)) {
            String html = render(url);
            int headStart = html.indexOf("<thead>");
            int headEnd = html.indexOf("</thead>", headStart);
            String head = html.substring(headStart, headEnd);

            assertThat(head).contains("<th>사용</th>");
            assertThat(head).contains("<th>상태</th>");
        }
    }

    @Test
    @DisplayName("두 화면 모두 삭제된 행에 수정·삭제가 없다")
    void 삭제된_행에는_되돌릴_수_없는_동작이_없다() throws Exception {
        String payments = render(PAYMENTS_URL);
        assertThat(payments).doesNotContain("m=edit&amp;id=3");
        assertThat(payments).doesNotContain("confirm-payment-delete-3");

        String groups = render(EXPEND_GROUPS_URL);
        assertThat(groups).doesNotContain("m=edit&amp;id=3");
        assertThat(groups).doesNotContain("confirm-expend-group-delete-3");

        // 상세는 두 화면 모두 남긴다 — 지운 것이 어떤 값이었는지 확인할 자리는 있어야 한다.
        assertThat(payments).contains("m=detail&amp;id=3");
        assertThat(groups).contains("m=detail&amp;id=3");
    }

    @Test
    @DisplayName("두 화면이 같은 상태 조각을 쓴다")
    void 같은_조각을_쓴다() throws Exception {
        // 조각을 나눠 쓰면 한쪽 문구를 고쳤을 때 다른 쪽이 따라오지 않는다.
        for (String url : List.of(PAYMENTS_URL, EXPEND_GROUPS_URL)) {
            String html = render(url);
            assertThat(html).contains("badge-success");
            assertThat(html).contains("badge-danger");
            assertThat(html).contains("badge-warning");
        }
    }

    private String render(String url) throws Exception {
        return mockMvc.perform(get(url).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();
    }
}

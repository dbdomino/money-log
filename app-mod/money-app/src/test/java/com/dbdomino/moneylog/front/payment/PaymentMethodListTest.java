package com.dbdomino.moneylog.front.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 2.1 수단 목록.
 *
 * <p>핵심은 <b>삭제 표시된 수단이 목록에 남는다</b>는 것이다. 감추면 "행이 남는다"는 계약이
 * 화면에서 사라지고, 사용자는 지웠다고 믿는데 이름은 과거 내역에 계속 살아 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodListTest {

    private static final String LIST_URL = "/payments";
    private static final String LIST_PATH = "/payment-methods";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenReturn(PaymentMethodFixture.page());
    }

    @Test
    @DisplayName("목록을 열면 행이 그려지고 구분·용도가 말로 보인다")
    void 구분과_용도가_말로_보인다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("국민카드").contains("묵은통장");
        assertThat(html).contains("카드").contains("계좌");
        assertThat(html).contains("지출용").contains("소득용");

        // CARD·EXPENSE 는 화면 밖의 값이다. 표에 그대로 나오면 안 된다.
        int tableStart = html.indexOf("<tbody>");
        int tableEnd = html.indexOf("</tbody>", tableStart);
        assertThat(html.substring(tableStart, tableEnd))
                .doesNotContain("CARD")
                .doesNotContain("EXPENSE");
    }

    @Test
    @DisplayName("목록에 유효기간 열이 없다")
    void 유효기간_열이_없다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 계좌에는 그 값이 없어 절반이 빈 칸이 되고, 빈 칸은 "유효기간을 모르는 카드"로 읽힌다.
        int headStart = html.indexOf("<thead>");
        int headEnd = html.indexOf("</thead>", headStart);
        assertThat(html.substring(headStart, headEnd)).doesNotContain("유효기간");
    }

    @Test
    @DisplayName("삭제 표시된 수단이 목록에 남고 상태 열에 보인다")
    void 삭제된_수단이_목록에_남는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .as("감추면 행이 남는다는 계약이 화면에서 사라진다")
                .contains("없앤카드");
        assertThat(html).contains("삭제됨");
        assertThat(html).contains("사용 안 함");
    }

    @Test
    @DisplayName("삭제된 행에는 수정·삭제 버튼이 없고 상세만 있다")
    void 삭제된_행에는_상세만_있다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 다시 삭제하면 거절되고, 고쳐도 선택 목록에 돌아오지 않는다.
        assertThat(html).contains("m=edit&amp;id=1");
        assertThat(html).doesNotContain("m=edit&amp;id=3");
        assertThat(html).contains("confirm-payment-delete-1");
        assertThat(html).doesNotContain("confirm-payment-delete-3");

        // 상세는 남긴다 — 삭제된 수단이 어떤 값이었는지 확인할 자리는 있어야 한다.
        assertThat(html).contains("m=detail&amp;id=3");
    }

    @Test
    @DisplayName("목록 요청에 조회 구간이 실리지 않는다")
    void 조회_구간을_싣지_않는다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk());

        // 백엔드가 받지 않는 값을 실어 보낼 이유가 없다. 010~012 의 가계부·고정지출 목록은
        // 다르다 — 그쪽은 쪽 넘기기가 있어 007 의 환산기를 써야 한다.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq(LIST_PATH), query.capture(),
                eq(PaymentMethodListResult.class));
        assertThat(query.getValue()).doesNotContainKeys("offset", "limit");
    }

    @Test
    @DisplayName("응답이 비어 와도 빈 목록으로 그린다")
    void 응답이_비어도_빈_목록으로_그린다() throws Exception {
        when(backendApiClient.getByQuery(eq(LIST_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenReturn(null);

        // 여기서 터지면 사용자는 수단 관리가 통째로 죽었다고 읽는다.
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"));
    }

    @Test
    @DisplayName("미로그인으로 열면 로그인 화면으로 간다")
    void 미로그인은_막힌다() throws Exception {
        mockMvc.perform(get(LIST_URL)).andExpect(status().isFound());
    }
}

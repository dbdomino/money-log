package com.dbdomino.moneylog.front.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.util.List;
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
 * 009 의 두 목록에는 <b>쪽 넘기기가 없다</b>.
 *
 * <p>백엔드가 조회 구간을 받지 않고 본인 것을 전부 돌려준다. 받지 않는 값을 실어 보낼 이유가
 * 없어 007 의 페이징 환산기를 쓰지 않는다.
 *
 * <h2>010~012 는 다르다</h2>
 *
 * <p><b>목록이라고 다 같지 않다.</b> 가계부·고정지출 목록에는 쪽 넘기기가 있고, 그쪽은
 * 007 의 환산기를 써야 한다 — 화면이 조회 시작점을 직접 계산하면 개수의 배수가 아닌 값이
 * 새어 나가 목록이 통째로 실패한다. 이 시험을 "목록에는 페이징이 없다"는 일반 규칙으로
 * 읽으면 그 화면들이 잘못 만들어진다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListPagingAbsenceTest {

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
        when(backendApiClient.getByQuery(eq(PAYMENTS_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenReturn(new PaymentMethodListResult(List.of()));
        when(backendApiClient.getByQuery(eq(EXPEND_GROUPS_PATH), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(new ExpendGroupListResult(List.of()));
    }

    @Test
    @DisplayName("수단 목록 요청에 조회 구간이 실리지 않는다")
    void 수단_목록은_조회_구간을_싣지_않는다() throws Exception {
        mockMvc.perform(get(PAYMENTS_URL).session(LoggedInSessions.member()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq(PAYMENTS_PATH), query.capture(),
                eq(PaymentMethodListResult.class));
        assertThat(query.getValue()).isEmpty();
    }

    @Test
    @DisplayName("지출유형 목록 요청에 조회 구간이 실리지 않는다")
    void 지출유형_목록은_조회_구간을_싣지_않는다() throws Exception {
        mockMvc.perform(get(EXPEND_GROUPS_URL).session(LoggedInSessions.member()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> query = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).getByQuery(eq(EXPEND_GROUPS_PATH), query.capture(),
                eq(ExpendGroupListResult.class));
        assertThat(query.getValue()).isEmpty();
    }

    @Test
    @DisplayName("두 화면에 쪽 이동 장치가 없다")
    void 쪽_이동_장치가_없다() throws Exception {
        for (String url : List.of(PAYMENTS_URL, EXPEND_GROUPS_URL)) {
            String html = mockMvc.perform(get(url).session(LoggedInSessions.member()))
                    .andReturn().getResponse().getContentAsString();

            assertThat(html).doesNotContain("class=\"pagination\"");
            assertThat(html).doesNotContain("page=");
        }
    }
}

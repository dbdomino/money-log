package com.dbdomino.moneylog.front.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendUnavailableException;
import com.dbdomino.moneylog.front.expendgroup.ExpendGroupListResult;
import com.dbdomino.moneylog.front.payment.PaymentMethodListResult;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 백엔드에 닿지 못하면 <b>오류 화면으로</b> 간다.
 *
 * <p>폼 실패와 통신 실패는 착지가 반대다 — 폼 실패는 폼으로 돌아오고 통신 실패는 오류
 * 화면으로 간다. 사용자가 할 수 있는 일이 다르기 때문이다. 폼 실패는 값을 고쳐 다시
 * 시도하면 되지만 통신 실패는 기다리는 것 말고 할 일이 없다.
 *
 * <p>이 경계가 흐려지면 <b>백엔드가 죽었는데 빈 목록이 정상처럼 뜬다.</b> 사용자는 자기
 * 데이터가 없어졌다고 읽는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ListBackendFailureTest {

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
    }

    @Test
    @DisplayName("수단 목록이 백엔드에 닿지 못하면 오류 화면으로 간다")
    void 수단_목록_불통은_오류_화면이다() throws Exception {
        when(backendApiClient.getByQuery(eq(PAYMENTS_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenThrow(new BackendUnavailableException("백엔드에 닿지 못했습니다."));

        String html = mockMvc.perform(get(PAYMENTS_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andReturn().getResponse().getContentAsString();

        // 빈 목록으로 그리면 "가져오지 못한 목록"과 "정말 비어 있는 목록"이 같아 보인다.
        assertThat(html).doesNotContain("등록한 수단이 없습니다");
    }

    @Test
    @DisplayName("지출유형 목록이 백엔드에 닿지 못하면 오류 화면으로 간다")
    void 지출유형_목록_불통은_오류_화면이다() throws Exception {
        when(backendApiClient.getByQuery(eq(EXPEND_GROUPS_PATH), any(), eq(ExpendGroupListResult.class)))
                .thenThrow(new BackendUnavailableException("백엔드에 닿지 못했습니다."));

        String html = mockMvc.perform(get(EXPEND_GROUPS_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("등록한 지출유형이 없습니다");
    }

    @Test
    @DisplayName("응답이 비어 온 것은 불통과 다르다 — 빈 목록으로 그린다")
    void 빈_응답은_빈_목록이다() throws Exception {
        when(backendApiClient.getByQuery(eq(PAYMENTS_PATH), any(), eq(PaymentMethodListResult.class)))
                .thenReturn(null);

        // 백엔드가 답은 했다. 목록이 없는 것이지 화면이 고장 난 것이 아니다.
        mockMvc.perform(get(PAYMENTS_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"));
    }
}

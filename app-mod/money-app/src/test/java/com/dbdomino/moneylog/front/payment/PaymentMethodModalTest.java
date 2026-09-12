package com.dbdomino.moneylog.front.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.web.ModalParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 2.2 등록 · 2.3 상세 · 2.4 수정 모달.
 *
 * <p><b>목록 하나에 모달 셋</b>이라는 모양을 처음 검증하는 자리다. 지출유형이 이것을 그대로
 * 따르고 010~012 가 다시 따른다.
 *
 * <p>모르는 식별자로 들어왔을 때 <b>오류 화면이 아니라 목록이 정상으로</b> 뜨는 것이 중요하다.
 * 낡은 북마크일 뿐이고 목록 자체는 멀쩡하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodModalTest {

    private static final String LIST_URL = "/payments";
    private static final String LIST_PATH = "/payment-methods";
    private static final String ITEM_PATH = "/payment-methods/{paymentMethodId}";

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
    @DisplayName("m=create 로 들어오면 등록 모달이 열린 채 목록이 뜬다")
    void 등록_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()).param("m", "create"))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"));

        // 새로 만드는 화면이라 미리 가져올 값이 없다.
        verify(backendApiClient, never()).get(eq(ITEM_PATH), eq(PaymentMethodView.class), any());
    }

    @Test
    @DisplayName("m=detail 로 들어오면 값이 채워진 채 열리고 입력 칸이 없다")
    void 상세_모달은_읽기_전용이다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(PaymentMethodView.class), eq(1L)))
                .thenReturn(PaymentMethodFixture.card());

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "detail").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "detail"))
                .andReturn().getResponse().getContentAsString();

        // 값은 글로 보이고, 목록에 없는 유효기간을 여기서 확인할 수 있다.
        int bodyStart = html.indexOf("payment-detail-body");
        int bodyEnd = html.indexOf("payment-edit-body", bodyStart);
        String detail = html.substring(bodyStart, bodyEnd);
        assertThat(detail).contains("국민카드").contains("2027-05");
        assertThat(detail)
                .as("한 모달에서 읽기와 고치기를 겸하면 지금 고칠 수 있는 상태인지 매번 판단해야 한다")
                .doesNotContain("<input");
    }

    @Test
    @DisplayName("m=edit 로 들어오면 그 수단의 값이 채워진 수정 폼이 열린다")
    void 수정_모달이_값을_채운_채_열린다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(PaymentMethodView.class), eq(1L)))
                .thenReturn(PaymentMethodFixture.card());

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "edit").param("id", "1"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andReturn().getResponse().getContentAsString();

        // 여는 일만 브라우저가 하면 열린 모달이 빈 채로 뜬다.
        assertThat(html).contains("value=\"국민카드\"");
        assertThat(html).contains("value=\"2027-05\"");
    }

    @Test
    @DisplayName("모르는 식별자면 오류 화면이 아니라 목록이 정상으로 뜬다")
    void 모르는_식별자는_목록만_보인다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(PaymentMethodView.class), eq(99L)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_NOT_FOUND.code(),
                        ErrorCode.PAYMENT_METHOD_NOT_FOUND.message()));

        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "edit").param("id", "99"))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attribute("notice",
                        ErrorCode.PAYMENT_METHOD_NOT_FOUND.message()));
    }

    @Test
    @DisplayName("식별자 없이 m=edit 면 모달 없이 목록만 보인다")
    void 식별자가_없으면_모달을_열지_않는다() throws Exception {
        // 누구를 고칠지 모르는 상태다.
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()).param("m", "edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));

        verify(backendApiClient, never()).get(eq(ITEM_PATH), eq(PaymentMethodView.class), any());
    }

    @Test
    @DisplayName("모르는 모달 값은 오류가 아니라 부모 목록이다")
    void 모르는_모달_값은_목록이다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()).param("m", "nonsense"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));
    }

    @Test
    @DisplayName("등록이 실패하면 모달을 연 채로 목록이 다시 뜬다")
    void 등록_실패가_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.post(eq(LIST_PATH), any(), eq(PaymentMethodView.class)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_TYPE_INVALID.code(),
                        ErrorCode.PAYMENT_METHOD_TYPE_INVALID.message()));

        String html = mockMvc.perform(post(LIST_URL).session(LoggedInSessions.member())
                        .param("name", "새카드")
                        .param("type", "CARD")
                        .param("purpose", "EXPENSE")
                        .param("inUse", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"))
                .andReturn().getResponse().getContentAsString();

        // 모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다.
        assertThat(html).contains(ErrorCode.PAYMENT_METHOD_TYPE_INVALID.message());
        assertThat(html).contains("value=\"새카드\"");
    }

    @Test
    @DisplayName("수정이 실패하면 그 수단의 모달을 연 채로 목록이 다시 뜬다")
    void 수정_실패가_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(PaymentMethodView.class), eq(1L)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_EXPIRY_INVALID.code(),
                        ErrorCode.PAYMENT_METHOD_EXPIRY_INVALID.message()));

        mockMvc.perform(post("/payments/1").session(LoggedInSessions.member())
                        .param("name", "국민카드").param("type", "CARD")
                        .param("purpose", "EXPENSE").param("inUse", "true")
                        .param("cardExpiry", "20-27"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andExpect(model().attribute("targetId", "1"));
    }
}

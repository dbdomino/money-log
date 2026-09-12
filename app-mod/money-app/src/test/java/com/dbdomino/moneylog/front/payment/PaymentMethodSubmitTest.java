package com.dbdomino.moneylog.front.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.payment.form.PaymentMethodForm;
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
 * 수단 등록·수정·삭제가 백엔드로 무엇을 보내는가.
 *
 * <p>두 가지가 핵심이다. <b>계좌면 유효기간을 보내지 않는 것</b>은 브라우저가 칸을 접어도
 * 서버가 따로 보장해야 하고, <b>용도를 바꿀 수 없다는 거절 뒤에 고른 값이 남는 것</b>은
 * 되돌리면 사용자가 자기가 무엇을 바꿨는지 모른 채 저장을 반복하기 때문이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentMethodSubmitTest {

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
    @DisplayName("구분이 계좌면 유효기간이 요청에 실리지 않는다")
    void 계좌는_유효기간을_보내지_않는다() throws Exception {
        when(backendApiClient.post(eq(LIST_PATH), any(), eq(PaymentMethodView.class)))
                .thenReturn(PaymentMethodFixture.unusedAccount());

        // 브라우저가 칸을 접어도 스크립트가 막힌 환경에서는 값이 그대로 나간다. 보장은 서버다.
        mockMvc.perform(post(LIST_URL).session(LoggedInSessions.member())
                        .param("name", "월급통장")
                        .param("type", "ACCOUNT")
                        .param("purpose", "INCOME")
                        .param("inUse", "true")
                        .param("cardExpiry", "2027-05"))
                .andExpect(status().isOk());

        assertThat(capturedCreateRequest().cardExpiry()).isNull();
    }

    @Test
    @DisplayName("구분이 카드면 유효기간이 실린다")
    void 카드는_유효기간을_보낸다() throws Exception {
        when(backendApiClient.post(eq(LIST_PATH), any(), eq(PaymentMethodView.class)))
                .thenReturn(PaymentMethodFixture.card());

        mockMvc.perform(post(LIST_URL).session(LoggedInSessions.member())
                        .param("name", "국민카드")
                        .param("type", "CARD")
                        .param("purpose", "EXPENSE")
                        .param("inUse", "true")
                        .param("cardExpiry", "2027-05"))
                .andExpect(status().isOk());

        assertThat(capturedCreateRequest().cardExpiry()).isEqualTo("2027-05");
    }

    @Test
    @DisplayName("용도를 비우고 저장하면 저장되지 않는다")
    void 용도가_없으면_거절된다() throws Exception {
        when(backendApiClient.post(eq(LIST_PATH), any(), eq(PaymentMethodView.class)))
                .thenThrow(new BackendApiException(ErrorCode.BAD_REQUEST.code(),
                        ErrorCode.BAD_REQUEST.message()));

        String html = mockMvc.perform(post(LIST_URL).session(LoggedInSessions.member())
                        .param("name", "국민카드")
                        .param("type", "CARD")
                        .param("inUse", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.BAD_REQUEST.message());
    }

    @Test
    @DisplayName("용도를 바꿀 수 없다는 거절이 용도 칸 가까이 붙고 고른 값이 그대로 남는다")
    void 용도_잠김이_칸에_붙고_고른_값이_남는다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(PaymentMethodView.class), eq(1L)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_PURPOSE_LOCKED.code(),
                        ErrorCode.PAYMENT_METHOD_PURPOSE_LOCKED.message()));

        String html = mockMvc.perform(post("/payments/1").session(LoggedInSessions.member())
                        .param("name", "국민카드").param("type", "CARD")
                        .param("purpose", "INCOME").param("inUse", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 안내가 용도 칸과 사용 여부 칸 사이에 있어야 그 칸에 붙은 것이다.
        int purposeField = html.indexOf("id=\"edit-purpose\"");
        int inUseField = html.indexOf("id=\"edit-inUse\"");
        assertThat(purposeField).isGreaterThan(0);
        assertThat(html.substring(purposeField, inUseField))
                .contains(ErrorCode.PAYMENT_METHOD_PURPOSE_LOCKED.message());

        // 되돌리면 사용자는 자기가 무엇을 바꿨는지 모른 채 저장을 반복한다.
        int editForm = html.indexOf("payment-edit-body");
        assertThat(html.substring(editForm))
                .as("사용자가 고른 용도가 그대로 남아야 원래대로 돌릴 수 있다")
                .contains("value=\"INCOME\" selected");
    }

    @Test
    @DisplayName("수정에서 계좌로 바꾸면 유효기간을 비우라는 값으로 보낸다")
    void 계좌로_바꾸면_유효기간을_비운다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(PaymentMethodView.class), eq(1L)))
                .thenReturn(PaymentMethodFixture.unusedAccount());

        // 싣지 않으면 카드에서 계좌로 바꿨을 때 옛 유효기간이 그대로 남는다.
        mockMvc.perform(post("/payments/1").session(LoggedInSessions.member())
                        .param("name", "바뀐통장").param("type", "ACCOUNT")
                        .param("purpose", "INCOME").param("inUse", "true")
                        .param("cardExpiry", "2027-05"))
                .andExpect(status().isOk());

        assertThat(capturedUpdateBody()).containsEntry("cardExpiry", null);
    }

    @Test
    @DisplayName("삭제가 POST 로 나가고 이미 삭제된 수단은 목록 안내로 보인다")
    void 삭제는_POST_이고_재삭제는_목록_안내다() throws Exception {
        mockMvc.perform(post("/payments/1/delete").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"));

        verify(backendApiClient).delete(eq(ITEM_PATH), eq(1L));

        org.mockito.Mockito.doThrow(new BackendApiException(
                        ErrorCode.PAYMENT_METHOD_ALREADY_DELETED.code(),
                        ErrorCode.PAYMENT_METHOD_ALREADY_DELETED.message()))
                .when(backendApiClient).delete(eq(ITEM_PATH), eq(3L));

        // 이미 원하는 상태라 사용자가 할 일이 없다. 오류 화면으로 보내면 걸음만 늘어난다.
        String html = mockMvc.perform(post("/payments/3/delete").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.PAYMENT_METHOD_ALREADY_DELETED.message());
        assertThat(html).contains("국민카드");
    }

    @Test
    @DisplayName("모달에 유효기간 칸을 접고 펴는 장치가 들어 있다")
    void 유효기간_칸이_구분에_따라_접힌다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "create"))
                .andReturn().getResponse().getContentAsString();

        // 표시는 브라우저가 맡는다. 보장은 서버가 하므로 이 스크립트가 막혀도 저장은 돈다.
        assertThat(html).contains("data-expiry-toggle");
        assertThat(html).contains("data-expiry-field");
        assertThat(html).contains("data-expiry-form");
    }

    private PaymentMethodForm.Request capturedCreateRequest() {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).post(eq(LIST_PATH), body.capture(), eq(PaymentMethodView.class));
        return (PaymentMethodForm.Request) body.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedUpdateBody() {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).patch(eq(ITEM_PATH), body.capture(), eq(PaymentMethodView.class),
                eq(1L));
        return (Map<String, Object>) body.getValue();
    }
}

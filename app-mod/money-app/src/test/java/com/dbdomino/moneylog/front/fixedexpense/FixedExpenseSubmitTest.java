package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.support.FormFailure;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import com.dbdomino.moneylog.front.web.ModalParam;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 4.3 등록 · 4.5 수정의 제출.
 *
 * <p>핵심은 <b>적용 기간이 정수 네 칸으로 나간다</b>는 것이다(FR-1002 · SC-1007). 010 의
 * 할부가 문자열 한 칸이라 <b>같은 저장소 안에서 형식이 갈리고</b>, 한쪽 규칙을 옮기면
 * 등록이 통째로 막힌다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FixedExpenseSubmitTest {

    private static final String CREATE_URL = "/fixed-expenses";
    private static final String CREATE_PATH = "/fixed-expenses";
    private static final String ITEM_PATH = "/fixed-expenses/{fixedExpenseId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        FixedExpenseTestSupport.stubCommon(backendApiClient);
        FixedExpenseTestSupport.stubFirstPage(backendApiClient);
    }

    @Test
    @DisplayName("등록이 적용 기간을 정수 네 칸으로 싣는다 (FR-1002·SC-1007)")
    void 적용_기간이_정수_네_칸이다() throws Exception {
        mockMvc.perform(filled(post(CREATE_URL))).andExpect(status().isOk());

        Map<String, Object> body = capturedCreateBody();
        assertThat(body)
                .containsEntry("startYear", 2026)
                .containsEntry("startMonth", 1)
                .containsEntry("endYear", 2026)
                .containsEntry("endMonth", 12);
    }

    @Test
    @DisplayName("문자열 YYYY-MM 이 실리지 않는다 — 010 의 할부 형식이 새어 들어오지 않는다")
    void 문자열_형식이_실리지_않는다() throws Exception {
        mockMvc.perform(filled(post(CREATE_URL))).andExpect(status().isOk());

        Map<String, Object> body = capturedCreateBody();
        assertThat(body).doesNotContainKeys("startYearMonth", "endYearMonth", "period");

        // 네 칸이 전부 숫자다. 문자열로 합쳐 보내면 백엔드가 형식 오류로 거절한다.
        assertThat(body.get("startYear")).isInstanceOf(Integer.class);
        assertThat(body.get("startMonth")).isInstanceOf(Integer.class);
        assertThat(body.get("endYear")).isInstanceOf(Integer.class);
        assertThat(body.get("endMonth")).isInstanceOf(Integer.class);
    }

    @Test
    @DisplayName("열 칸이 전부 실린다 (FR-1001)")
    void 열_칸이_전부_실린다() throws Exception {
        mockMvc.perform(filled(post(CREATE_URL))).andExpect(status().isOk());

        assertThat(capturedCreateBody()).containsOnlyKeys(
                "name", "paymentMethodId", "expendGroupId", "amount", "paymentDayOfMonth",
                "content", "startYear", "startMonth", "endYear", "endMonth");
    }

    @Test
    @DisplayName("수정도 적용 기간을 네 칸으로 싣는다")
    void 수정도_네_칸이다() throws Exception {
        mockMvc.perform(filled(post("/fixed-expenses/1"))).andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).patch(eq(ITEM_PATH), captor.capture(), eq(Void.class), eq(1L));

        assertThat(captor.getValue())
                .containsEntry("startYear", 2026)
                .containsEntry("endMonth", 12)
                .doesNotContainKey("startYearMonth");
    }

    @Test
    @DisplayName("수단 실패가 수단 칸에, 지출유형 실패가 지출유형 칸에 붙는다")
    void 실패가_칸에_갈라_붙는다() throws Exception {
        when(backendApiClient.post(eq(CREATE_PATH), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.PAYMENT_METHOD_NOT_FOUND.code(),
                        "결제수단을 찾을 수 없습니다."));

        mockMvc.perform(filled(post(CREATE_URL)))
                .andExpect(model().attribute(FormFailure.FIELD, "paymentMethodId"));

        when(backendApiClient.post(eq(CREATE_PATH), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_NOT_FOUND.code(),
                        "지출유형을 찾을 수 없습니다."));

        mockMvc.perform(filled(post(CREATE_URL)))
                .andExpect(model().attribute(FormFailure.FIELD, "expendGroupId"));
    }

    @Test
    @DisplayName("값 오류와 용도 불일치는 폼 상단이고 서버 문구가 그대로 보인다 (FR-1005)")
    void 값_오류는_폼_상단이다() throws Exception {
        // 한 코드가 결제일·금액·기간 오류와 수단 용도 불일치를 겸한다. 코드만으로는 어느
        // 칸인지 가릴 수 없어 짐작해 붙이면 사용자가 맞는 칸을 고치고 또 틀린다.
        when(backendApiClient.post(eq(CREATE_PATH), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID.code(),
                        "지출용 수단을 고르세요."));

        String html = mockMvc.perform(filled(post(CREATE_URL)))
                .andExpect(model().attribute(FormFailure.FIELD, FormFailure.FIELD_FORM))
                .andExpect(model().attribute("failMessage", "지출용 수단을 고르세요."))
                .andReturn().getResponse().getContentAsString();

        // 화면이 「수단이 없다」로 바꿔 적지 않는다 — 수단은 실재하고 목록에도 보인다.
        assertThat(html).contains("지출용 수단을 고르세요.");
    }

    @Test
    @DisplayName("모달 제출이 실패하면 모달을 연 채로 목록이 다시 뜬다")
    void 제출_실패는_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.post(eq(CREATE_PATH), any(), eq(Void.class)))
                .thenThrow(new BackendApiException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID.code(),
                        "결제일은 1~31 이어야 합니다."));

        mockMvc.perform(filled(post(CREATE_URL)))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                // 모달을 닫아 버리면 사용자가 채운 칸이 전부 사라진다.
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"))
                .andExpect(model().attributeExists("fixedExpenses"));
    }

    @Test
    @DisplayName("수정 제출이 실패하면 수정 모달을 연 채로 돌아온다")
    void 수정_실패도_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.patch(eq(ITEM_PATH), any(), eq(Void.class), eq(1L)))
                .thenThrow(new BackendApiException(ErrorCode.FIXED_EXPENSE_FIELD_INVALID.code(),
                        "종료 연월이 시작보다 앞섭니다."));

        mockMvc.perform(filled(post("/fixed-expenses/1")))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andExpect(model().attribute("targetId", "1"));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    /** 열 칸을 모두 채운 제출. */
    private static MockHttpServletRequestBuilder filled(MockHttpServletRequestBuilder request) {
        return request
                .param("name", "월세")
                .param("paymentMethodId", "1")
                .param("expendGroupId", "2")
                .param("amount", "800000")
                .param("paymentDayOfMonth", "5")
                .param("content", "원룸 월세")
                .param("startYear", "2026")
                .param("startMonth", "1")
                .param("endYear", "2026")
                .param("endMonth", "12")
                .session(LoggedInSessions.member());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedCreateBody() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(backendApiClient).post(eq(CREATE_PATH), captor.capture(), eq(Void.class));
        return captor.getValue();
    }
}

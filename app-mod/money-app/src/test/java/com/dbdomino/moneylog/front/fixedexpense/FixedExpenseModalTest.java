package com.dbdomino.moneylog.front.fixedexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
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
 * 4.3 등록 · 4.4 상세 · 4.5 수정 모달이 열리는 방식.
 *
 * <p>핵심은 <b>없는 대상이면 빈 모달을 띄우지 않는다</b>는 것이다. 그리고 <b>없는 것과
 * 남의 것을 가르지 않는다</b> — 가르면 식별자를 훑어 남의 고정지출이 실재하는지 알아낼 수
 * 있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FixedExpenseModalTest {

    private static final String LIST_URL = "/fixed-expenses";
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
    @DisplayName("?m=create 면 등록 모달이 열린 채 목록이 뜬다")
    void 등록_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "create").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("fixed-expenses/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"))
                // 모달은 목록 위에 얹힌다. 부모가 함께 그려져야 한다.
                .andExpect(model().attributeExists("fixedExpenses"));
    }

    @Test
    @DisplayName("?m=detail&id=… 면 그 설정의 값이 읽기 전용으로 보인다")
    void 상세_모달이_읽기_전용이다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(FixedExpenseView.class), eq(1L)))
                .thenReturn(FixedExpenseFixture.monthlyRent());

        String html = mockMvc.perform(get(LIST_URL).param("m", "detail").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "detail"))
                .andExpect(model().attributeExists("target"))
                .andReturn().getResponse().getContentAsString();

        String modal = FixedExpenseTestSupport.modalBody(html, "fixed-detail-body");
        assertThat(modal).contains("월세").contains("2026-01 ~ 2026-12").contains("매달 5일");

        // 읽기 전용이라 입력 칸이 없다.
        assertThat(modal).doesNotContain("name=\"name\"").doesNotContain("name=\"amount\"");

        // 값을 들여다보다 되돌릴 수 없는 동작을 누르는 자리를 만들지 않는다.
        assertThat(modal).doesNotContain("삭제");
    }

    @Test
    @DisplayName("?m=edit&id=… 면 값이 채워진 수정 폼이 뜬다")
    void 수정_모달에_값이_채워진다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(FixedExpenseView.class), eq(1L)))
                .thenReturn(FixedExpenseFixture.monthlyRent());

        String html = mockMvc.perform(get(LIST_URL).param("m", "edit").param("id", "1")
                        .session(LoggedInSessions.member()))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andExpect(model().attributeExists("target"))
                .andReturn().getResponse().getContentAsString();

        String modal = FixedExpenseTestSupport.modalBody(html, "fixed-edit-body");
        assertThat(modal).contains("value=\"월세\"").contains("value=\"800000\"");

        // 적용 기간이 네 칸으로 채워진다 — 한 칸짜리 연월 입력이 아니다.
        assertThat(modal).contains("name=\"startYear\"").contains("name=\"startMonth\"")
                .contains("name=\"endYear\"").contains("name=\"endMonth\"");
        assertThat(modal).doesNotContain("name=\"startYearMonth\"");
    }

    @Test
    @DisplayName("없는 식별자면 빈 모달이 아니라 목록만 뜨고 안내가 보인다")
    void 없는_식별자면_목록만_뜬다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(FixedExpenseView.class), eq(999L)))
                .thenThrow(new BackendApiException(ErrorCode.FIXED_EXPENSE_NOT_FOUND.code(),
                        "고정지출을 찾을 수 없습니다."));

        mockMvc.perform(get(LIST_URL).param("m", "edit").param("id", "999")
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                // 모달을 열지 않기로 한 경우 키 자체를 담지 않는다.
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attributeExists("fixedExpenses"))
                // 없는 것과 남의 것을 가르지 않는다 — 서버 문구를 그대로 쓴다.
                .andExpect(model().attribute("notice", "고정지출을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("식별자 없이 수정 모달을 열면 목록만 뜬다")
    void 식별자가_없으면_열지_않는다() throws Exception {
        mockMvc.perform(get(LIST_URL).param("m", "edit").session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));
    }
}

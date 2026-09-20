package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.client.BinaryPayload;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 3.5 양식 받기 — <b>브라우저가 백엔드를 직접 부르지 않는다.</b>
 *
 * <p>양식 받기에도 인증이 필요한데 <b>브라우저의 링크는 인증을 붙이지 못한다.</b> 그래서
 * 화면 모듈 주소를 걸고, 여기서 세션의 인증을 실어 받아 바이트를 흘려보낸다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelTemplateTest {

    private static final String PAGE_URL = "/ledger/excel";
    private static final String TEMPLATE_URL = "/ledger/excel/template";
    private static final String BACKEND_PATH = "/expense-incomes/excel/template";

    private static final String XLSX_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
    @DisplayName("양식 링크가 화면 모듈 주소이고 백엔드 주소가 응답에 없다 (FR-924)")
    void 양식_링크가_화면_모듈_주소다() throws Exception {
        String html = mockMvc.perform(get(PAGE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("href=\"/ledger/excel/template\"");

        // 브라우저의 링크는 인증을 붙이지 못한다. 백엔드 주소가 새어 나가면 그 링크는
        // 언제나 실패한다.
        assertThat(html).doesNotContain("/api/v1").doesNotContain(BACKEND_PATH);
        assertThat(html).doesNotContain(":8081");
    }

    @Test
    @DisplayName("받은 파일 이름과 형식을 그대로 넘긴다")
    void 이름과_형식을_그대로_넘긴다() throws Exception {
        when(backendApiClient.getBinary(eq(BACKEND_PATH)))
                .thenReturn(new BinaryPayload("xlsx-bytes".getBytes(StandardCharsets.UTF_8),
                        XLSX_TYPE, "expense_income_template.xlsx"));

        // 화면이 이름을 지어내면 백엔드가 양식을 바꿀 때 파일 이름만 옛것으로 남는다.
        mockMvc.perform(get(TEMPLATE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(XLSX_TYPE))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("expense_income_template.xlsx")))
                .andExpect(content().bytes("xlsx-bytes".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("이름을 받지 못하면 지어내지 않고 정해진 값을 쓴다")
    void 이름이_없으면_정해진_값을_쓴다() throws Exception {
        when(backendApiClient.getBinary(eq(BACKEND_PATH)))
                .thenReturn(new BinaryPayload("xlsx".getBytes(StandardCharsets.UTF_8),
                        XLSX_TYPE, null));

        mockMvc.perform(get(TEMPLATE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".xlsx")));
    }

    @Test
    @DisplayName("양식을 받지 못하면 오류 화면으로 간다 — 아이콘과 다르다")
    void 받지_못하면_오류_화면이다() throws Exception {
        when(backendApiClient.getBinary(eq(BACKEND_PATH)))
                .thenThrow(new BackendApiException(ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "서버 오류가 발생했습니다."));

        // 아이콘은 화면 한 귀퉁이가 비는 것으로 끝나지만, 양식을 못 받으면 사용자가 이
        // 화면에서 할 수 있는 일이 없다 — 007 의 공통 오류 화면으로 간다.
        mockMvc.perform(get(TEMPLATE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .view().name("error"));
    }
}

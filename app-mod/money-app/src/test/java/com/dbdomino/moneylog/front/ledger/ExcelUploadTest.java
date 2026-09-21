package com.dbdomino.moneylog.front.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 3.5 엑셀 올리기 — <b>성공과 실패의 모양이 다르다.</b>
 *
 * <p>성공은 건수 셋이고 실패는 <b>행 번호·열·사유의 표</b>다. 표가 나오려면 007 의 실패
 * 예외가 <b>봉투의 값을 통째로</b> 들고 올라와야 한다 — 코드와 문구만으로는 "몇 행의 무엇을
 * 고쳐야 하는지"가 화면에 닿지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExcelUploadTest {

    private static final String PAGE_URL = "/ledger/excel";
    private static final String UPLOAD_PATH = "/expense-incomes/excel/upload";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
    }

    @Test
    @DisplayName("성공하면 건수와 가계부로 가는 길이 보인다")
    void 성공하면_건수와_가계부로_가는_길이_보인다() throws Exception {
        when(backendApiClient.postMultipart(eq(UPLOAD_PATH), any(), eq(ExcelUploadResult.class)))
                .thenReturn(new ExcelUploadResult(25, 20, 5));

        String html = upload().andExpect(status().isOk())
                .andExpect(view().name("ledger/excel"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("25").contains("20").contains("5");
        // 올린 것이 실제로 들어갔는지 확인할 자리가 있어야 한다.
        assertThat(html).contains("href=\"/ledger\"").contains("가계부에서 확인");

        // 성공 화면에 오류 표가 함께 뜨지 않는다.
        assertThat(html).doesNotContain("고쳐야 할 행");
    }

    @Test
    @DisplayName("행 오류가 표로, 행 번호 순으로 보인다 (FR-927·SC-907)")
    void 행_오류가_행_번호_순의_표로_보인다() throws Exception {
        // 응답이 뒤섞여 와도 화면이 순서를 보장한다 — 사용자는 엑셀을 열어 위에서부터
        // 고치기 때문이다.
        when(backendApiClient.postMultipart(eq(UPLOAD_PATH), any(), eq(ExcelUploadResult.class)))
                .thenThrow(rowErrorFailure("""
                        {"message":"엑셀 검증에 실패했습니다","errors":[
                          {"row":8,"column":"D","message":"존재하지 않는 수단입니다"},
                          {"row":5,"column":"C","message":"금액은 0보다 커야 합니다"}
                        ]}"""));

        String html = upload().andExpect(status().isOk())
                .andExpect(view().name("ledger/excel"))
                .andExpect(model().attributeExists("rowErrors"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("금액은 0보다 커야 합니다").contains("존재하지 않는 수단입니다");
        assertThat(html.indexOf("금액은 0보다 커야 합니다"))
                .as("5행이 8행보다 먼저 보여야 한다")
                .isLessThan(html.indexOf("존재하지 않는 수단입니다"));

        // 성공 건수 영역이 함께 뜨지 않는다 — 모양이 다르다.
        assertThat(html).doesNotContain("등록 결과");
    }

    @Test
    @DisplayName("표 위에 전체가 등록되지 않았다가 적혀 있다")
    void 표_위에_전체_실패가_적혀_있다() throws Exception {
        when(backendApiClient.postMultipart(eq(UPLOAD_PATH), any(), eq(ExcelUploadResult.class)))
                .thenThrow(rowErrorFailure("""
                        {"message":"엑셀 검증에 실패했습니다","errors":[
                          {"row":5,"column":"C","message":"금액은 0보다 커야 합니다"}
                        ]}"""));

        String html = upload().andReturn().getResponse().getContentAsString();

        // 화면이 "몇 건은 들어갔다"고 말하지 않는다.
        assertThat(html).contains("전체가 등록되지 않습니다");
        assertThat(html).contains("한 건도 등록되지 않았습니다");
        assertThat(html.indexOf("전체가 등록되지 않습니다"))
                .as("안내가 표보다 위에 있어야 한다")
                .isLessThan(html.indexOf("<table"));
    }

    @Test
    @DisplayName("형식·한도 거절 안내에 허용 형식과 한도가 적혀 있다")
    void 거절_안내에_제약을_다시_적는다() throws Exception {
        when(backendApiClient.postMultipart(eq(UPLOAD_PATH), any(), eq(ExcelUploadResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXCEL_FORMAT_NOT_XLSX.code(),
                        ErrorCode.EXCEL_FORMAT_NOT_XLSX.message()));

        String html = upload().andReturn().getResponse().getContentAsString();

        // 무엇이 허용되는지 모른 채 다른 파일을 시도하게 두지 않는다.
        assertThat(html).contains("xlsx 파일만 업로드할 수 있습니다");
        assertThat(html).contains(".xlsx 형식만").contains("300행까지");

        // 행 오류가 아니므로 표가 뜨지 않는다.
        assertThat(html).doesNotContain("고쳐야 할 행");
    }

    @Test
    @DisplayName("파일 선택 필터가 .xlsx 하나다 (FR-925)")
    void 파일_선택_필터가_xlsx_하나다() throws Exception {
        String html = mockMvc.perform(get(PAGE_URL).session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 프로토타입은 .xls 를 함께 허용한다. 백엔드가 받지 않아 그대로 두면 "고를 수는
        // 있는데 거절당하는 형식"이 남는다.
        assertThat(html).contains("accept=\".xlsx\"");
        assertThat(html).doesNotContain(".xls,").doesNotContain(",.xls");
    }

    @Test
    @DisplayName("큰 파일에 올리기 전 경고가 걸리고, 막지는 않는다 (FR-926·SC-908)")
    void 올리기_전에_경고한다() throws Exception {
        String html = mockMvc.perform(get(PAGE_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-excel-warning");
        assertThat(html).contains("300행을 넘을 수 있습니다");
        // 막는 것이 아니라 알리는 것이다 — 가늠이 틀릴 수 있다.
        assertThat(html).contains("그래도 올리려면");
    }

    @Test
    @DisplayName("중복 행에 별도 경고가 없다 (FR-928)")
    void 중복_행에_경고하지_않는다() throws Exception {
        String html = mockMvc.perform(get(PAGE_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // 같은 날 같은 금액을 두 번 쓰는 일은 흔하다. 화면이 임의로 막으면 그 사용자가
        // 기록하지 못한다.
        assertThat(html).doesNotContain("중복");
    }

    @Test
    @DisplayName("파일 없이 올리면 왕복하지 않고 사유를 보인다")
    void 파일_없이_올리면_왕복하지_않는다() throws Exception {
        mockMvc.perform(multipart(PAGE_URL)
                        .file(new MockMultipartFile("file", "", null, new byte[0]))
                        .session(LoggedInSessions.member()))
                .andExpect(status().isOk())
                .andExpect(view().name("ledger/excel"))
                .andExpect(model().attribute("failResCode", ErrorCode.EXCEL_FILE_EMPTY.code()));

        org.mockito.Mockito.verify(backendApiClient, org.mockito.Mockito.never())
                .postMultipart(eq(UPLOAD_PATH), any(), eq(ExcelUploadResult.class));
    }

    // ── 도우미 ──────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions upload() throws Exception {
        return mockMvc.perform(multipart(PAGE_URL)
                .file(new MockMultipartFile("file", "2026-07.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "xlsx-bytes".getBytes(StandardCharsets.UTF_8)))
                .session(LoggedInSessions.member()));
    }

    /** 행 오류를 담은 실패. <b>봉투의 값이 함께 올라오는</b> 유일한 실패다. */
    private BackendApiException rowErrorFailure(String dataJson) {
        return new BackendApiException(ErrorCode.EXCEL_ROW_VALIDATION_FAILED.code(),
                "엑셀 검증에 실패했습니다", objectMapper.readTree(dataJson));
    }
}

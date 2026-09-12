package com.dbdomino.moneylog.front.expendgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
import com.dbdomino.moneylog.front.support.LoggedInSessions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;

/**
 * 아이콘을 올리고 보이는 길.
 *
 * <p>두 가지가 눈으로 확인되지 않아 시험으로만 고정된다 — <b>브라우저가 백엔드 주소로
 * 아이콘을 요청하지 않는다</b>는 것과 <b>파일을 고르지 않은 수정에 파일 칸이 실리지
 * 않는다</b>는 것이다. 뒤의 것을 놓치면 이름만 고쳤는데 형식 오류가 난다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpendGroupIconTest {

    private static final String LIST_URL = "/expend-groups";
    private static final String ITEM_PATH = "/expend-groups/{expendGroupId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "hong", SessionUser.ROLE_MEMBER, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(ExpendGroupListResult.class)))
                .thenReturn(ExpendGroupFixture.page());
    }

    @Test
    @DisplayName("이미지 주소가 화면 모듈 경로이고 백엔드 주소가 응답 어디에도 없다")
    void 아이콘을_화면_모듈_주소로_건다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/expend-groups/icons/1_1.png");
        // 백엔드 주소를 직접 걸면 인증이 붙지 않아 전부 실패하고, 그 실패는 이미지 깨짐으로만
        // 보여 원인을 짐작하기 어렵다.
        assertThat(html)
                .as("브라우저가 백엔드를 직접 부르는 길이 화면에 남으면 안 된다")
                .doesNotContain("/api/v1/");
    }

    @Test
    @DisplayName("아이콘이 없는 유형에는 이미지 태그를 걸지 않는다")
    void 아이콘이_없으면_이미지를_걸지_않는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()))
                .andReturn().getResponse().getContentAsString();

        // "기타"는 아이콘이 없다. 빈 주소를 건 이미지는 브라우저가 현재 페이지를 다시 받아 와
        // 깨진 표시를 남긴다 — 자리가 비어 보이는 것과 깨진 것은 다르다.
        int row = html.indexOf("기타");
        int rowStart = html.lastIndexOf("<tr", row);
        int rowEnd = html.indexOf("</tr>", row);
        String cells = html.substring(rowStart, rowEnd);
        assertThat(cells).doesNotContain("<img");
        assertThat(cells).contains("—");
    }

    @Test
    @DisplayName("파일을 고르지 않은 수정에 파일 칸이 실리지 않는다")
    void 파일을_고르지_않으면_그_칸을_빼고_보낸다() throws Exception {
        when(backendApiClient.patchMultipart(eq(ITEM_PATH), any(), eq(ExpendGroupResponse.class),
                eq(2L))).thenReturn(ExpendGroupFixture.custom());

        // 브라우저의 파일 입력은 고르지 않아도 빈 값을 함께 보낸다. 그것을 그대로 옮기면
        // 0바이트 파일을 올리는 요청이 되어 "이름만 고쳤는데 아이콘이 잘못됐다"가 된다.
        mockMvc.perform(multipart("/expend-groups/2")
                        .file(new MockMultipartFile("iconFile", "", "application/octet-stream",
                                new byte[0]))
                        .session(LoggedInSessions.member())
                        .param("name", "새이름").param("inUse", "true"))
                .andExpect(status().isOk());

        assertThat(capturedParts(2L)).doesNotContainKey("iconFile");
        assertThat(capturedParts(2L)).containsKey("name");
    }

    @Test
    @DisplayName("파일을 고른 수정에는 파일 칸이 실린다")
    void 파일을_고르면_실린다() throws Exception {
        when(backendApiClient.patchMultipart(eq(ITEM_PATH), any(), eq(ExpendGroupResponse.class),
                eq(2L))).thenReturn(ExpendGroupFixture.custom());

        mockMvc.perform(multipart("/expend-groups/2")
                        .file(new MockMultipartFile("iconFile", "icon.png", "image/png",
                                new byte[] {1, 2, 3}))
                        .session(LoggedInSessions.member())
                        .param("name", "새이름").param("inUse", "true"))
                .andExpect(status().isOk());

        assertThat(capturedParts(2L)).containsKey("iconFile");
    }

    @Test
    @DisplayName("아이콘 제약이 입력 전부터 보인다")
    void 제약을_입력_전부터_알린다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "create"))
                .andReturn().getResponse().getContentAsString();

        // 고른 뒤에만 알려 주면 사용자는 파일을 찾아 다시 골라야 한다.
        assertThat(html).contains("png·jpg·gif 형식, 1MB 이하");
        assertThat(html).contains("확장자가 아니라 파일 내용으로 판정");
    }

    @Test
    @DisplayName("아이콘이 거절되면 허용 형식과 크기를 다시 알린다")
    void 거절되면_제약을_다시_알린다() throws Exception {
        when(backendApiClient.postMultipart(eq(LIST_URL), any(), eq(ExpendGroupResponse.class)))
                .thenThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_ICON_INVALID.code(),
                        ErrorCode.EXPEND_GROUP_ICON_INVALID.message()));

        String html = mockMvc.perform(multipart(LIST_URL)
                        .file(new MockMultipartFile("iconFile", "big.png", "image/png",
                                new byte[] {1}))
                        .session(LoggedInSessions.member())
                        .param("name", "새유형").param("inUse", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_ICON_INVALID.message());
        assertThat(html)
                .as("무엇이 허용되는지 모른 채 다른 파일을 시도하게 두지 않는다")
                .contains("png·jpg·gif 형식, 1MB 이하");
    }

    @Test
    @DisplayName("미리보기는 브라우저가 그리며 백엔드를 부르지 않는다")
    void 미리보기가_API_를_부르지_않는다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "create"))
                .andReturn().getResponse().getContentAsString();

        // 007 이 "화면 스크립트는 API 를 부르지 않는다"로 정한 선 안에 있다.
        assertThat(html).contains("data-icon-preview");
        assertThat(html).contains("FileReader");
    }

    @SuppressWarnings("unchecked")
    private MultiValueMap<String, Object> capturedParts(long id) {
        ArgumentCaptor<MultiValueMap<String, Object>> parts =
                ArgumentCaptor.forClass(MultiValueMap.class);
        verify(backendApiClient).patchMultipart(eq(ITEM_PATH), parts.capture(),
                eq(ExpendGroupResponse.class), eq(id));
        return parts.getValue();
    }
}

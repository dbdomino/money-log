package com.dbdomino.moneylog.front.expendgroup;

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
 * 2.6 등록 · 2.7 상세 · 2.8 수정 모달.
 *
 * <p>수단 모달과 같은 규칙이며 <b>기본 유형의 이름 칸이 잠긴다</b>는 것 하나가 더 있다.
 * 목록 응답이 기본인지 알려 주므로 화면이 미리 막을 수 있다 — 수단의 용도와 다른 점이며,
 * 그쪽은 참조 건수를 알 방법이 없어 미리 막지 못한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExpendGroupModalTest {

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
    @DisplayName("m=create 로 들어오면 등록 모달이 열린 채 목록이 뜬다")
    void 등록_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member()).param("m", "create"))
                .andExpect(status().isOk())
                .andExpect(view().name("expend-groups/list"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"));
    }

    @Test
    @DisplayName("기본 유형의 수정 모달은 이름 칸이 잠긴 채 열린다")
    void 기본_유형은_이름_칸이_잠긴다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(ExpendGroupResponse.class), eq(1L)))
                .thenReturn(ExpendGroupFixture.defaultGroup());

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "edit").param("id", "1"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andReturn().getResponse().getContentAsString();

        int nameInput = html.indexOf("id=\"edit-name\"");
        int inUseField = html.indexOf("id=\"edit-inUse\"");
        assertThat(nameInput).isGreaterThan(0);
        assertThat(html.substring(nameInput, inUseField))
                .as("고칠 수 없는 칸을 열어 두고 저장에서 실패시킬 이유가 없다")
                .contains("readonly")
                .contains("기본 유형의 이름은 바꿀 수 없습니다");
    }

    @Test
    @DisplayName("직접 만든 유형은 이름 칸이 열린 채 값이 채워진다")
    void 직접_만든_유형은_이름을_고칠_수_있다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(ExpendGroupResponse.class), eq(2L)))
                .thenReturn(ExpendGroupFixture.custom());

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "edit").param("id", "2"))
                .andReturn().getResponse().getContentAsString();

        int nameInput = html.indexOf("id=\"edit-name\"");
        int inUseField = html.indexOf("id=\"edit-inUse\"");
        assertThat(html.substring(nameInput, inUseField)).doesNotContain("readonly");
        assertThat(html).contains("value=\"취미\"");
    }

    @Test
    @DisplayName("상세 모달은 읽기 전용이고 수정으로 가는 길이 있다")
    void 상세_모달은_읽기_전용이다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(ExpendGroupResponse.class), eq(2L)))
                .thenReturn(ExpendGroupFixture.custom());

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "detail").param("id", "2"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "detail"))
                .andReturn().getResponse().getContentAsString();

        int bodyStart = html.indexOf("expend-group-detail-body");
        int bodyEnd = html.indexOf("expend-group-edit-body", bodyStart);
        String detail = html.substring(bodyStart, bodyEnd);
        assertThat(detail).contains("취미");
        assertThat(detail).doesNotContain("<input");
        assertThat(detail).contains("m=edit&amp;id=2");
    }

    @Test
    @DisplayName("모르는 식별자면 모달 없이 목록이 정상으로 뜬다")
    void 모르는_식별자는_목록만_보인다() throws Exception {
        when(backendApiClient.get(eq(ITEM_PATH), eq(ExpendGroupResponse.class), eq(99L)))
                .thenThrow(new BackendApiException(ErrorCode.EXPEND_GROUP_NOT_FOUND.code(),
                        ErrorCode.EXPEND_GROUP_NOT_FOUND.message()));

        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.member())
                        .param("m", "edit").param("id", "99"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attribute("notice", ErrorCode.EXPEND_GROUP_NOT_FOUND.message()));
    }

    @Test
    @DisplayName("등록 실패가 모달을 연 채로 돌아오고 이름 중복은 삭제된 이름도 센다고 알린다")
    void 이름_중복이_삭제된_이름도_센다고_알린다() throws Exception {
        when(backendApiClient.postMultipart(eq(LIST_URL), any(), eq(ExpendGroupResponse.class)))
                .thenThrow(new BackendApiException(
                        ErrorCode.EXPEND_GROUP_NAME_DUPLICATED.code(),
                        ErrorCode.EXPEND_GROUP_NAME_DUPLICATED.message()));

        String html = mockMvc.perform(multipart(LIST_URL).session(LoggedInSessions.member())
                        .param("name", "없앤유형").param("inUse", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_NAME_DUPLICATED.message());
        // 적지 않으면 사용자는 삭제한 것과 겹칠 리 없다고 생각해 화면이 잘못됐다고 읽는다.
        assertThat(html).contains("삭제 표시된 유형의 이름도 겹침으로 셉니다");
    }

    @Test
    @DisplayName("기본 유형의 이름 변경이 서버에서 거절되면 이름 칸에 붙는다")
    void 기본_유형_이름_변경_거절이_칸에_붙는다() throws Exception {
        when(backendApiClient.patchMultipart(eq(ITEM_PATH), any(), eq(ExpendGroupResponse.class),
                eq(1L)))
                .thenThrow(new BackendApiException(
                        ErrorCode.EXPEND_GROUP_DEFAULT_NAME_LOCKED.code(),
                        ErrorCode.EXPEND_GROUP_DEFAULT_NAME_LOCKED.message()));

        // 화면이 칸을 잠가도 주소로 직접 올 수 있다. 화면이 막는 것과 서버가 막는 것은
        // 서로를 대신하지 않는다.
        String html = mockMvc.perform(multipart("/expend-groups/1")
                        .session(LoggedInSessions.member())
                        .param("name", "바꾼이름").param("inUse", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.EXPEND_GROUP_DEFAULT_NAME_LOCKED.message());
    }
}

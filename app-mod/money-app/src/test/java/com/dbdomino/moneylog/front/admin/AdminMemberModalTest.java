package com.dbdomino.moneylog.front.admin;

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
import com.dbdomino.moneylog.front.member.MemberView;
import com.dbdomino.moneylog.front.session.SessionUser;
import com.dbdomino.moneylog.front.session.TokenValidateResult;
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

/**
 * 1.9 회원 추가와 1.10 회원 수정 모달.
 *
 * <p>모르는 식별자로 들어왔을 때 <b>오류 화면이 아니라 목록이 정상으로</b> 뜨는 것이 중요하다.
 * 낡은 북마크일 뿐이고 목록 자체는 멀쩡하다 — 오류 화면으로 보내면 관리자는 회원 관리가
 * 고장 났다고 읽는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberModalTest {

    private static final String LIST_URL = "/admin/members";
    private static final String MEMBER_PATH = "/admin/members/{memberId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @BeforeEach
    void setUp() {
        when(backendApiClient.get(eq("/auth/validate"), eq(TokenValidateResult.class)))
                .thenReturn(new TokenValidateResult(true, "admin", SessionUser.ROLE_ADMIN, 86_400));
        when(backendApiClient.getByQuery(eq(LIST_URL), any(), eq(AdminMemberListResult.class)))
                .thenReturn(AdminMemberListTest.page());
    }

    @Test
    @DisplayName("m=create 로 들어오면 추가 모달이 열린 채 목록이 뜬다")
    void 추가_모달이_열린다() throws Exception {
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()).param("m", "create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"));

        // 새로 만드는 화면이라 미리 가져올 값이 없다.
        verify(backendApiClient, never()).get(eq(MEMBER_PATH), eq(MemberView.class), any());
    }

    @Test
    @DisplayName("m=edit 로 들어오면 그 회원의 값이 채워진 채 모달이 열린다")
    void 수정_모달이_값을_채운_채_열린다() throws Exception {
        when(backendApiClient.get(eq(MEMBER_PATH), eq(MemberView.class), eq("hong")))
                .thenReturn(new MemberView("hong", "홍길동", "hong@example.com", "01012345678",
                        "안녕하세요", SessionUser.ROLE_MEMBER, true));

        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin())
                        .param("m", "edit").param("id", "hong"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andReturn().getResponse().getContentAsString();

        // 여는 일만 브라우저가 하면 열린 모달이 빈 채로 뜨고 값을 채우려면 다시 요청해야 한다.
        assertThat(html).contains("안녕하세요");
        assertThat(html).contains("value=\"hong@example.com\"");
    }

    @Test
    @DisplayName("모르는 식별자면 오류 화면이 아니라 목록이 정상으로 뜬다")
    void 모르는_식별자는_목록만_보인다() throws Exception {
        when(backendApiClient.get(eq(MEMBER_PATH), eq(MemberView.class), eq("nobody")))
                .thenThrow(new BackendApiException(ErrorCode.MEMBER_NOT_FOUND.code(),
                        ErrorCode.MEMBER_NOT_FOUND.message()));

        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin())
                        .param("m", "edit").param("id", "nobody"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE))
                .andExpect(model().attribute("notice", ErrorCode.MEMBER_NOT_FOUND.message()));
    }

    @Test
    @DisplayName("식별자 없이 m=edit 면 모달 없이 목록만 보인다")
    void 식별자가_없으면_모달을_열지_않는다() throws Exception {
        // 누구를 고칠지 모르는 상태다.
        mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin()).param("m", "edit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist(ModalParam.MODEL_ATTRIBUTE));

        verify(backendApiClient, never()).get(eq(MEMBER_PATH), eq(MemberView.class), any());
    }

    @Test
    @DisplayName("추가가 실패하면 모달을 연 채로 목록이 다시 뜬다")
    void 추가_실패가_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.post(eq(LIST_URL), any(), eq(MemberView.class)))
                .thenThrow(new BackendApiException(ErrorCode.MEMBER_ID_DUPLICATED.code(),
                        ErrorCode.MEMBER_ID_DUPLICATED.message()));

        String html = mockMvc.perform(post(LIST_URL).session(LoggedInSessions.admin())
                        .param("memberId", "hong")
                        .param("password", "TempPass1!")
                        .param("nickname", "홍길동")
                        .param("role", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/members"))
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "create"))
                .andReturn().getResponse().getContentAsString();

        // 모달을 닫아 버리면 관리자가 채운 칸이 전부 사라진다.
        assertThat(html).contains(ErrorCode.MEMBER_ID_DUPLICATED.message());
        assertThat(html).doesNotContain("TempPass1!");
    }

    @Test
    @DisplayName("수정이 실패하면 그 회원의 모달을 연 채로 목록이 다시 뜬다")
    void 수정_실패가_모달을_연_채로_돌아온다() throws Exception {
        when(backendApiClient.patch(eq(MEMBER_PATH), any(), eq(MemberView.class), eq("hong")))
                .thenThrow(new BackendApiException(ErrorCode.EMAIL_DUPLICATED.code(),
                        ErrorCode.EMAIL_DUPLICATED.message()));

        mockMvc.perform(post("/admin/members/hong").session(LoggedInSessions.admin())
                        .param("nickname", "홍길동").param("role", "3")
                        .param("email", "taken@example.com"))
                .andExpect(status().isOk())
                .andExpect(model().attribute(ModalParam.MODEL_ATTRIBUTE, "edit"))
                .andExpect(model().attribute("editTargetId", "hong"));
    }

    @Test
    @DisplayName("권한은 관리자와 일반 두 값만 고를 수 있다")
    void 권한_선택지가_둘뿐이다() throws Exception {
        String html = mockMvc.perform(get(LIST_URL).session(LoggedInSessions.admin())
                        .param("m", "create"))
                .andReturn().getResponse().getContentAsString();

        int selectStart = html.indexOf("id=\"create-role\"");
        int selectEnd = html.indexOf("</select>", selectStart);
        String options = html.substring(selectStart, selectEnd);

        // 화면이 고를 수 없는 값을 백엔드가 받는 일이 없어야 한다.
        assertThat(options).contains("value=\"1\"").contains("value=\"3\"");
        assertThat(options.split("<option").length - 1).isEqualTo(2);
    }

    @Test
    @DisplayName("수정 저장의 빈 칸 뜻이 본인 정보와 같다")
    void 빈_칸의_뜻이_1_7_과_같다() throws Exception {
        when(backendApiClient.patch(eq(MEMBER_PATH), any(), eq(MemberView.class), eq("hong")))
                .thenReturn(new MemberView("hong", "홍길동", null, null, null,
                        SessionUser.ROLE_MEMBER, true));

        mockMvc.perform(post("/admin/members/hong").session(LoggedInSessions.admin())
                        .param("nickname", "홍길동").param("role", "3")
                        .param("email", "").param("newPassword", ""))
                .andExpect(status().isOk());

        Map<String, Object> body = capturedPatchBody();
        assertThat(body).doesNotContainKey("password");
        assertThat(body).containsEntry("email", null);
        assertThat(body).containsEntry("role", 3);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPatchBody() {
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(backendApiClient).patch(eq(MEMBER_PATH), body.capture(), eq(MemberView.class),
                eq("hong"));
        return (Map<String, Object>) body.getValue();
    }
}

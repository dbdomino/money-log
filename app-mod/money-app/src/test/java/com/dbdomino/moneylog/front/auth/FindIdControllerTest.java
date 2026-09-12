package com.dbdomino.moneylog.front.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dbdomino.moneylog.common.error.ErrorCode;
import com.dbdomino.moneylog.front.client.BackendApiClient;
import com.dbdomino.moneylog.front.client.BackendApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 1.3 아이디 찾기 화면. 결과가 <b>같은 화면에</b> 나타나고 결과 전용 주소가 없다. */
@SpringBootTest
@AutoConfigureMockMvc
class FindIdControllerTest {

    private static final String FIND_ID_URL = "/auth/find-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackendApiClient backendApiClient;

    @Test
    @DisplayName("열면 이메일 한 칸짜리 폼이 뜬다")
    void 폼이_뜬다() throws Exception {
        mockMvc.perform(get(FIND_ID_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/find-id"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"email\"")));
    }

    @Test
    @DisplayName("찾으면 같은 화면에 아이디가 나오고 가려져 왔으면 그 사실이 적힌다")
    void 결과가_같은_화면에_나온다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(FIND_ID_URL), any(), eq(FindIdResult.class)))
                .thenReturn(new FindIdResult("use***01", true));

        String html = mockMvc.perform(post(FIND_ID_URL).param("email", "user01@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/find-id"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("use***01");
        assertThat(html)
                .as("적지 않으면 사용자는 가려진 값이 진짜 아이디라고 믿고 로그인을 시도한다")
                .contains("가려진 아이디");
    }

    @Test
    @DisplayName("가려지지 않은 결과에는 가림 안내를 붙이지 않는다")
    void 가리지_않았으면_안내가_없다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(FIND_ID_URL), any(), eq(FindIdResult.class)))
                .thenReturn(new FindIdResult("user01", false));

        String html = mockMvc.perform(post(FIND_ID_URL).param("email", "user01@example.com"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("user01");
        assertThat(html).doesNotContain("가려진 아이디");
    }

    @Test
    @DisplayName("2001 은 그 이메일로 가입된 회원이 없다는 안내다")
    void 회원이_없으면_안내한다() throws Exception {
        when(backendApiClient.postWithoutAuth(eq(FIND_ID_URL), any(), eq(FindIdResult.class)))
                .thenThrow(new BackendApiException(ErrorCode.MEMBER_NOT_FOUND.code(),
                        ErrorCode.MEMBER_NOT_FOUND.message()));

        String html = mockMvc.perform(post(FIND_ID_URL).param("email", "none@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/find-id"))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(ErrorCode.MEMBER_NOT_FOUND.message());
        assertThat(html)
                .as("실패했으니 결과 영역은 그리지 않는다")
                .doesNotContain("찾은 아이디");
        assertThat(html)
                .as("사용자가 넣은 이메일은 남는다")
                .contains("none@example.com");
    }
}

package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.common.api.RestResponseDto;
import com.dbdomino.moneylog.common.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인가 실패({@code 1002})를 {@code { resCode, data }} 규격으로 내보낸다.
 *
 * <p>일반 회원 토큰으로 관리자 API 를 부른 경우가 여기로 온다. 이 핸들러가 없으면
 * Spring Security 의 기본 403 JSON 이 나가고 <b>SC-106("관리자 API 5건 전부
 * {@code 1002}")이 깨진다</b>(quickstart #38).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                RestResponseDto.fail(ErrorCode.FORBIDDEN_ADMIN_ONLY));
    }
}

package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.common.api.RestResponseDto;
import com.dbdomino.moneylog.common.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 실패({@code 1001})를 {@code { resCode, data }} 규격으로 내보낸다.
 *
 * <p>Spring Security 의 기본 응답은 HTTP 401 + 자체 JSON 이다. 그대로 두면 인증이
 * 필요한 API 를 토큰 없이 불렀을 때만 응답 형식이 달라져, 클라이언트가 이 경로 하나를
 * 위해 별도 파싱을 갖게 된다(002 SC-101 · quickstart #37).
 *
 * <p>비즈니스 실패와 마찬가지로 <b>HTTP 200</b>으로 내보낸다.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), RestResponseDto.fail(ErrorCode.UNAUTHORIZED));
    }
}

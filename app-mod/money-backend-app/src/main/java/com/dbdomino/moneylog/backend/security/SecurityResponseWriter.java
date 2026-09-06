package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.common.api.RestResponseDto;
import com.dbdomino.moneylog.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 보안 계층의 실패 응답을 {@code { resCode, data }} 규격으로 쓴다.
 *
 * <p>필터·엔트리포인트·인가 실패 핸들러는 {@code @RestControllerAdvice} 가 닿지 않는
 * 자리다. Controller 진입 전에 끝나기 때문이다. 그래서 규격을 지키는 일을 이 한 곳에
 * 모아 둔다 — 세 곳이 각자 JSON 을 만들면 한 곳만 형식이 틀어져도 알아채기 어렵다.
 *
 * <p><b>HTTP 상태는 200 이다.</b> 비즈니스 실패와 같은 규칙이며, 클라이언트가 상태 코드로
 * 분기하지 않고 {@code resCode} 하나만 보게 하려는 것이다(SC-101).
 */
@Component
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 실패 코드를 규격 응답으로 내보낸다. */
    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), RestResponseDto.fail(errorCode));
    }
}

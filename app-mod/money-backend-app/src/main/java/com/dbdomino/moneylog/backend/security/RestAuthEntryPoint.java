package com.dbdomino.moneylog.backend.security;

import com.dbdomino.moneylog.backend.controller.ExpendGroupIconController;
import com.dbdomino.moneylog.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증 없이 보호 API 에 닿았을 때의 응답.
 *
 * <p>Spring Security 의 기본 응답은 HTTP 401 + 자체 JSON 이다. 그대로 두면 이 경로에서만
 * 응답 형식이 달라져, 클라이언트가 여기 하나를 위해 별도 파싱을 갖게 된다
 * (SC-101 · quickstart #37).
 *
 * <h2>코드는 필터가 정한 것을 쓴다</h2>
 *
 * <p>토큰이 아예 없으면 {@code 1001} 이지만, 토큰은 있는데 세션이 무효면 {@code 1006},
 * 계정이 비활성이면 {@code 1004} 다. 그 판정은 DB 를 본 {@link TokenAuthenticationFilter}
 * 만 할 수 있으므로, 필터가 요청 속성에 담아 둔 코드를 여기서 꺼내 쓴다. 없으면
 * "토큰이 오지 않았다"는 뜻이라 {@code 1001} 이다.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final SecurityResponseWriter responseWriter;

    public RestAuthEntryPoint(SecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (isIconRequest(request)) {
            // 4자리 코드를 실을 JSON 본문이 없는 API 다. 상태 코드만 남긴다.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Object attribute = request.getAttribute(TokenAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        ErrorCode errorCode = attribute instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED;
        responseWriter.write(response, errorCode);
    }

    /**
     * 2.10 아이콘 조회인가.
     *
     * <p>경로 문자열을 여기 적지 않고 {@link ExpendGroupIconController#ICON_PATH} 를 본다 —
     * 두 곳에 복제하면 경로를 바꿀 때 한쪽만 고쳐 놓고 "아이콘만 이상한 401 이 온다"가 된다.
     */
    private static boolean isIconRequest(HttpServletRequest request) {
        return request.getRequestURI() != null
                && request.getRequestURI().startsWith(ExpendGroupIconController.ICON_PATH);
    }
}

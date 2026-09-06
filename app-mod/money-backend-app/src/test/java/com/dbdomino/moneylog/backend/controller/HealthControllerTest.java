package com.dbdomino.moneylog.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dbdomino.moneylog.backend.AbstractApiIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 1.1 HealthCheck.
 *
 * <p>인가 경계 표에서 이 경로만 {@code permitAll}인 GET 이다. 보안 도입 뒤에도
 * <b>토큰 없이 200</b>이어야 한다 — 여기가 막히면 나머지 API 의 규격을 확인할 진입점이
 * 사라진다.
 */
class HealthControllerTest extends AbstractApiIT {

    @Test
    @DisplayName("#35 토큰 없이 호출해도 resCode 200 이다")
    void healthReturnsResCode200() throws Exception {
        mockMvc.perform(get("/api/v1/ha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resCode").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.module").value("money-backend-app"));
    }

    @Test
    @DisplayName("#37 인증이 필요한 경로를 토큰 없이 부르면 Spring 기본 401 이 아니라 resCode 1001 이다")
    void protectedEndpointReturnsUnauthorizedResCode() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resCode").value(1001));
    }
}

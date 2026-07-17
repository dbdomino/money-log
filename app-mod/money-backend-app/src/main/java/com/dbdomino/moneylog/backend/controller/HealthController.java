package com.dbdomino.moneylog.backend.controller;

import com.dbdomino.moneylog.backend.dto.response.HealthResponseDto;
import com.dbdomino.moneylog.common.api.RestResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/ha")
    public RestResponseDto<HealthResponseDto> health() {
        return RestResponseDto.ok(new HealthResponseDto("UP", "money-backend-app"));
    }
}

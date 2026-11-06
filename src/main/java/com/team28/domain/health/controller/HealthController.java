package com.team28.domain.health.controller;

import com.team28.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "서버 상태 확인", description = "ApiResponse 형식이 잘 나오는지 확인하는 용도")
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.onSuccess("ok");
    }
}

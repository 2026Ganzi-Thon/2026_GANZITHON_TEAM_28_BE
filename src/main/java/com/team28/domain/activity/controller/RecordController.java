package com.team28.domain.activity.controller;

import com.team28.domain.activity.dto.RecordResponse;
import com.team28.domain.activity.service.RecordService;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Record", description = "행동 기록")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "월별 행동 기록 (S-06)",
            description = "한 달치 기록을 날짜별로 묶어서 응답. 기록 있는 날만 포함 (달력 표시 + 날짜 누르면 스크롤 이동)")
    @GetMapping
    public ApiResponse<RecordResponse.Month> getMonth(
            @CurrentUser User user,
            @Parameter(description = "yyyy-MM (예: 2026-11)", example = "2026-11") @RequestParam String month) {
        return ApiResponse.onSuccess(recordService.getMonth(user, month));
    }
}

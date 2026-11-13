package com.team28.domain.step.controller;

import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.step.service.StepService;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Step", description = "더 작게, 해봤어요")
@RestController
@RequestMapping("/api/steps")
@RequiredArgsConstructor
public class StepController {

    private final StepService stepService;

    @Operation(summary = "더 작게 (S-04) 🤖",
            description = "더 쉬운 행동 1개 생성. canShrinkMore=false 인 행동에 요청하면 SHRINK409. 이미 처리된 행동이면 STEP409.")
    @PostMapping("/{stepId}/shrink")
    public ApiResponse<GoalResponse.Detail> shrink(@CurrentUser User user, @PathVariable Long stepId) {
        return ApiResponse.onSuccess(stepService.shrink(user, stepId));
    }

    @Operation(summary = "해봤어요 (S-04)",
            description = "시도 기록만 저장 (AI 호출 없음). isLastStep=true 면 마무리 제안(완료 / 더 이어가기), false 면 S-04-3.")
    @PostMapping("/{stepId}/try")
    public ApiResponse<GoalResponse.Tried> tryStep(@CurrentUser User user, @PathVariable Long stepId) {
        return ApiResponse.onSuccess(stepService.tryStep(user, stepId));
    }
}

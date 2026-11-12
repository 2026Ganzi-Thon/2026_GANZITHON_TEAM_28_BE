package com.team28.domain.goal.controller;

import com.team28.domain.goal.dto.GoalRequest;
import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.service.GoalCreateResult;
import com.team28.domain.goal.service.GoalService;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.apiPayload.code.SuccessStatus;
import com.team28.global.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goal", description = "목표 생성·조회·다음 걸음·쉬어가기·이어하기·종료")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @Operation(summary = "홈 (S-02)", description = "진행 중 목표 1개(없으면 null) + 쉬어가는 목표 목록 + 현재 목표 개수")
    @GetMapping
    public ApiResponse<GoalResponse.Home> home(@CurrentUser User user) {
        return ApiResponse.onSuccess(goalService.getHome(user));
    }

    @Operation(summary = "목표 생성 (S-03) 🤖",
            description = "목표 + 에너지·부담(1~4) → AI 첫 걸음까지 응답. "
                    + "위기 신호면 code=CRISIS200, result={isCrisis:true} 이고 목표는 저장하지 않음 → S-08. "
                    + "기존 진행 중 목표는 자동으로 쉬어가기. 진행 중+쉬어가는 목표가 3개면 GOAL409_1.")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@CurrentUser User user,
                                                 @Valid @RequestBody GoalRequest.Create request) {
        GoalCreateResult result = goalService.create(user, request);
        if (result.crisis()) {
            return ResponseEntity
                    .status(SuccessStatus.CRISIS.getHttpStatus())
                    .body(ApiResponse.of(SuccessStatus.CRISIS, new GoalResponse.Crisis(true)));
        }
        return ResponseEntity
                .status(SuccessStatus.CREATED.getHttpStatus())
                .body(ApiResponse.of(SuccessStatus.CREATED, result.goal()));
    }

    @Operation(summary = "목표 상세 (S-04)", description = "currentStep 이 null 이면 '오늘은 여기까지' 상태 → S-04-3 으로")
    @GetMapping("/{goalId}")
    public ApiResponse<GoalResponse.Detail> detail(@CurrentUser User user, @PathVariable Long goalId) {
        return ApiResponse.onSuccess(goalService.getDetail(user, goalId));
    }

    @Operation(summary = "다음 걸음 받기 (S-04-3) 🤖",
            description = "해봤어요 후 에너지를 다시 골라 다음 행동 생성. 에너지는 처음 값 ±1 (벗어나면 ENERGY400). "
                    + "아직 현재 행동이 있으면 STEP409_1.")
    @PostMapping("/{goalId}/next-step")
    public ApiResponse<GoalResponse.Detail> nextStep(@CurrentUser User user, @PathVariable Long goalId,
                                                     @Valid @RequestBody GoalRequest.NextStep request) {
        return ApiResponse.onSuccess(goalService.nextStep(user, goalId, request));
    }

    @Operation(summary = "쉬어가기 (S-05)", description = "진행 중 → 쉬어가는 중")
    @PatchMapping("/{goalId}/rest")
    public ApiResponse<GoalResponse.Status> rest(@CurrentUser User user, @PathVariable Long goalId) {
        return ApiResponse.onSuccess(goalService.rest(user, goalId));
    }

    @Operation(summary = "이어하기 (S-02-1)", description = "쉬어가는 목표 → 진행 중. 기존 진행 중 목표는 자동으로 쉬어가기")
    @PostMapping("/{goalId}/resume")
    public ApiResponse<GoalResponse.Detail> resume(@CurrentUser User user, @PathVariable Long goalId) {
        return ApiResponse.onSuccess(goalService.resume(user, goalId));
    }

    @Operation(summary = "목표 종료 (S-07)", description = "{ \"type\": \"COMPLETED\" | \"STOPPED\" }")
    @PatchMapping("/{goalId}/end")
    public ApiResponse<GoalResponse.Status> end(@CurrentUser User user, @PathVariable Long goalId,
                                                @Valid @RequestBody GoalRequest.End request) {
        return ApiResponse.onSuccess(goalService.end(user, goalId, request));
    }
}

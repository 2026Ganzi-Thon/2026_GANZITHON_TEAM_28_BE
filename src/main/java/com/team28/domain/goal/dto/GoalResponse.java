package com.team28.domain.goal.dto;

import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.step.entity.Step;
import java.time.LocalDateTime;
import java.util.List;

public class GoalResponse {

    private GoalResponse() {
    }

    /** 현재 행동. 진행 중인데 행동이 없으면('오늘은 여기까지') null → 프론트는 S-04-3 으로 */
    public record CurrentStep(
            Long stepId,
            String content,
            String reason,
            int sequence,
            int shrinkLevel,
            int energy,
            boolean canShrinkMore,
            boolean isLastStep
    ) {
        public static CurrentStep from(Step step) {
            if (step == null) {
                return null;
            }
            return new CurrentStep(step.getId(), step.getContent(), step.getReason(), step.getSequence(), step.getShrinkLevel(),
                    step.getEnergyLevel(), step.canShrinkMore(), step.isLastStep());
        }
    }

    /** 목표 생성, 상세, 더 작게, 다음 걸음 받기, 이어하기 공통 응답 */
    public record Detail(
            Long goalId,
            String goalContent,
            GoalStatus goalStatus,
            int energyLevel,
            int burdenLevel,
            long triedCount,
            CurrentStep currentStep
    ) {
        public static Detail of(Goal goal, long triedCount, Step currentStep) {
            return new Detail(goal.getId(), goal.getContent(), goal.getStatus(), goal.getEnergyLevel(),
                    goal.getBurdenLevel(), triedCount, CurrentStep.from(currentStep));
        }
    }

    /** 목표 생성 시 위기 신호 감지 (code: CRISIS200). 목표는 저장하지 않음 */
    public record Crisis(boolean isCrisis) {
    }

    /** 홈 화면 쉬어가는 목표 카드 */
    public record RestingGoal(Long goalId, String content, long triedCount, LocalDateTime restedAt) {
    }

    /** 홈 화면 (S-02) */
    public record Home(
            Detail inProgressGoal,
            List<RestingGoal> restingGoals,
            long activeGoalCount,
            int maxGoalCount
    ) {
    }

    /** 쉬어가기, 목표 종료 응답 */
    public record Status(Long goalId, GoalStatus goalStatus) {

        public static Status from(Goal goal) {
            return new Status(goal.getId(), goal.getStatus());
        }
    }

    /** 해봤어요 응답. isLastStep=true 면 마무리 제안(완료 / 더 이어가기), false 면 S-04-3 */
    public record Tried(Long goalId, Long stepId, long triedCount, boolean isLastStep) {
    }
}

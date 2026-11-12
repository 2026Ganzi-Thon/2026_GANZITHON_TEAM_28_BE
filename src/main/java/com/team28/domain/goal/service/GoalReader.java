package com.team28.domain.goal.service;

import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.exception.GoalErrorStatus;
import com.team28.domain.goal.repository.GoalRepository;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.enums.StepStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 목표 조회·응답 조립 공통 로직. GoalService, StepService 가 같이 씁니다.
 * 호출하는 쪽의 트랜잭션 안에서 사용하세요.
 */
@Component
@RequiredArgsConstructor
public class GoalReader {

    private final GoalRepository goalRepository;
    private final StepRepository stepRepository;

    /** 내 목표만 찾는다. 다른 사람 목표면 없는 것과 똑같이 GOAL404 */
    public Goal findOwnedGoal(User user, Long goalId) {
        return goalRepository.findById(goalId)
                .filter(goal -> goal.isOwnedBy(user))
                .orElseThrow(() -> new GeneralException(GoalErrorStatus.NOT_FOUND));
    }

    public Step findCurrentStep(Goal goal) {
        return stepRepository.findFirstByGoalAndStatus(goal, StepStatus.SUGGESTED).orElse(null);
    }

    public long countTried(Goal goal) {
        return stepRepository.countByGoalAndStatus(goal, StepStatus.TRIED);
    }

    public GoalResponse.Detail toDetail(Goal goal) {
        return GoalResponse.Detail.of(goal, countTried(goal), findCurrentStep(goal));
    }
}

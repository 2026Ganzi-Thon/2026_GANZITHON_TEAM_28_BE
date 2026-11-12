package com.team28.domain.step.service;

import com.team28.domain.activity.entity.GoalActivity;
import com.team28.domain.activity.enums.ActivityType;
import com.team28.domain.activity.repository.GoalActivityRepository;
import com.team28.domain.ai.dto.AiStepResult;
import com.team28.domain.ai.service.AiService;
import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.goal.exception.GoalErrorStatus;
import com.team28.domain.goal.service.GoalReader;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.exception.StepErrorStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepService {

    private final StepRepository stepRepository;
    private final GoalActivityRepository activityRepository;
    private final GoalReader goalReader;
    private final AiService aiService;

    /** 더 작게 (S-04) 🤖. 같은 걸음 번호 유지, 최대 5회 또는 AI 판단까지 */
    @Transactional
    public GoalResponse.Detail shrink(User user, Long stepId) {
        Step step = findActionableStep(user, stepId);
        if (!step.canShrinkMore()) {
            throw new GeneralException(StepErrorStatus.CANNOT_SHRINK_MORE);
        }
        Goal goal = step.getGoal();

        List<String> shrunkSteps = stepRepository
                .findAllByGoalAndSequenceOrderByShrinkLevelAsc(goal, step.getSequence())
                .stream()
                .map(Step::getContent)
                .toList();

        AiStepResult ai = aiService.shrinkStep(goal.getContent(), step.getEnergyLevel(), goal.getBurdenLevel(),
                step.getContent(), shrunkSteps);

        step.markShrunk();
        Step smaller = stepRepository.save(Step.shrunkFrom(step, ai.step(), ai.canShrinkMore(), ai.isLastStep()));
        activityRepository.save(GoalActivity.of(goal, step, ActivityType.SHRINK));

        return GoalResponse.Detail.of(goal, goalReader.countTried(goal), smaller);
    }

    /**
     * 해봤어요 (S-04). 시도 기록만 저장하고 AI 는 부르지 않는다.
     * 응답 isLastStep=true 면 마무리 제안(완료 → 목표 종료 API / 더 이어가기 → 다음 걸음 받기), false 면 S-04-3.
     */
    @Transactional
    public GoalResponse.Tried tryStep(User user, Long stepId) {
        Step step = findActionableStep(user, stepId);
        Goal goal = step.getGoal();

        step.markTried();
        activityRepository.save(GoalActivity.of(goal, step, ActivityType.TRY));

        return new GoalResponse.Tried(goal.getId(), step.getId(), goalReader.countTried(goal), step.isLastStep());
    }

    /** 내 목표의 행동이고, 목표가 진행 중이고, 아직 처리 안 된(SUGGESTED) 행동만 통과 */
    private Step findActionableStep(User user, Long stepId) {
        Step step = stepRepository.findById(stepId)
                .filter(s -> s.getGoal().isOwnedBy(user))
                .orElseThrow(() -> new GeneralException(StepErrorStatus.NOT_FOUND));
        if (step.getGoal().getStatus() != GoalStatus.IN_PROGRESS) {
            throw new GeneralException(GoalErrorStatus.NOT_IN_PROGRESS);
        }
        if (!step.isSuggested()) {
            throw new GeneralException(StepErrorStatus.ALREADY_HANDLED);
        }
        return step;
    }
}

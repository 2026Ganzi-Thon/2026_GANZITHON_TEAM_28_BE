package com.team28.domain.goal.service;

import com.team28.domain.activity.entity.GoalActivity;
import com.team28.domain.activity.enums.ActivityType;
import com.team28.domain.activity.repository.GoalActivityRepository;
import com.team28.domain.goal.dto.GoalRequest;
import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalEndType;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.goal.exception.GoalErrorStatus;
import com.team28.domain.goal.repository.GoalRepository;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.exception.StepErrorStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.step.service.TemporaryStep;
import com.team28.domain.user.entity.User;
import com.team28.global.apiPayload.exception.GeneralException;
import com.team28.global.common.TextNormalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목표 상태 규칙 (PM 확정 11/12)
 * - 진행 중 + 쉬어가는 중 합산 최대 3개, 진행 중은 1개
 * - 새 목표 생성·이어하기 시 기존 진행 중 목표는 자동으로 쉬어가기
 * - 완료·중단된 목표는 개수에 포함하지 않음
 *
 * AI 연결은 AI 담당자가 TODO(AI 연결) 자리에 붙입니다. 지금은 TemporaryStep 의 임시 문장을 씁니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    public static final int MAX_ACTIVE_GOALS = 3;
    private static final List<GoalStatus> ACTIVE_STATUSES = List.of(GoalStatus.IN_PROGRESS, GoalStatus.RESTING);

    private final GoalRepository goalRepository;
    private final StepRepository stepRepository;
    private final GoalActivityRepository activityRepository;
    private final GoalReader goalReader;

    /** 홈 (S-02): 진행 중 목표 1개 + 쉬어가는 목표 목록 */
    public GoalResponse.Home getHome(User user) {
        GoalResponse.Detail inProgress = goalRepository.findByUserAndStatus(user, GoalStatus.IN_PROGRESS)
                .map(goalReader::toDetail)
                .orElse(null);

        List<GoalResponse.RestingGoal> resting = goalRepository
                .findAllByUserAndStatusInOrderByUpdatedAtDesc(user, List.of(GoalStatus.RESTING))
                .stream()
                .map(goal -> new GoalResponse.RestingGoal(goal.getId(), goal.getContent(),
                        goalReader.countTried(goal), goal.getRestedAt()))
                .toList();

        long activeCount = goalRepository.countByUserAndStatusIn(user, ACTIVE_STATUSES);
        return new GoalResponse.Home(inProgress, resting, activeCount, MAX_ACTIVE_GOALS);
    }

    /** 목표 생성 (S-03) + 첫 걸음. 위기 신호면 저장하지 않음 */
    @Transactional
    public GoalCreateResult create(User user, GoalRequest.Create request) {
        if (goalRepository.countByUserAndStatusIn(user, ACTIVE_STATUSES) >= MAX_ACTIVE_GOALS) {
            throw new GeneralException(GoalErrorStatus.LIMIT_EXCEEDED);
        }

        String content = TextNormalizer.clean(request.content());
        // TODO(AI 연결): 목표·에너지·부담으로 위기 감지 + 첫 걸음 생성
        //  - 위기 신호면 아무것도 저장하지 말고 return GoalCreateResult.ofCrisis();
        //  - AI 가 준 행동 문장, 더 작게 가능 여부, 마지막 걸음 여부를 아래 Step.next(...) 에 넣기
        String firstStep = TemporaryStep.first(content);

        restCurrentInProgress(user);

        Goal goal = goalRepository.save(
                Goal.create(user, content, request.energyLevel(), request.burdenLevel()));
        Step first = stepRepository.save(
                Step.next(goal, 1, request.energyLevel(), firstStep, true, false));

        return GoalCreateResult.of(GoalResponse.Detail.of(goal, 0, first));
    }

    /** 목표 상세 (현재 행동, 해본 횟수) */
    public GoalResponse.Detail getDetail(User user, Long goalId) {
        return goalReader.toDetail(goalReader.findOwnedGoal(user, goalId));
    }

    /** 다음 걸음 받기 (S-04-3). 처음 고른 에너지 ±1 */
    @Transactional
    public GoalResponse.Detail nextStep(User user, Long goalId, GoalRequest.NextStep request) {
        Goal goal = goalReader.findOwnedGoal(user, goalId);
        requireInProgress(goal);
        if (goalReader.findCurrentStep(goal) != null) {
            throw new GeneralException(StepErrorStatus.CURRENT_STEP_EXISTS);
        }
        int energy = request.energy();
        if (Math.abs(energy - goal.getEnergyLevel()) > 1) {
            throw new GeneralException(GoalErrorStatus.ENERGY_OUT_OF_RANGE);
        }

        int nextSequence = stepRepository.findFirstByGoalOrderBySequenceDescIdDesc(goal)
                .map(step -> step.getSequence() + 1)
                .orElse(1);
        // TODO(AI 연결): 다시 고른 에너지 + 최근 해본 행동으로 다음 걸음 생성
        //  - 최근 해본 행동: stepRepository.findTop5ByGoalAndStatusOrderByTriedAtDesc(goal, StepStatus.TRIED)
        //  - AI 가 준 행동 문장, 더 작게 가능 여부, 마지막 걸음 여부를 아래 Step.next(...) 에 넣기
        String nextContent = TemporaryStep.next(goal.getContent(), nextSequence);
        Step step = stepRepository.save(
                Step.next(goal, nextSequence, energy, nextContent, true, false));

        return GoalResponse.Detail.of(goal, goalReader.countTried(goal), step);
    }

    /** 쉬어가기 (S-05) */
    @Transactional
    public GoalResponse.Status rest(User user, Long goalId) {
        Goal goal = goalReader.findOwnedGoal(user, goalId);
        if (goal.getStatus() != GoalStatus.IN_PROGRESS) {
            throw new GeneralException(GoalErrorStatus.INVALID_STATE);
        }
        goal.rest();
        activityRepository.save(GoalActivity.of(goal, ActivityType.REST));
        return GoalResponse.Status.from(goal);
    }

    /** 이어하기 (S-02-1). 기존 진행 중 목표는 자동으로 쉬어가기 */
    @Transactional
    public GoalResponse.Detail resume(User user, Long goalId) {
        Goal goal = goalReader.findOwnedGoal(user, goalId);
        if (goal.getStatus() != GoalStatus.RESTING) {
            throw new GeneralException(GoalErrorStatus.INVALID_STATE);
        }
        restCurrentInProgress(user);
        goal.resume();
        activityRepository.save(GoalActivity.of(goal, ActivityType.RESUME));
        return goalReader.toDetail(goal);
    }

    /** 목표 종료 (S-07): 완료 또는 그만두기 */
    @Transactional
    public GoalResponse.Status end(User user, Long goalId, GoalRequest.End request) {
        Goal goal = goalReader.findOwnedGoal(user, goalId);
        if (!goal.getStatus().isActive()) {
            throw new GeneralException(GoalErrorStatus.INVALID_STATE);
        }
        if (request.type() == GoalEndType.COMPLETED) {
            goal.complete();
            activityRepository.save(GoalActivity.of(goal, ActivityType.COMPLETE));
        } else {
            goal.stop();
            activityRepository.save(GoalActivity.of(goal, ActivityType.STOP));
        }
        return GoalResponse.Status.from(goal);
    }

    private void restCurrentInProgress(User user) {
        goalRepository.findByUserAndStatus(user, GoalStatus.IN_PROGRESS).ifPresent(current -> {
            current.rest();
            activityRepository.save(GoalActivity.of(current, ActivityType.REST));
        });
    }

    private void requireInProgress(Goal goal) {
        if (goal.getStatus() != GoalStatus.IN_PROGRESS) {
            throw new GeneralException(GoalErrorStatus.NOT_IN_PROGRESS);
        }
    }
}

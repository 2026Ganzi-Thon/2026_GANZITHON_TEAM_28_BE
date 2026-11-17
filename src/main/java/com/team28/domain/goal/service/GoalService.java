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
import com.team28.domain.step.enums.StepStatus;
import com.team28.domain.step.exception.StepErrorStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.user.entity.User;
import com.team28.domain.ai.exception.AiErrorStatus;
import com.team28.domain.ai.service.AiStepClient;
import com.team28.domain.ai.dto.AiStepResult;
import com.team28.global.apiPayload.exception.GeneralException;
import com.team28.global.common.TextNormalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 목표 상태 규칙 (PM 확정 11/12)
 * - 진행 중 + 쉬어가는 중 합산 최대 3개, 진행 중은 1개
 * - 새 목표 생성·이어하기 시 기존 진행 중 목표는 자동으로 쉬어가기
 * - 완료·중단된 목표는 개수에 포함하지 않음
 *
 * AI 서버 호출(create, nextStep)은 DB 트랜잭션 밖에서 한다.
 * AI 응답이 길어져도 DB 연결을 잡고 있지 않도록, "읽고 검증 → AI 호출 → 쓰기(다시 검증)" 순서로 나눈다.
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
    private final AiStepClient aiStepClient;
    private final TransactionTemplate tx;

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GoalCreateResult create(User user, GoalRequest.Create request) {
        String content = TextNormalizer.clean(request.content());

        // 1) 개수 제한은 AI 호출 전에 먼저 확인 (불필요한 AI 호출 방지)
        tx.executeWithoutResult(status -> requireGoalSlot(user));

        // 2) AI 호출 (트랜잭션 밖). 위기 신호면 아무것도 저장하지 않는다.
        AiStepResult ai = aiStepClient.first(content, request.energyLevel(), request.burdenLevel());
        if (ai.crisis()) {
            return GoalCreateResult.ofCrisis();
        }

        // 3) 저장. AI 를 기다리는 동안 상태가 바뀌었을 수 있어 개수를 다시 확인한다.
        return tx.execute(status -> {
            requireGoalSlot(user);
            restCurrentInProgress(user);

            Goal goal = goalRepository.save(
                    Goal.create(user, content, request.energyLevel(), request.burdenLevel()));
            Step first = stepRepository.save(
                    Step.next(goal, 1, request.energyLevel(), ai.step(), ai.reason(), ai.canShrinkMore(), false));
            return GoalCreateResult.of(GoalResponse.Detail.of(goal, 0, first));
        });
    }

    /** 목표 상세 (현재 행동, 해본 횟수) */
    public GoalResponse.Detail getDetail(User user, Long goalId) {
        return goalReader.toDetail(goalReader.findOwnedGoal(user, goalId));
    }

    /** 다음 걸음 받기 (S-04-3). 처음 고른 에너지 ±1 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GoalResponse.Detail nextStep(User user, Long goalId, GoalRequest.NextStep request) {
        // 1) 읽기·검증, AI 에 보낼 값 모으기
        NextStepPlan plan = tx.execute(status -> {
            Goal goal = goalReader.findOwnedGoal(user, goalId);
            requireInProgress(goal);
            if (goalReader.findCurrentStep(goal) != null) {
                throw new GeneralException(StepErrorStatus.CURRENT_STEP_EXISTS);
            }
            int energy = request.energy();
            if (Math.abs(energy - goal.getEnergyLevel()) > 1) {
                throw new GeneralException(GoalErrorStatus.ENERGY_OUT_OF_RANGE);
            }

            Step last = stepRepository.findFirstByGoalOrderBySequenceDescIdDesc(goal).orElse(null);
            int nextSequence = last == null ? 1 : last.getSequence() + 1;
            int previousEnergy = last == null ? goal.getEnergyLevel() : last.getEnergyLevel();
            List<String> recentTries = stepRepository
                    .findTop5ByGoalAndStatusOrderByTriedAtDesc(goal, StepStatus.TRIED).stream()
                    .map(Step::getContent)
                    .toList();

            return new NextStepPlan(
                    new AiStepClient.NextCommand(
                            goal.getContent(),
                            energy,
                            goal.getBurdenLevel(),
                            last == null ? goal.getContent() : last.getContent(),
                            energyChange(previousEnergy, energy),
                            (int) goalReader.countTried(goal),
                            stepRepository.existsByGoalAndLastStepTrue(goal),
                            recentTries),
                    nextSequence);
        });

        // 2) AI 호출 (트랜잭션 밖). 다음 걸음에서는 위기 판정을 쓰지 않으므로 위기 응답은 비정상 응답으로 본다.
        AiStepResult ai = aiStepClient.next(plan.command());
        if (ai.crisis()) {
            throw new GeneralException(AiErrorStatus.BAD_RESPONSE);
        }

        // 3) 저장. 기다리는 동안 다른 요청이 걸음을 만들었을 수 있어 다시 확인한다.
        return tx.execute(status -> {
            Goal goal = goalReader.findOwnedGoal(user, goalId);
            requireInProgress(goal);
            if (goalReader.findCurrentStep(goal) != null) {
                throw new GeneralException(StepErrorStatus.CURRENT_STEP_EXISTS);
            }
            Step step = stepRepository.save(Step.next(
                    goal, plan.sequence(), plan.command().energy(), ai.step(), ai.reason(), ai.canShrinkMore(), ai.lastStep()));
            return GoalResponse.Detail.of(goal, goalReader.countTried(goal), step);
        });
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

    private void requireGoalSlot(User user) {
        if (goalRepository.countByUserAndStatusIn(user, ACTIVE_STATUSES) >= MAX_ACTIVE_GOALS) {
            throw new GeneralException(GoalErrorStatus.LIMIT_EXCEEDED);
        }
    }

    /** 바로 앞 걸음에서 고른 에너지와 비교한 변화. AI 프롬프트의 "그대로예요/조금 생겼어요/더 지쳤어요" */
    private static String energyChange(int previous, int current) {
        if (current > previous) {
            return "UP";
        }
        return current < previous ? "DOWN" : "SAME";
    }

    private record NextStepPlan(AiStepClient.NextCommand command, int sequence) {
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

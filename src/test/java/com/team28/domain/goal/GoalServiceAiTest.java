package com.team28.domain.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team28.domain.activity.repository.GoalActivityRepository;
import com.team28.domain.goal.dto.GoalRequest;
import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.goal.exception.GoalErrorStatus;
import com.team28.domain.goal.repository.GoalRepository;
import com.team28.domain.goal.service.GoalCreateResult;
import com.team28.domain.goal.service.GoalReader;
import com.team28.domain.goal.service.GoalService;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.enums.StepStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.user.entity.User;
import com.team28.global.ai.AiErrorStatus;
import com.team28.global.ai.AiStepClient;
import com.team28.global.ai.AiStepResult;
import com.team28.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class GoalServiceAiTest {

    private GoalRepository goalRepository;
    private StepRepository stepRepository;
    private GoalReader goalReader;
    private AiStepClient ai;
    private GoalService service;
    private User user;

    @BeforeEach
    void setUp() {
        goalRepository = mock(GoalRepository.class);
        stepRepository = mock(StepRepository.class);
        goalReader = mock(GoalReader.class);
        ai = mock(AiStepClient.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new GoalService(goalRepository, stepRepository, mock(GoalActivityRepository.class),
                goalReader, ai, new TransactionTemplate(tm));
        user = User.create("tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(goalRepository.save(any(Goal.class))).thenAnswer(i -> i.getArgument(0));
        when(stepRepository.save(any(Step.class))).thenAnswer(i -> i.getArgument(0));
        when(goalRepository.findByUserAndStatus(any(), any())).thenReturn(Optional.empty());
    }

    private GoalRequest.Create createRequest() {
        return new GoalRequest.Create("  방 청소하기 ", 2, 4);
    }

    @Test
    void create_saves_goal_and_ai_first_step() {
        when(ai.first("방 청소하기", 2, 4))
                .thenReturn(AiStepResult.of("책상 위 쓰레기 1개만 버리기", true, false, "이유"));

        GoalCreateResult result = service.create(user, createRequest());

        assertThat(result.crisis()).isFalse();
        GoalResponse.CurrentStep step = result.goal().currentStep();
        assertThat(step.content()).isEqualTo("책상 위 쓰레기 1개만 버리기");
        assertThat(step.canShrinkMore()).isTrue();
        assertThat(step.isLastStep()).isFalse();
        assertThat(step.sequence()).isEqualTo(1);
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void create_crisis_saves_nothing() {
        when(ai.first(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(AiStepResult.ofCrisis());

        GoalCreateResult result = service.create(user, createRequest());

        assertThat(result.crisis()).isTrue();
        assertThat(result.goal()).isNull();
        verify(goalRepository, never()).save(any());
        verify(stepRepository, never()).save(any());
    }

    @Test
    void create_over_limit_does_not_call_ai() {
        when(goalRepository.countByUserAndStatusIn(any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.create(user, createRequest()))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(GoalErrorStatus.LIMIT_EXCEEDED));
        verify(ai, never()).first(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void create_ai_failure_saves_nothing() {
        when(ai.first(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new GeneralException(AiErrorStatus.TIMEOUT));

        assertThatThrownBy(() -> service.create(user, createRequest()))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(AiErrorStatus.TIMEOUT));
        verify(goalRepository, never()).save(any());
    }

    private Goal goalWithEnergy(int energy) {
        Goal goal = Goal.create(user, "방 청소하기", energy, 4);
        ReflectionTestUtils.setField(goal, "status", GoalStatus.IN_PROGRESS);
        return goal;
    }

    @Test
    void next_step_sends_energy_change_history_and_saves_ai_step_with_last_flag() {
        Goal goal = goalWithEnergy(2);
        Step tried = Step.next(goal, 3, 2, "바닥 옷 줍기", true, false);
        when(goalReader.findOwnedGoal(user, 10L)).thenReturn(goal);
        when(goalReader.findCurrentStep(goal)).thenReturn(null);
        when(goalReader.countTried(goal)).thenReturn(3L);
        when(stepRepository.findFirstByGoalOrderBySequenceDescIdDesc(goal)).thenReturn(Optional.of(tried));
        when(stepRepository.findTop5ByGoalAndStatusOrderByTriedAtDesc(goal, StepStatus.TRIED))
                .thenReturn(List.of(tried));
        when(stepRepository.existsByGoalAndLastStepTrue(goal)).thenReturn(false);
        when(ai.next(any())).thenReturn(AiStepResult.of("책상 닦기", true, true, null));

        GoalResponse.Detail detail = service.nextStep(user, 10L, new GoalRequest.NextStep(3));

        ArgumentCaptor<AiStepClient.NextCommand> captor = ArgumentCaptor.forClass(AiStepClient.NextCommand.class);
        verify(ai).next(captor.capture());
        AiStepClient.NextCommand c = captor.getValue();
        assertThat(c.energyChange()).isEqualTo("UP");
        assertThat(c.completedSteps()).isEqualTo(3);
        assertThat(c.finishSuggested()).isFalse();
        assertThat(c.currentStep()).isEqualTo("바닥 옷 줍기");
        assertThat(c.recentTries()).containsExactly("바닥 옷 줍기");
        assertThat(detail.currentStep().content()).isEqualTo("책상 닦기");
        assertThat(detail.currentStep().isLastStep()).isTrue();
        assertThat(detail.currentStep().sequence()).isEqualTo(4);
        assertThat(detail.currentStep().energy()).isEqualTo(3);
    }

    @Test
    void next_step_with_existing_current_step_does_not_call_ai() {
        Goal goal = goalWithEnergy(2);
        when(goalReader.findOwnedGoal(user, 10L)).thenReturn(goal);
        when(goalReader.findCurrentStep(goal)).thenReturn(Step.next(goal, 1, 2, "x", true, false));

        assertThatThrownBy(() -> service.nextStep(user, 10L, new GoalRequest.NextStep(2)))
                .isInstanceOf(GeneralException.class);
        verify(ai, never()).next(any());
    }

    @Test
    void next_step_crisis_response_is_treated_as_bad_response() {
        Goal goal = goalWithEnergy(2);
        when(goalReader.findOwnedGoal(user, 10L)).thenReturn(goal);
        when(goalReader.findCurrentStep(goal)).thenReturn(null);
        when(stepRepository.findFirstByGoalOrderBySequenceDescIdDesc(goal)).thenReturn(Optional.empty());
        when(ai.next(any())).thenReturn(AiStepResult.ofCrisis());

        assertThatThrownBy(() -> service.nextStep(user, 10L, new GoalRequest.NextStep(2)))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        verify(stepRepository, never()).save(any());
    }
}

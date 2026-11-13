package com.team28.domain.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team28.domain.activity.repository.GoalActivityRepository;
import com.team28.domain.goal.dto.GoalResponse;
import com.team28.domain.goal.entity.Goal;
import com.team28.domain.goal.enums.GoalStatus;
import com.team28.domain.goal.service.GoalReader;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.enums.StepStatus;
import com.team28.domain.step.exception.StepErrorStatus;
import com.team28.domain.step.repository.StepRepository;
import com.team28.domain.step.service.StepService;
import com.team28.domain.user.entity.User;
import com.team28.domain.ai.exception.AiErrorStatus;
import com.team28.domain.ai.service.AiStepClient;
import com.team28.domain.ai.dto.AiStepResult;
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

class StepServiceAiTest {

    private StepRepository stepRepository;
    private AiStepClient ai;
    private StepService service;
    private User user;
    private Step current;

    @BeforeEach
    void setUp() {
        stepRepository = mock(StepRepository.class);
        ai = mock(AiStepClient.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new StepService(stepRepository, mock(GoalActivityRepository.class), mock(GoalReader.class),
                ai, new TransactionTemplate(tm));

        user = User.create("tester");
        ReflectionTestUtils.setField(user, "id", 1L);
        Goal goal = Goal.create(user, "방 청소하기", 2, 4);
        ReflectionTestUtils.setField(goal, "status", GoalStatus.IN_PROGRESS);
        current = Step.next(goal, 2, 2, "옷 1벌 줍기", true, true);
        when(stepRepository.findById(5L)).thenReturn(Optional.of(current));
        when(stepRepository.save(any(Step.class))).thenAnswer(i -> i.getArgument(0));
        when(stepRepository.findAllByGoalAndSequenceOrderByShrinkLevelAsc(any(), any()))
                .thenReturn(List.of(current));
        when(stepRepository.findTop5ByGoalAndStatusOrderByTriedAtDesc(any(), any())).thenReturn(List.of());
    }

    @Test
    void shrink_sends_history_and_saves_smaller_step_keeping_last_flag() {
        when(ai.shrink(any())).thenReturn(AiStepResult.of("옷 1벌 바라보기", false, true, null));

        GoalResponse.Detail detail = service.shrink(user, 5L);

        ArgumentCaptor<AiStepClient.ShrinkCommand> captor = ArgumentCaptor.forClass(AiStepClient.ShrinkCommand.class);
        verify(ai).shrink(captor.capture());
        AiStepClient.ShrinkCommand c = captor.getValue();
        assertThat(c.currentStep()).isEqualTo("옷 1벌 줍기");
        assertThat(c.shrinkHistory()).containsExactly("옷 1벌 줍기");
        assertThat(c.shrinkCount()).isZero();
        assertThat(c.lastStep()).isTrue();
        assertThat(c.energy()).isEqualTo(2);
        assertThat(c.burden()).isEqualTo(4);

        assertThat(current.getStatus()).isEqualTo(StepStatus.SHRUNK);
        GoalResponse.CurrentStep smaller = detail.currentStep();
        assertThat(smaller.content()).isEqualTo("옷 1벌 바라보기");
        assertThat(smaller.canShrinkMore()).isFalse();
        assertThat(smaller.shrinkLevel()).isEqualTo(1);
        assertThat(smaller.sequence()).isEqualTo(2);
        assertThat(smaller.isLastStep()).isTrue();
    }

    @Test
    void shrink_ai_failure_leaves_step_untouched() {
        when(ai.shrink(any())).thenThrow(new GeneralException(AiErrorStatus.BAD_RESPONSE));

        assertThatThrownBy(() -> service.shrink(user, 5L))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        assertThat(current.getStatus()).isEqualTo(StepStatus.SUGGESTED);
        verify(stepRepository, never()).save(any());
    }

    @Test
    void shrink_when_ai_said_cannot_shrink_does_not_call_ai() {
        ReflectionTestUtils.setField(current, "shrinkable", false);

        assertThatThrownBy(() -> service.shrink(user, 5L))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(StepErrorStatus.CANNOT_SHRINK_MORE));
        verify(ai, never()).shrink(any());
    }

    @Test
    void shrink_already_handled_step_does_not_call_ai() {
        current.markTried();

        assertThatThrownBy(() -> service.shrink(user, 5L)).isInstanceOf(GeneralException.class);
        verify(ai, never()).shrink(any());
    }
}

package com.team28.domain.step.repository;

import com.team28.domain.goal.entity.Goal;
import com.team28.domain.step.entity.Step;
import com.team28.domain.step.enums.StepStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepRepository extends JpaRepository<Step, Long> {

    /** 목표의 현재 행동 (SUGGESTED 1개, 없을 수 있음) */
    Optional<Step> findFirstByGoalAndStatus(Goal goal, StepStatus status);

    /** 해봤어요 횟수 */
    long countByGoalAndStatus(Goal goal, StepStatus status);

    /** 가장 최근 걸음 번호 확인용 */
    Optional<Step> findFirstByGoalOrderBySequenceDescIdDesc(Goal goal);

    /** AI 프롬프트용: 같은 걸음에서 줄여온 행동들, 최근 해본 행동들 */
    List<Step> findAllByGoalAndSequenceOrderByShrinkLevelAsc(Goal goal, Integer sequence);

    List<Step> findTop5ByGoalAndStatusOrderByTriedAtDesc(Goal goal, StepStatus status);

    /** 이미 마지막 걸음(마무리 제안)이 나온 목표인지. 목표당 1번만 제안한다. */
    boolean existsByGoalAndLastStepTrue(Goal goal);
}

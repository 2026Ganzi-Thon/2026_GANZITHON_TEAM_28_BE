package com.team28.domain.ai.service;

import com.team28.domain.ai.exception.AiErrorStatus;
import com.team28.domain.ai.dto.AiStepResult;
import java.util.List;

/**
 * AI 서버(/ai/v1/steps/*) 호출 창구. 실패하면 GeneralException(AiErrorStatus.*) 를 던진다.
 * DB 트랜잭션 안에서 부르지 않는다 (AI 응답이 느리면 DB 연결을 오래 잡기 때문).
 */
public interface AiStepClient {

    /** 목표 입력 → 위기 확인 + 첫 걸음 */
    AiStepResult first(String goal, int energy, int burden);

    /** 더 작게 */
    AiStepResult shrink(ShrinkCommand command);

    /** 다음 걸음 */
    AiStepResult next(NextCommand command);

    record ShrinkCommand(
            String goal,
            int energy,
            int burden,
            String currentStep,
            List<String> shrinkHistory,
            int shrinkCount,
            boolean lastStep,
            List<String> recentTries
    ) {
    }

    /** energyChange: SAME / UP / DOWN (바로 앞 걸음에서 고른 에너지와 비교) */
    record NextCommand(
            String goal,
            int energy,
            int burden,
            String currentStep,
            String energyChange,
            int completedSteps,
            boolean finishSuggested,
            List<String> recentTries
    ) {
    }
}

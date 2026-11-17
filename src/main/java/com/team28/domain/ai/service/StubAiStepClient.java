package com.team28.domain.ai.service;

import com.team28.domain.ai.dto.AiStepResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** app.ai.enabled=false 일 때 쓰는 가짜 AI. AI 서버 없이 BE·FE 를 로컬에서 돌릴 때만 사용한다. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "false")
public class StubAiStepClient implements AiStepClient {

    @Override
    public AiStepResult first(String goal, int energy, int burden) {
        return AiStepResult.of("[임시] '" + goal + "'을 위해 필요한 물건 하나 꺼내두기", true, false, "[임시] 지금 에너지에 맞춰 골랐어요");
    }

    @Override
    public AiStepResult shrink(ShrinkCommand c) {
        boolean more = c.shrinkCount() + 1 < 5;
        return AiStepResult.of("[임시] '" + c.currentStep() + "'의 첫 동작만 해보기", more, c.lastStep(), "[임시] 더 작게 나눠봤어요");
    }

    @Override
    public AiStepResult next(NextCommand c) {
        return AiStepResult.of("[임시] '" + c.goal() + "' " + (c.completedSteps() + 1) + "걸음째 행동", true, false, "[임시] 지금 에너지에 맞춰 골랐어요");
    }
}

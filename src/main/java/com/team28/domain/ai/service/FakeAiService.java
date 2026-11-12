package com.team28.domain.ai.service;

import com.team28.domain.ai.dto.AiStepResult;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 실제 AI 연결 전까지 쓰는 가짜 구현. 프론트 연결·API 테스트용.
 *
 * - 목표에 "죽고", "자살", "사라지고" 가 들어가면 위기 응답 (S-08 화면 테스트용)
 * - 다음 걸음은 해본 행동이 4개 이상이면 마지막 걸음으로 표시 (마무리 제안 테스트용)
 * - 더 작게는 2번 줄이면 canShrinkMore=false (S-04-2 테스트용)
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeAiService implements AiService {

    private static final List<String> CRISIS_WORDS = List.of("죽고", "자살", "사라지고");

    @Override
    public AiStepResult firstStep(String goal, int energy, int burden) {
        if (CRISIS_WORDS.stream().anyMatch(goal::contains)) {
            return AiStepResult.crisis();
        }
        return AiStepResult.of("'" + goal + "'을 위해 필요한 물건 하나 꺼내두기", true, false);
    }

    @Override
    public AiStepResult shrinkStep(String goal, int energy, int burden, String currentStep, List<String> shrunkSteps) {
        boolean canShrinkMore = shrunkSteps.size() < 2;
        return AiStepResult.of("'" + currentStep + "'의 첫 동작만 해보기", canShrinkMore, false);
    }

    @Override
    public AiStepResult nextStep(String goal, int energy, int burden, List<String> triedSteps) {
        boolean last = triedSteps.size() >= 4;
        String step = last
                ? "'" + goal + "' 마무리 상태 한 번 확인하기"
                : "'" + goal + "'을 위해 " + (triedSteps.size() + 1) + "분만 해보기";
        return AiStepResult.of(step, true, last);
    }
}

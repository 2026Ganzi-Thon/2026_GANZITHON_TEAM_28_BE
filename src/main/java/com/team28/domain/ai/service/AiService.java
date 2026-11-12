package com.team28.domain.ai.service;

import com.team28.domain.ai.dto.AiStepResult;
import java.util.List;

/**
 * AI 행동 생성 인터페이스. 목표·행동 API 는 이 인터페이스만 호출합니다.
 *
 * - 실제 구현(재원님 담당): app.ai.mode=real 일 때 등록되는 구현체를 만들고
 *   클래스에 @ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "real") 를 붙여주세요.
 * - 지금은 FakeAiService 가 고정 문장을 돌려줍니다 (app.ai.mode=fake, 기본값).
 * - 실패 시 GeneralException(AiErrorStatus.BAD_RESPONSE / TIMEOUT) 을 던져주세요.
 * - step 은 200자를 넘지 않게 해주세요. (넘으면 저장할 때 잘림)
 */
public interface AiService {

    /** 목표 생성 시 첫 걸음. 위기 신호가 있으면 isCrisis=true (step 은 null 가능) */
    AiStepResult firstStep(String goal, int energy, int burden);

    /**
     * '더 작게'. currentStep 보다 더 쉬운 행동 1개.
     * @param shrunkSteps 같은 걸음 안에서 지금까지 줄여온 행동들 (처음 행동 → 현재 행동 순서)
     */
    AiStepResult shrinkStep(String goal, int energy, int burden, String currentStep, List<String> shrunkSteps);

    /**
     * '다음 걸음 받기' (S-04-3).
     * @param energy     사용자가 다시 고른 에너지 1~4
     * @param triedSteps 최근에 해본 행동들 (최신순, 최대 5개)
     */
    AiStepResult nextStep(String goal, int energy, int burden, List<String> triedSteps);
}

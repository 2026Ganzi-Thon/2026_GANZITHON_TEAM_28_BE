package com.team28.domain.ai.dto;

/**
 * AI 응답.
 *
 * @param isCrisis      위기 신호 감지 여부 (목표 생성 시에만 사용)
 * @param step          제안 행동 (200자 이내)
 * @param canShrinkMore AI 가 보기에 더 작게 만들 수 있는지 (false 면 '더 작게' 비활성화)
 * @param isLastStep    이 행동이 목표의 마지막 걸음인지 (true 면 '해봤어요' 후 마무리 제안)
 */
public record AiStepResult(boolean isCrisis, String step, boolean canShrinkMore, boolean isLastStep) {

    public static AiStepResult crisis() {
        return new AiStepResult(true, null, false, false);
    }

    public static AiStepResult of(String step, boolean canShrinkMore, boolean isLastStep) {
        return new AiStepResult(false, step, canShrinkMore, isLastStep);
    }
}

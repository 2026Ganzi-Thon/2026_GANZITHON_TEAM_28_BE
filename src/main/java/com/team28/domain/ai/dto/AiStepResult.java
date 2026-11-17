package com.team28.domain.ai.dto;

/**
 * AI 서버가 돌려준 걸음 1개.
 * crisis=true 면 step 은 null 이고 아무것도 저장하지 않는다.
 * reason(이유 문구)은 steps.reason 에 저장한다 (V4). 없으면 null.
 */
public record AiStepResult(boolean crisis, String step, boolean canShrinkMore, boolean lastStep, String reason) {

    public static AiStepResult ofCrisis() {
        return new AiStepResult(true, null, false, false, null);
    }

    public static AiStepResult of(String step, boolean canShrinkMore, boolean lastStep, String reason) {
        return new AiStepResult(false, step, canShrinkMore, lastStep, reason);
    }
}

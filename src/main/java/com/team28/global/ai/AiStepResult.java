package com.team28.global.ai;

/**
 * AI 서버가 돌려준 걸음 1개.
 * crisis=true 면 step 은 null 이고 아무것도 저장하지 않는다.
 * reason(이유 문구)은 아직 저장하지 않는다 (팀 확인 후 steps 컬럼 추가 예정).
 */
public record AiStepResult(boolean crisis, String step, boolean canShrinkMore, boolean lastStep, String reason) {

    public static AiStepResult ofCrisis() {
        return new AiStepResult(true, null, false, false, null);
    }

    public static AiStepResult of(String step, boolean canShrinkMore, boolean lastStep, String reason) {
        return new AiStepResult(false, step, canShrinkMore, lastStep, reason);
    }
}

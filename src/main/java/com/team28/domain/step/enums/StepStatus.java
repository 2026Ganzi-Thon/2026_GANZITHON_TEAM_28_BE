package com.team28.domain.step.enums;

public enum StepStatus {
    /** 현재 제안된 행동 (목표당 최대 1개) */
    SUGGESTED,
    /** '더 작게'로 다른 행동으로 대체됨 */
    SHRUNK,
    /** '해봤어요' 완료 */
    TRIED
}

package com.team28.domain.goal.enums;

public enum GoalStatus {
    /** 진행 중 (사용자당 1개) */
    IN_PROGRESS,
    /** 쉬어가는 중 */
    RESTING,
    /** 완료 */
    COMPLETED,
    /** 중단 */
    STOPPED;

    /** 목표 3개 제한에 포함되는 상태인지 (진행 중 + 쉬어가는 중) */
    public boolean isActive() {
        return this == IN_PROGRESS || this == RESTING;
    }
}

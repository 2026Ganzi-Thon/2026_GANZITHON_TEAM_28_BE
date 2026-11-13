package com.team28.domain.goal.service;

import com.team28.domain.goal.dto.GoalResponse;

/** 목표 생성 결과. 위기 신호면 goal 은 null (저장하지 않음) */
public record GoalCreateResult(boolean crisis, GoalResponse.Detail goal) {

    public static GoalCreateResult ofCrisis() {
        return new GoalCreateResult(true, null);
    }

    public static GoalCreateResult of(GoalResponse.Detail goal) {
        return new GoalCreateResult(false, goal);
    }
}

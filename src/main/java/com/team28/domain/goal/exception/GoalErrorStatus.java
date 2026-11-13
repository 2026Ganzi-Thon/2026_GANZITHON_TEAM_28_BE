package com.team28.domain.goal.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GoalErrorStatus implements BaseStatus {

    NOT_FOUND(HttpStatus.NOT_FOUND, "GOAL404", "목표를 찾을 수 없습니다."),
    INVALID_STATE(HttpStatus.CONFLICT, "GOAL409", "지금 목표 상태에서는 할 수 없는 요청입니다."),
    LIMIT_EXCEEDED(HttpStatus.CONFLICT, "GOAL409_1", "목표는 진행 중과 쉬어가는 중을 합쳐 최대 3개까지 만들 수 있습니다."),
    NOT_IN_PROGRESS(HttpStatus.CONFLICT, "GOAL409_2", "진행 중인 목표에서만 할 수 있습니다. 쉬어가는 목표는 먼저 이어하기를 해주세요."),
    ENERGY_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "ENERGY400", "에너지는 처음 고른 값에서 1단계 위아래로만 고를 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.team28.domain.step.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StepErrorStatus implements BaseStatus {

    NOT_FOUND(HttpStatus.NOT_FOUND, "STEP404", "행동을 찾을 수 없습니다."),
    ALREADY_HANDLED(HttpStatus.CONFLICT, "STEP409", "이미 처리된 행동입니다."),
    CURRENT_STEP_EXISTS(HttpStatus.CONFLICT, "STEP409_1", "아직 해보지 않은 현재 행동이 있습니다."),
    CANNOT_SHRINK_MORE(HttpStatus.CONFLICT, "SHRINK409", "더 작게 만들 수 없는 행동입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

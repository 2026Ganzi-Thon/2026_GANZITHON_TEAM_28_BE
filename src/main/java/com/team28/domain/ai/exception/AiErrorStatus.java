package com.team28.domain.ai.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** AI 구현체(재원님 담당)에서 실패 시 던지는 에러. 프론트는 같은 요청으로 재시도. */
@Getter
@RequiredArgsConstructor
public enum AiErrorStatus implements BaseStatus {

    BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "AI502", "행동을 만들지 못했어요. 다시 시도해 주세요."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI504", "응답이 늦어지고 있어요. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

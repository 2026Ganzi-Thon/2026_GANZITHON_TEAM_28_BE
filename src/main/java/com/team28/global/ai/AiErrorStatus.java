package com.team28.global.ai;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** AI 서버 호출 실패. 프론트는 E-04(앗, 행동을 가져오다 멈췄어요) 모달을 띄우고 입력은 그대로 둔다. */
@Getter
@RequiredArgsConstructor
public enum AiErrorStatus implements BaseStatus {

    BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "AI502", "AI 응답을 처리하지 못했어요. 다시 시도해 주세요."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI503", "AI를 지금 사용할 수 없어요. 잠시 후 다시 시도해 주세요."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI504", "AI 응답이 늦어지고 있어요. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

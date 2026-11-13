package com.team28.domain.ai.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** AI 서버 호출 실패. 프론트는 E-04(앗, 행동을 가져오다 멈췄어요) 모달을 띄우고 입력은 그대로 둔다. */
@Getter
@RequiredArgsConstructor
public enum AiErrorStatus implements BaseStatus {

    /** AI 응답 형식 오류, AI 서버 5xx(502·503), 연결 실패. 1회 재시도 후에도 실패한 경우 (AI 명세: AI503 은 502 로 변환) */
    BAD_RESPONSE(HttpStatus.BAD_GATEWAY, "AI502", "AI 응답을 처리하지 못했어요. 다시 시도해 주세요."),
    /** 1회 재시도 후에도 응답 시간 초과 */
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI504", "AI 응답이 늦어지고 있어요. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

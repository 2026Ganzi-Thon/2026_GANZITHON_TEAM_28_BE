package com.team28.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseStatus {

    OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "생성에 성공했습니다."),

    /** 목표 생성 시 위기 신호 감지 → 목표를 저장하지 않고 위기 안내 화면(S-08)으로 */
    CRISIS(HttpStatus.OK, "CRISIS200", "도움이 필요할 수 있어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

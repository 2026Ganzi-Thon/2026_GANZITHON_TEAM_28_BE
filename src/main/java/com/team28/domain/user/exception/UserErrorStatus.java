package com.team28.domain.user.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorStatus implements BaseStatus {

    /** X-User-Id 헤더가 없거나 모르는 값 → 프론트는 저장된 userId 를 지우고 시작 화면으로 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "USER401", "사용자 정보를 찾을 수 없습니다. 다시 시작해 주세요."),
    NOTIFICATION_INVALID(HttpStatus.BAD_REQUEST, "USER400_1", "알림을 켜려면 시간과 요일을 하나 이상 골라야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

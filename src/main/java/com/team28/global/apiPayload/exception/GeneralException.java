package com.team28.global.apiPayload.exception;

import com.team28.global.apiPayload.code.BaseStatus;
import lombok.Getter;

/**
 * 서비스 로직에서 던지는 공통 예외
 * 사용 예: throw new GeneralException(ErrorStatus.RESOURCE_NOT_FOUND);
 */
@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus status;

    public GeneralException(BaseStatus status) {
        super(status.getMessage());
        this.status = status;
    }
}

package com.team28.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.team28.global.apiPayload.code.BaseStatus;
import com.team28.global.apiPayload.code.SuccessStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답의 공통 형식
 * { "isSuccess": true, "code": "COMMON200", "message": "...", "result": {...} }
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, SuccessStatus.OK.getCode(), SuccessStatus.OK.getMessage(), result);
    }

    public static <T> ApiResponse<T> of(BaseStatus status, T result) {
        return new ApiResponse<>(true, status.getCode(), status.getMessage(), result);
    }

    public static <T> ApiResponse<T> onFailure(BaseStatus status, T result) {
        return new ApiResponse<>(false, status.getCode(), status.getMessage(), result);
    }
}

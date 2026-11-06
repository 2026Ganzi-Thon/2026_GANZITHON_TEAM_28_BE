package com.team28.global.apiPayload.exception;

import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.apiPayload.code.ErrorStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 예외를 ApiResponse 형식으로 바꿔서 응답합니다.
 * Controller 에서 try-catch 로 에러 응답을 직접 만들지 마세요.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스에서 던진 GeneralException
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(GeneralException e) {
        return ResponseEntity
                .status(e.getStatus().getHttpStatus())
                .body(ApiResponse.onFailure(e.getStatus(), null));
    }

    // @Valid 검증 실패 → 어떤 필드가 왜 틀렸는지 result 에 담아서 응답
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity
                .status(ErrorStatus.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.VALIDATION_FAILED, errors));
    }

    // 그 밖의 예상하지 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(ErrorStatus.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.onFailure(ErrorStatus.INTERNAL_SERVER_ERROR, null));
    }
}

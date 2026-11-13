package com.team28.global.apiPayload.exception;

import com.team28.global.apiPayload.ApiResponse;
import com.team28.global.apiPayload.code.BaseStatus;
import com.team28.global.apiPayload.code.ErrorStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        return fail(e.getStatus());
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

    // JSON 형식 오류, enum 값 오타(예: type: "DONE") 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return fail(ErrorStatus.BAD_REQUEST);
    }

    // 쿼리 파라미터 누락, 경로 변수 타입 오류(예: /api/goals/abc)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadParameter(Exception e) {
        return fail(ErrorStatus.BAD_REQUEST);
    }

    // 없는 URL
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return fail(ErrorStatus.RESOURCE_NOT_FOUND);
    }

    // URL 은 맞는데 메서드가 다름 (예: GET 으로 해야 하는데 POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return fail(ErrorStatus.METHOD_NOT_ALLOWED);
    }

    // 그 밖의 예상하지 못한 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return fail(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> fail(BaseStatus status) {
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(ApiResponse.onFailure(status, null));
    }
}

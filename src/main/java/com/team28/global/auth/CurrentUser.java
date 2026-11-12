package com.team28.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 파라미터에 붙이면 X-User-Id 헤더로 찾은 User 가 들어옵니다.
 * 사용 예: public ApiResponse<...> me(@CurrentUser User user)
 * 헤더가 없거나 모르는 값이면 USER401 응답.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

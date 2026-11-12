package com.team28.global.auth;

import com.team28.domain.user.entity.User;
import com.team28.domain.user.exception.UserErrorStatus;
import com.team28.domain.user.repository.UserRepository;
import com.team28.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * X-User-Id 헤더(닉네임 입력 시 발급한 uuid)로 사용자를 찾아 @CurrentUser 파라미터에 넣어줍니다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            throw new GeneralException(UserErrorStatus.UNAUTHORIZED);
        }
        return userRepository.findByUuid(userId.trim())
                .orElseThrow(() -> new GeneralException(UserErrorStatus.UNAUTHORIZED));
    }
}

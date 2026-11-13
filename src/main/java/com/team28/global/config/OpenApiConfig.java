package com.team28.global.config;

import com.team28.global.auth.CurrentUser;
import com.team28.global.auth.CurrentUserArgumentResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import java.util.Arrays;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static {
        // @CurrentUser User 파라미터는 Swagger 요청 본문/쿼리로 보이지 않게 숨김
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Team28 API")
                        .description("무기력한 사람을 위한 초소형 행동 지원 서비스 API. "
                                + "POST /api/users 로 받은 userId 를 다른 요청의 X-User-Id 헤더에 넣으세요.")
                        .version("v2"));
    }

    /** @CurrentUser 를 쓰는 API 에만 Swagger 에 X-User-Id 헤더 입력칸을 추가 */
    @Bean
    public OperationCustomizer userIdHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean needsUser = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(CurrentUser.class));
            if (needsUser) {
                operation.addParametersItem(new HeaderParameter()
                        .name(CurrentUserArgumentResolver.USER_ID_HEADER)
                        .required(true)
                        .description("POST /api/users 응답의 userId")
                        .schema(new StringSchema()));
            }
            return operation;
        };
    }
}

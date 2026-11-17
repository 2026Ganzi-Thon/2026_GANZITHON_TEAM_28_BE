package com.team28.domain.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI 서버 연결 설정 (application.yml 의 app.ai).
 * enabled=false 면 AI 서버 없이 임시 문장(StubAiStepClient)을 돌려준다. 프론트가 로컬에서 BE 만 띄울 때 쓴다.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("http://localhost:8000") String baseUrl,
        @DefaultValue("") String internalKey,
        @DefaultValue("2s") Duration connectTimeout,
        // AI 서버 LLM 제한(6초)보다 약간 길게. 1회 재시도까지 합쳐 프론트 오류 모달(15초) 안에 끝나야 한다 (7초 x 2 = 14초)
        @DefaultValue("7s") Duration readTimeout
) {
}

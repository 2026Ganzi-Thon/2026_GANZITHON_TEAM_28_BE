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
        // AI 서버 LLM 제한(6초)보다 약간 길게
        @DefaultValue("7s") Duration readTimeout,
        // 재시도를 포함한 전체 시간 예산. 프론트 오류 모달(15초)보다 짧게 둔다.
        // 시간 초과는 재시도하지 않고, 그 밖의 실패는 남은 시간이 연결+읽기(9초) 이상일 때만 재시도한다.
        @DefaultValue("13s") Duration totalTimeout
) {
}

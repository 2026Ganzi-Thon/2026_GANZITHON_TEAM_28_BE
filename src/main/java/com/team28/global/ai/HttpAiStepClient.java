package com.team28.global.ai;

import com.team28.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * AI 서버 HTTP 호출. 사용자가 입력한 목표·행동 문장은 로그에 남기지 않고, requestId·경로·상태만 남긴다.
 * 요청 계약은 AI 레포 README 참고 (camelCase JSON, X-Internal-Key 헤더).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpAiStepClient implements AiStepClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;

    public HttpAiStepClient(AiProperties properties) {
        // JDK 클라이언트는 기본으로 HTTP/2(h2c) 업그레이드를 시도한다. uvicorn 은 이를 받지 못해 400 이 나므로 HTTP/1.1 로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-Internal-Key", properties.internalKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public AiStepResult first(String goal, int energy, int burden) {
        Map<String, Object> body = base(goal, energy, burden);
        return post("/ai/v1/steps/first", body);
    }

    @Override
    public AiStepResult shrink(ShrinkCommand c) {
        Map<String, Object> body = base(c.goal(), c.energy(), c.burden());
        body.put("currentStep", c.currentStep());
        body.put("shrinkHistory", c.shrinkHistory());
        body.put("shrinkCount", c.shrinkCount());
        body.put("isLastStep", c.lastStep());
        body.put("recentTries", c.recentTries());
        return post("/ai/v1/steps/shrink", body);
    }

    @Override
    public AiStepResult next(NextCommand c) {
        Map<String, Object> body = base(c.goal(), c.energy(), c.burden());
        body.put("currentStep", c.currentStep());
        if (c.energyChange() != null) {
            body.put("energyChange", c.energyChange());
        }
        body.put("completedSteps", c.completedSteps());
        body.put("finishSuggested", c.finishSuggested());
        body.put("recentTries", c.recentTries());
        return post("/ai/v1/steps/next", body);
    }

    private static Map<String, Object> base(String goal, int energy, int burden) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("goal", goal);
        body.put("energy", energy);
        body.put("burden", burden);
        return body;
    }

    private AiStepResult post(String path, Map<String, Object> body) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> envelope;
        try {
            envelope = restClient.post()
                    .uri(path)
                    .header("X-Request-Id", requestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (RestClientResponseException e) {
            log.warn("AI 서버 오류 응답 requestId={} path={} status={}", requestId, path, e.getStatusCode().value());
            throw new GeneralException(mapStatus(e.getStatusCode().value()));
        } catch (ResourceAccessException e) {
            boolean timeout = isTimeout(e);
            log.warn("AI 서버 호출 실패 requestId={} path={} timeout={}", requestId, path, timeout);
            throw new GeneralException(timeout ? AiErrorStatus.TIMEOUT : AiErrorStatus.UNAVAILABLE);
        } catch (RestClientException e) {
            // 응답 본문을 JSON 으로 읽지 못한 경우 등
            log.warn("AI 응답을 읽지 못함 requestId={} path={} cause={}", requestId, path, e.getClass().getSimpleName());
            throw new GeneralException(AiErrorStatus.BAD_RESPONSE);
        }
        return toResult(envelope, requestId, path);
    }

    @SuppressWarnings("unchecked")
    private AiStepResult toResult(Map<String, Object> envelope, String requestId, String path) {
        try {
            if (envelope == null || !(envelope.get("result") instanceof Map<?, ?> raw)) {
                throw new IllegalStateException("result 없음");
            }
            Map<String, Object> result = (Map<String, Object>) raw;
            if (Boolean.TRUE.equals(result.get("isCrisis")) || "CRISIS200".equals(envelope.get("code"))) {
                return AiStepResult.ofCrisis();
            }
            Object step = result.get("step");
            if (!(step instanceof String text) || text.isBlank()) {
                throw new IllegalStateException("step 없음");
            }
            String reason = result.get("reason") instanceof String r && !r.isBlank() ? r : null;
            return AiStepResult.of(
                    text.strip(),
                    Boolean.TRUE.equals(result.get("canShrinkMore")),
                    Boolean.TRUE.equals(result.get("isLastStep")),
                    reason);
        } catch (RuntimeException e) {
            log.warn("AI 응답 형식 오류 requestId={} path={} cause={}", requestId, path, e.getMessage());
            throw new GeneralException(AiErrorStatus.BAD_RESPONSE);
        }
    }

    private static AiErrorStatus mapStatus(int status) {
        return switch (status) {
            case 504 -> AiErrorStatus.TIMEOUT;
            case 503 -> AiErrorStatus.UNAVAILABLE;
            default -> AiErrorStatus.BAD_RESPONSE; // 502, 4xx(요청 형식 불일치) 등
        };
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
            if (t instanceof IOException && t.getMessage() != null && t.getMessage().contains("timed out")) {
                return true;
            }
        }
        return false;
    }
}

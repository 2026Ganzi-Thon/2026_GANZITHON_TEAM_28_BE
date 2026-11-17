package com.team28.domain.ai.service;

import com.team28.domain.ai.config.AiProperties;
import com.team28.domain.ai.dto.AiStepResult;
import com.team28.domain.ai.exception.AiErrorStatus;
import com.team28.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
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
 *
 * 재시도 규칙 (AI API명세서): AI 서버가 502·503 을 주거나, 연결 실패·응답 형식 오류면 1회 재시도한다.
 * 시간 초과(읽기 제한 초과, AI 504)는 재시도하지 않는다 (같은 시간이 또 걸려 프론트 15초를 넘기므로).
 * 4xx(내부 키 오류, 요청 검증 실패)도 백엔드 쪽 문제라 재시도하지 않는다.
 * 재시도는 전체 시간 예산(app.ai.total-timeout) 안에 한 번 더 끝날 수 있을 때만 한다.
 * 최종 실패는 시간 초과면 AI504, 그 외에는 AI502 (AI503 은 502 로 변환).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpAiStepClient implements AiStepClient {

    private static final int MAX_ATTEMPTS = 2;
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final Duration maxAttemptTime; // 한 번 호출이 걸릴 수 있는 최대 시간 (연결 + 읽기)
    private final Duration totalTimeout;

    public HttpAiStepClient(AiProperties properties) {
        this.maxAttemptTime = properties.connectTimeout().plus(properties.readTimeout());
        this.totalTimeout = properties.totalTimeout();
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

    /**
     * 1회 재시도를 포함한 호출. 같은 requestId 로 보내서 AI 서버 로그와 짝을 맞출 수 있다.
     * 시간 초과(읽기 제한·AI 504)는 다시 보내도 같은 시간이 걸리므로 재시도하지 않는다.
     * 그 밖의 실패는 한 번 더 보내되, 전체 시간 예산(totalTimeout)에서 한 번 더 걸릴 최대 시간이 남을 때만 한다.
     */
    private AiStepResult post(String path, Map<String, Object> body) {
        String requestId = UUID.randomUUID().toString();
        long startNanos = System.nanoTime();
        AiErrorStatus lastFailure = AiErrorStatus.BAD_RESPONSE;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(path, body, requestId);
            } catch (Retryable e) {
                lastFailure = e.status;
                log.warn("AI 호출 실패 requestId={} path={} attempt={}/{} reason={}",
                        requestId, path, attempt, MAX_ATTEMPTS, e.reason);
                if (!e.retryable) {
                    break;
                }
                Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
                if (elapsed.plus(maxAttemptTime).compareTo(totalTimeout) > 0) {
                    log.warn("AI 재시도 생략 (시간 예산 부족) requestId={} path={} elapsedMs={}",
                            requestId, path, elapsed.toMillis());
                    break;
                }
            }
        }
        throw new GeneralException(lastFailure);
    }

    private AiStepResult callOnce(String path, Map<String, Object> body, String requestId) {
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
            int status = e.getStatusCode().value();
            if (status == 504) { // AI 서버가 이미 LLM 시간을 다 쓴 뒤라 다시 보내도 같다
                throw new Retryable(AiErrorStatus.TIMEOUT, "status 504", false);
            }
            if (status >= 500) { // 502, 503 등
                throw new Retryable(AiErrorStatus.BAD_RESPONSE, "status " + status, true);
            }
            // 4xx: 내부 키·요청 형식 문제. 다시 보내도 같으므로 바로 실패
            log.warn("AI 서버가 요청을 거절함 requestId={} path={} status={}", requestId, path, status);
            throw new GeneralException(AiErrorStatus.BAD_RESPONSE);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new Retryable(AiErrorStatus.TIMEOUT, "timeout", false);
            }
            throw new Retryable(AiErrorStatus.BAD_RESPONSE, "connection", true);
        } catch (RestClientException e) {
            // 응답 본문을 JSON 으로 읽지 못한 경우 등
            throw new Retryable(AiErrorStatus.BAD_RESPONSE, e.getClass().getSimpleName(), true);
        }
        return toResult(envelope);
    }

    @SuppressWarnings("unchecked")
    private AiStepResult toResult(Map<String, Object> envelope) {
        if (envelope == null || !(envelope.get("result") instanceof Map<?, ?> raw)) {
            throw new Retryable(AiErrorStatus.BAD_RESPONSE, "result 없음", true);
        }
        Map<String, Object> result = (Map<String, Object>) raw;
        if (Boolean.TRUE.equals(result.get("isCrisis")) || "CRISIS200".equals(envelope.get("code"))) {
            return AiStepResult.ofCrisis();
        }
        Object step = result.get("step");
        if (!(step instanceof String text) || text.isBlank()) {
            throw new Retryable(AiErrorStatus.BAD_RESPONSE, "step 없음", true);
        }
        String reason = result.get("reason") instanceof String r && !r.isBlank() ? r : null;
        return AiStepResult.of(
                text.strip(),
                Boolean.TRUE.equals(result.get("canShrinkMore")),
                Boolean.TRUE.equals(result.get("isLastStep")),
                reason);
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

    /** AI 호출 실패(재시도 여부 포함). 사용자 입력은 담지 않는다. */
    private static final class Retryable extends RuntimeException {
        private final AiErrorStatus status;
        private final String reason;
        private final boolean retryable;

        private Retryable(AiErrorStatus status, String reason, boolean retryable) {
            super(reason, null, false, false);
            this.status = status;
            this.reason = reason;
            this.retryable = retryable;
        }
    }
}

package com.team28.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.team28.domain.ai.config.AiProperties;
import com.team28.domain.ai.dto.AiStepResult;
import com.team28.domain.ai.exception.AiErrorStatus;
import com.team28.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 실제 HTTP 서버(JDK)를 띄워 AI 서버 응답을 흉내 낸다. 응답은 호출 순서대로 쓰고, 마지막 응답을 반복한다. */
class HttpAiStepClientTest {

    private record Resp(int status, String body, long delayMs) {
        static Resp ok() {
            return new Resp(200, OK, 0);
        }

        static Resp status(int status) {
            return new Resp(status, "{}", 0);
        }
    }

    private static final String OK = """
            {"isSuccess":true,"code":"COMMON200","message":"ok",
             "result":{"isCrisis":false,"step":"책상 위 쓰레기 1개만 버리기","canShrinkMore":true,"reason":"작게 골랐어요","isLastStep":false}}""";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> seen = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpAiStepClient client(Resp... responses) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            int index = Math.min(calls.getAndIncrement(), responses.length - 1);
            Resp resp = responses[index];
            seen.set(exchange.getRequestURI().getPath()
                    + "|" + exchange.getRequestHeaders().getFirst("X-Internal-Key")
                    + "|" + exchange.getRequestHeaders().getFirst("X-Request-Id")
                    + "|" + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(resp.delayMs());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = resp.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(resp.status(), bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // 클라이언트가 먼저 끊은 경우
            } finally {
                exchange.close();
            }
        });
        server.start();
        return new HttpAiStepClient(new AiProperties(
                true, "http://127.0.0.1:" + server.getAddress().getPort(), "secret",
                Duration.ofSeconds(1), Duration.ofMillis(500)));
    }

    private static AiErrorStatus statusOf(Throwable t) {
        return (AiErrorStatus) ((GeneralException) t).getStatus();
    }

    @Test
    void first_success_sends_key_request_id_and_camel_case_body() throws Exception {
        HttpAiStepClient client = client(Resp.ok());

        AiStepResult result = client.first("방 청소하기", 2, 4);

        assertThat(result.crisis()).isFalse();
        assertThat(result.step()).isEqualTo("책상 위 쓰레기 1개만 버리기");
        assertThat(result.canShrinkMore()).isTrue();
        assertThat(result.lastStep()).isFalse();
        assertThat(result.reason()).isEqualTo("작게 골랐어요");
        assertThat(calls).hasValue(1);
        String[] parts = seen.get().split("\\|", 4);
        assertThat(parts[0]).isEqualTo("/ai/v1/steps/first");
        assertThat(parts[1]).isEqualTo("secret");
        assertThat(parts[2]).isNotBlank();
        assertThat(parts[3]).contains("\"goal\":\"방 청소하기\"", "\"energy\":2", "\"burden\":4");
    }

    @Test
    void shrink_and_next_send_their_fields() throws Exception {
        HttpAiStepClient client = client(Resp.ok());

        client.shrink(new AiStepClient.ShrinkCommand("g", 2, 3, "cur", List.of("a", "cur"), 1, true, List.of("t")));
        assertThat(seen.get()).contains("/ai/v1/steps/shrink", "\"currentStep\":\"cur\"", "\"shrinkCount\":1",
                "\"isLastStep\":true", "\"shrinkHistory\":[\"a\",\"cur\"]");

        client.next(new AiStepClient.NextCommand("g", 3, 3, "cur", "UP", 3, false, List.of()));
        assertThat(seen.get()).contains("/ai/v1/steps/next", "\"energyChange\":\"UP\"", "\"completedSteps\":3",
                "\"finishSuggested\":false");
    }

    @Test
    void crisis_response_has_no_step() throws Exception {
        HttpAiStepClient client = client(new Resp(200, """
                {"isSuccess":true,"code":"CRISIS200","message":"x","result":{"isCrisis":true,"step":null,"canShrinkMore":false}}""", 0));
        assertThat(client.first("g", 1, 1).crisis()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void retries_once_after_503_and_succeeds() throws Exception {
        HttpAiStepClient client = client(Resp.status(503), Resp.ok());
        assertThat(client.first("g", 1, 1).step()).isNotBlank();
        assertThat(calls).hasValue(2);
    }

    @Test
    void retries_once_after_malformed_body_and_succeeds() throws Exception {
        HttpAiStepClient client = client(new Resp(200, "not json", 0), Resp.ok());
        assertThat(client.first("g", 1, 1).step()).isNotBlank();
        assertThat(calls).hasValue(2);
    }

    @Test
    void ai_503_twice_becomes_ai502() throws Exception {
        HttpAiStepClient client = client(Resp.status(503));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        assertThat(calls).hasValue(2);
    }

    @Test
    void ai_504_twice_is_timeout() throws Exception {
        HttpAiStepClient client = client(Resp.status(504));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.TIMEOUT));
        assertThat(calls).hasValue(2);
    }

    @Test
    void slow_response_twice_is_timeout() throws Exception {
        HttpAiStepClient client = client(new Resp(200, OK, 1500));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.TIMEOUT));
    }

    @Test
    void client_errors_are_not_retried() throws Exception {
        HttpAiStepClient client = client(Resp.status(400));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        assertThat(calls).hasValue(1);
    }

    @Test
    void empty_step_twice_is_bad_response() throws Exception {
        HttpAiStepClient client = client(new Resp(200,
                "{\"result\":{\"isCrisis\":false,\"step\":\"  \",\"canShrinkMore\":true}}", 0));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        assertThat(calls).hasValue(2);
    }

    @Test
    void connection_refused_is_ai502() {
        HttpAiStepClient client = new HttpAiStepClient(new AiProperties(
                true, "http://127.0.0.1:1", "k", Duration.ofMillis(500), Duration.ofMillis(500)));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
    }
}

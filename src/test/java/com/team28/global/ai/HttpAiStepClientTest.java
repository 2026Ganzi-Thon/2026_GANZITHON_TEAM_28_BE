package com.team28.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.team28.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 실제 HTTP 서버(JDK)를 띄워 AI 서버 응답을 흉내 낸다. */
class HttpAiStepClientTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpAiStepClient client(int status, String body, long delayMs, AtomicReference<String> seen) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (seen != null) {
                seen.set(exchange.getRequestURI().getPath()
                        + "|" + exchange.getRequestHeaders().getFirst("X-Internal-Key")
                        + "|" + exchange.getRequestHeaders().getFirst("X-Request-Id")
                        + "|" + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
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

    private static final String OK = """
            {"isSuccess":true,"code":"COMMON200","message":"ok",
             "result":{"isCrisis":false,"step":"책상 위 쓰레기 1개만 버리기","canShrinkMore":true,"reason":"작게 골랐어요","isLastStep":false}}""";

    @Test
    void first_success_sends_key_request_id_and_camel_case_body() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        HttpAiStepClient client = client(200, OK, 0, seen);

        AiStepResult result = client.first("방 청소하기", 2, 4);

        assertThat(result.crisis()).isFalse();
        assertThat(result.step()).isEqualTo("책상 위 쓰레기 1개만 버리기");
        assertThat(result.canShrinkMore()).isTrue();
        assertThat(result.lastStep()).isFalse();
        assertThat(result.reason()).isEqualTo("작게 골랐어요");
        String[] parts = seen.get().split("\\|", 4);
        assertThat(parts[0]).isEqualTo("/ai/v1/steps/first");
        assertThat(parts[1]).isEqualTo("secret");
        assertThat(parts[2]).isNotBlank();
        assertThat(parts[3]).contains("\"goal\":\"방 청소하기\"", "\"energy\":2", "\"burden\":4");
    }

    @Test
    void shrink_and_next_send_their_fields() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        HttpAiStepClient client = client(200, OK, 0, seen);

        client.shrink(new AiStepClient.ShrinkCommand("g", 2, 3, "cur", List.of("a", "cur"), 1, true, List.of("t")));
        assertThat(seen.get()).contains("/ai/v1/steps/shrink", "\"currentStep\":\"cur\"", "\"shrinkCount\":1",
                "\"isLastStep\":true", "\"shrinkHistory\":[\"a\",\"cur\"]");

        client.next(new AiStepClient.NextCommand("g", 3, 3, "cur", "UP", 3, false, List.of()));
        assertThat(seen.get()).contains("/ai/v1/steps/next", "\"energyChange\":\"UP\"", "\"completedSteps\":3",
                "\"finishSuggested\":false");
    }

    @Test
    void crisis_response_has_no_step() throws Exception {
        HttpAiStepClient client = client(200, """
                {"isSuccess":true,"code":"CRISIS200","message":"x","result":{"isCrisis":true,"step":null,"canShrinkMore":false}}""",
                0, null);
        assertThat(client.first("g", 1, 1).crisis()).isTrue();
    }

    @Test
    void ai_503_is_unavailable_504_is_timeout_others_are_bad_response() throws Exception {
        assertThatThrownBy(() -> client(503, "{}", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.UNAVAILABLE));
        stop();
        assertThatThrownBy(() -> client(504, "{}", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.TIMEOUT));
        stop();
        assertThatThrownBy(() -> client(502, "{}", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        stop();
        assertThatThrownBy(() -> client(400, "{}", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
    }

    @Test
    void slow_response_is_timeout() throws Exception {
        HttpAiStepClient client = client(200, OK, 1500, null);
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.TIMEOUT));
    }

    @Test
    void connection_refused_is_unavailable() {
        HttpAiStepClient client = new HttpAiStepClient(new AiProperties(
                true, "http://127.0.0.1:1", "k", Duration.ofMillis(500), Duration.ofMillis(500)));
        assertThatThrownBy(() -> client.first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.UNAVAILABLE));
    }

    @Test
    void malformed_or_empty_step_is_bad_response() throws Exception {
        assertThatThrownBy(() -> client(200, "not json", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
        stop();
        assertThatThrownBy(() -> client(200,
                "{\"result\":{\"isCrisis\":false,\"step\":\"  \",\"canShrinkMore\":true}}", 0, null).first("g", 1, 1))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(AiErrorStatus.BAD_RESPONSE));
    }
}

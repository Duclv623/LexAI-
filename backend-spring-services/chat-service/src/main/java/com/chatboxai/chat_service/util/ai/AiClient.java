package com.chatboxai.chat_service.util.ai;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final int TOP_K = 5;

    private final RestClient http;

    public AiClient(
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.timeout-seconds}") long timeoutSeconds) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        // để rộng tay, llm trả lời mất vài giây, trong khi giá trị mặc định là chờ vô hạn
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        // cố ý dùng builder tĩnh, từ spring boot 4 không còn bean RestClient.Builder ở đây nữa
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public AiRagResponse ask(
            String bearerToken,
            String question,
            List<AiRagRequest.AiHistoryItem> history) {

        log.info("Hỏi ai-service ({} lượt lịch sử)", history.size());

        return http.post()
                .uri("/rag")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(new AiRagRequest(question, history.isEmpty() ? null : history, TOP_K))
                .retrieve()
                .body(AiRagResponse.class);
    }
}

package com.chatboxai.chat_service.chat.ai;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Gọi ai-service để sinh câu trả lời.
 *
 * TẠM THỜI ĐỒNG BỘ: request của user bị giữ suốt thời gian LLM chạy (3–10 giây).
 * Đây là bước trung gian để có demo chạy được; bản bất đồng bộ qua Kafka sẽ thay
 * chỗ này. Nhược điểm cần biết: một thread của chat-service bị chiếm suốt lượt gọi,
 * và LLM chậm bất thường sẽ đội lên thành lỗi timeout cho người dùng.
 */
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
        // Phải rộng tay: sinh câu trả lời bằng LLM thường mất vài giây, cá biệt hơn chục.
        // Để mặc định (vô hạn) thì ai-service treo là chat-service treo theo vĩnh viễn.
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        // Dùng RestClient.builder() tĩnh thay vì inject RestClient.Builder: Spring Boot 4
        // tách starter thành nhiều mảnh nhỏ, và autoconfig cấp bean Builder KHÔNG còn đi
        // kèm spring-boot-starter-webmvc nữa — inject vào là context chết lúc khởi động.
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * @param bearerToken token GỐC của người dùng, truyền tiếp sang ai-service.
     *                    ai-service tự verify lại bằng JWKS — zero-trust vẫn được giữ,
     *                    và danh tính người hỏi không bị mất khi đi qua chat-service.
     */
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

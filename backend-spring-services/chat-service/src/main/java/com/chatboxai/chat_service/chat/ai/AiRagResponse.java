package com.chatboxai.chat_service.chat.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * Kết quả từ ai-service.
 *
 * citations và retrievedChunks để kiểu JsonNode chứ không map thành class cụ thể:
 * chat-service không đọc nội dung bên trong, nó chỉ chuyển tiếp cho frontend.
 * Map chi tiết ra sẽ biến mọi thay đổi format bên ai-service thành lỗi biên dịch
 * ở đây, đổi lấy đúng con số không lợi ích.
 */
public record AiRagResponse(
        String answer,
        JsonNode citations,
        @JsonProperty("retrieved_chunks") JsonNode retrievedChunks,
        @JsonProperty("latency_ms") Integer latencyMs
) {
}

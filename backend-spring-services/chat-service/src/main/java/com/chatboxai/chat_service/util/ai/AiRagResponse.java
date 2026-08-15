package com.chatboxai.chat_service.util.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

// citations và retrievedChunks giữ nguyên kiểu JsonNode, chat-service chỉ chuyển tiếp chứ không đọc
public record AiRagResponse(
        String answer,
        JsonNode citations,
        @JsonProperty("retrieved_chunks") JsonNode retrievedChunks,
        @JsonProperty("latency_ms") Integer latencyMs
) {
}

package com.chatboxai.chat_service.util.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

// citations and retrievedChunks stay as JsonNode, chat-service only forwards them
public record AiRagResponse(
        String answer,
        JsonNode citations,
        @JsonProperty("retrieved_chunks") JsonNode retrievedChunks,
        @JsonProperty("latency_ms") Integer latencyMs
) {
}

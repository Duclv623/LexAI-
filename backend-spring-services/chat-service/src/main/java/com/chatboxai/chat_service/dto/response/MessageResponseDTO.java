package com.chatboxai.chat_service.dto.response;

import java.time.Instant;

import com.chatboxai.chat_service.entity.MessageRole;

import tools.jackson.databind.JsonNode;

public record MessageResponseDTO(
        Long id,
        Long conversationId,
        MessageRole role,
        String content,
        JsonNode citations,
        Integer latencyMs,
        Instant createdAt
) {
}

package com.chatboxai.chat_service.dto.response;

import tools.jackson.databind.JsonNode;

public record TurnResponseDTO(
        MessageResponseDTO userMessage,
        MessageResponseDTO assistantMessage,
        JsonNode retrievedChunks
) {
}

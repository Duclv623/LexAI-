package com.chatboxai.chat_service.dto.response;

import java.time.Instant;

public record ConversationResponseDTO(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        long messageCount
) {
}

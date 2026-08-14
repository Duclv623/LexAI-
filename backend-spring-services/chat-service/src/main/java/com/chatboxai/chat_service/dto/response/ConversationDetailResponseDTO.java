package com.chatboxai.chat_service.dto.response;

import java.time.Instant;
import java.util.List;

public record ConversationDetailResponseDTO(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponseDTO> messages
) {
}

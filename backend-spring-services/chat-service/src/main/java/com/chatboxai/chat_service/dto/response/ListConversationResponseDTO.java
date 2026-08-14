package com.chatboxai.chat_service.dto.response;

import java.util.List;

public record ListConversationResponseDTO(
        List<ConversationResponseDTO> conversations,
        PaginationDTO pagination
) {
}

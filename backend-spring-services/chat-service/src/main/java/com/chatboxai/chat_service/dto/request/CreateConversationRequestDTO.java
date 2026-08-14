package com.chatboxai.chat_service.dto.request;

import jakarta.validation.constraints.Size;

public record CreateConversationRequestDTO(
        @Size(max = 160, message = "Tiêu đề tối đa 160 ký tự")
        String title
) {
}

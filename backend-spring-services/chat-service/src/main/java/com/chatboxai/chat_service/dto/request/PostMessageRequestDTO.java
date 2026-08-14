package com.chatboxai.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// no role field on purpose, the server assigns USER so a client cannot forge assistant messages
public record PostMessageRequestDTO(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 8000, message = "Nội dung tối đa 8000 ký tự")
        String content
) {
}

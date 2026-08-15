package com.chatboxai.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// cố ý không có trường role, server tự gán USER để client không giả mạo được tin nhắn của trợ lý
public record PostMessageRequestDTO(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 8000, message = "Nội dung tối đa 8000 ký tự")
        String content
) {
}

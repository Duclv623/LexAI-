package com.chatboxai.chat_service.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body khi user gửi một tin nhắn.
 *
 * Chú ý: KHÔNG có trường role. Client chỉ được gửi nội dung, còn vai trò do server
 * gán cứng là USER. Nếu để client tự khai role thì họ tự chèn được tin nhắn giả
 * mạo ASSISTANT vào lịch sử hội thoại.
 */
public record PostMessageRequest(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 8000, message = "Nội dung tối đa 8000 ký tự")
        String content,

        // Chặn ngay ở đây thay vì để ai-service trả 500: nó chỉ nhận đúng hai giá trị này.
        @Pattern(regexp = "gemini|groq", message = "provider chỉ nhận 'gemini' hoặc 'groq'")
        String provider
) {
    public String providerOrDefault() {
        return provider == null || provider.isBlank() ? "gemini" : provider;
    }
}

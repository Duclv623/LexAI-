package com.chatboxai.chat_service.chat.dto;

import jakarta.validation.constraints.Size;

/** title để trống cũng được — service sẽ đặt tiêu đề mặc định. */
public record CreateConversationRequest(
        @Size(max = 160, message = "Tiêu đề tối đa 160 ký tự")
        String title
) {
}

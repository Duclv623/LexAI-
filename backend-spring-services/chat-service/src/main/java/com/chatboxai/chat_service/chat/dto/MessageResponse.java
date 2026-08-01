package com.chatboxai.chat_service.chat.dto;

import java.time.Instant;

import com.chatboxai.chat_service.chat.entity.Message;
import com.chatboxai.chat_service.chat.entity.MessageRole;

public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt());
    }
}

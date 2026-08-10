package com.chatboxai.chat_service.chat.dto;

import java.time.Instant;

import com.chatboxai.chat_service.chat.entity.Message;
import com.chatboxai.chat_service.chat.entity.MessageRole;

import tools.jackson.databind.JsonNode;

/**
 * Một tin nhắn trả ra API.
 *
 * citations là JsonNode chứ không phải chuỗi: trong DB nó lưu dạng chuỗi JSON thô,
 * nhưng API thì trả mảng thật để client khỏi phải JSON.parse lần nữa.
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        MessageRole role,
        String content,
        JsonNode citations,
        Integer latencyMs,
        Instant createdAt
) {
    public static MessageResponse from(Message message, JsonNode citations) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getLatencyMs(),
                message.getCreatedAt());
    }
}

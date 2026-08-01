package com.chatboxai.chat_service.chat.dto;

import java.time.Instant;

import com.chatboxai.chat_service.chat.entity.Conversation;

/**
 * Hội thoại ở dạng tóm tắt (dùng cho danh sách).
 *
 * Cố tình KHÔNG trả entity ra thẳng controller: entity mà lộ ra API thì mọi lần
 * đổi cột DB đều thành breaking change với client, và userId cũng rò ra ngoài
 * dù client không cần biết.
 */
public record ConversationResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}

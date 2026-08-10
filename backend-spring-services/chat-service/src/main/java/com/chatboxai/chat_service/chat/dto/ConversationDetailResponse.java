package com.chatboxai.chat_service.chat.dto;

import java.time.Instant;
import java.util.List;

import com.chatboxai.chat_service.chat.entity.Conversation;

/** Hội thoại kèm toàn bộ tin nhắn (dùng cho màn hình chi tiết). */
public record ConversationDetailResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages
) {
    public static ConversationDetailResponse of(Conversation conversation, List<MessageResponse> messages) {
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }
}

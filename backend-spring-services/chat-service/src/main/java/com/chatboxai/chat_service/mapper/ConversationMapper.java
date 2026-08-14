package com.chatboxai.chat_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.chatboxai.chat_service.dto.response.ConversationDetailResponseDTO;
import com.chatboxai.chat_service.dto.response.ConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.MessageResponseDTO;
import com.chatboxai.chat_service.entity.Conversation;

@Component
public class ConversationMapper {

    public static ConversationResponseDTO toResponse(Conversation conversation, long messageCount) {
        return new ConversationResponseDTO(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageCount);
    }

    public static ConversationDetailResponseDTO toDetailResponse(
            Conversation conversation, List<MessageResponseDTO> messages) {
        return new ConversationDetailResponseDTO(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages);
    }
}

package com.chatboxai.chat_service.mapper;

import org.springframework.stereotype.Component;

import com.chatboxai.chat_service.dto.response.MessageResponseDTO;
import com.chatboxai.chat_service.entity.Message;

import tools.jackson.databind.JsonNode;

@Component
public class MessageMapper {

    public static MessageResponseDTO toResponse(Message message, JsonNode citations) {
        return new MessageResponseDTO(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                citations,
                message.getLatencyMs(),
                message.getCreatedAt());
    }
}

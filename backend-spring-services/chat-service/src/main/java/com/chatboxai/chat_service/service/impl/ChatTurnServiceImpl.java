package com.chatboxai.chat_service.service.impl;

import org.springframework.stereotype.Service;

import com.chatboxai.chat_service.dto.request.PostMessageRequestDTO;
import com.chatboxai.chat_service.dto.response.TurnResponseDTO;
import com.chatboxai.chat_service.entity.Message;
import com.chatboxai.chat_service.mapper.MessageMapper;
import com.chatboxai.chat_service.service.ChatService;
import com.chatboxai.chat_service.service.ChatTurnService;
import com.chatboxai.chat_service.util.ai.AiClient;
import com.chatboxai.chat_service.util.ai.AiRagResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

// deliberately not @Transactional, the ai-service call sits between two short transactions
@Service
@RequiredArgsConstructor
public class ChatTurnServiceImpl implements ChatTurnService {

    private final ChatService chatService;
    private final AiClient aiClient;

    @Override
    public TurnResponseDTO send(
            String userId,
            String bearerToken,
            Long conversationId,
            PostMessageRequestDTO request) {

        String question = request.content().strip();

        ChatService.PreparedTurn prepared = chatService.beginTurn(userId, conversationId, question);

        AiRagResponse ai = aiClient.ask(bearerToken, question, prepared.history());

        String citationsJson = isEmpty(ai.citations()) ? null : ai.citations().toString();
        Message assistant = chatService.completeTurn(
                conversationId, ai.answer(), citationsJson, ai.latencyMs());

        return new TurnResponseDTO(
                MessageMapper.toResponse(prepared.userMessage(), null),
                MessageMapper.toResponse(assistant, ai.citations()),
                ai.retrievedChunks());
    }

    private static boolean isEmpty(JsonNode node) {
        return node == null || node.isNull() || node.isEmpty();
    }
}

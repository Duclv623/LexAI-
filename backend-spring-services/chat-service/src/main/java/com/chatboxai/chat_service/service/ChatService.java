package com.chatboxai.chat_service.service;

import java.util.List;

import com.chatboxai.chat_service.dto.request.CreateConversationRequestDTO;
import com.chatboxai.chat_service.dto.response.ConversationDetailResponseDTO;
import com.chatboxai.chat_service.dto.response.ConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.ListConversationResponseDTO;
import com.chatboxai.chat_service.entity.Message;
import com.chatboxai.chat_service.util.ai.AiRagRequest;

public interface ChatService {

    // một lượt hỏi đáp được chẻ làm hai giao dịch, để lời gọi ai-service không nằm trong giao dịch nào
    record PreparedTurn(Message userMessage, List<AiRagRequest.AiHistoryItem> history) {
    }

    ConversationResponseDTO create(String userId, CreateConversationRequestDTO request);

    ListConversationResponseDTO list(String userId, int page, int size);

    ConversationDetailResponseDTO detail(String userId, Long conversationId);

    void delete(String userId, Long conversationId);

    PreparedTurn beginTurn(String userId, Long conversationId, String question);

    Message completeTurn(Long conversationId, String answer, String citationsJson, Integer latencyMs);
}

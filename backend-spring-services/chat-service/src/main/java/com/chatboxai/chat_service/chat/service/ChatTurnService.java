package com.chatboxai.chat_service.chat.service;

import org.springframework.stereotype.Service;

import com.chatboxai.chat_service.chat.ai.AiClient;
import com.chatboxai.chat_service.chat.ai.AiRagResponse;
import com.chatboxai.chat_service.chat.dto.MessageResponse;
import com.chatboxai.chat_service.chat.dto.PostMessageRequest;
import com.chatboxai.chat_service.chat.dto.TurnResponse;
import com.chatboxai.chat_service.chat.entity.Message;

import tools.jackson.databind.JsonNode;

/**
 * Điều phối một lượt hỏi đáp: lưu câu hỏi → gọi ai-service → lưu câu trả lời.
 *
 * Lớp này CỐ Ý không mang @Transactional. Nó gọi ai-service ở giữa, và transaction
 * chỉ được mở bên trong hai method của ChatService — trước và sau lời gọi đó.
 * Nếu gộp cả ba bước vào một transaction thì mỗi câu hỏi sẽ giam một connection DB
 * suốt thời gian LLM chạy.
 */
@Service
public class ChatTurnService {

    private final ChatService chatService;
    private final AiClient aiClient;

    public ChatTurnService(ChatService chatService, AiClient aiClient) {
        this.chatService = chatService;
        this.aiClient = aiClient;
    }

    public TurnResponse send(
            String userId,
            String bearerToken,
            Long conversationId,
            PostMessageRequest request) {

        String question = request.content().strip();

        // 1) transaction ngắn: kiểm quyền sở hữu + lưu câu hỏi
        ChatService.PreparedTurn prepared = chatService.beginTurn(userId, conversationId, question);

        // 2) KHÔNG transaction: gọi LLM, mất vài giây
        AiRagResponse ai = aiClient.ask(bearerToken, question, prepared.history());

        // 3) transaction ngắn: lưu câu trả lời
        String citationsJson = isEmpty(ai.citations()) ? null : ai.citations().toString();
        Message assistant = chatService.completeTurn(
                conversationId, ai.answer(), citationsJson, ai.latencyMs());

        return new TurnResponse(
                MessageResponse.from(prepared.userMessage(), null),
                MessageResponse.from(assistant, ai.citations()),
                ai.retrievedChunks());
    }

    private static boolean isEmpty(JsonNode node) {
        return node == null || node.isNull() || node.isEmpty();
    }
}

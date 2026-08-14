package com.chatboxai.chat_service.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatboxai.chat_service.dao.repository.ConversationRepository;
import com.chatboxai.chat_service.dao.repository.MessageRepository;
import com.chatboxai.chat_service.dto.request.CreateConversationRequestDTO;
import com.chatboxai.chat_service.dto.response.ConversationDetailResponseDTO;
import com.chatboxai.chat_service.dto.response.ConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.MessageResponseDTO;
import com.chatboxai.chat_service.entity.Conversation;
import com.chatboxai.chat_service.entity.Message;
import com.chatboxai.chat_service.entity.MessageRole;
import com.chatboxai.chat_service.exception.ConversationNotFoundException;
import com.chatboxai.chat_service.mapper.ConversationMapper;
import com.chatboxai.chat_service.mapper.MessageMapper;
import com.chatboxai.chat_service.service.ChatService;
import com.chatboxai.chat_service.util.ai.AiRagRequest;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final String DEFAULT_TITLE = "Hội thoại mới";
    private static final int TITLE_MAX = 60;
    private static final int HISTORY_LIMIT = 10;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ConversationResponseDTO create(String userId, CreateConversationRequestDTO request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(hasText(request.title()) ? request.title().strip() : DEFAULT_TITLE);
        return ConversationMapper.toResponse(conversationRepository.save(conversation), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> list(String userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> ConversationMapper.toResponse(c, messageRepository.countByConversationId(c.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponseDTO detail(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        List<MessageResponseDTO> history = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(m -> MessageMapper.toResponse(m, parseCitations(m.getCitations())))
                .toList();
        return ConversationMapper.toDetailResponse(conversation, history);
    }

    @Override
    @Transactional
    public void delete(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        // delete children first, there is no @ManyToOne so jpa will not cascade
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public PreparedTurn beginTurn(String userId, Long conversationId, String question) {
        Conversation conversation = requireOwned(userId, conversationId);

        // collect history before saving the new question, otherwise it appears twice in the prompt
        List<Message> previous = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<AiRagRequest.AiHistoryItem> history = previous.stream()
                .skip(Math.max(0, previous.size() - HISTORY_LIMIT))
                .map(m -> new AiRagRequest.AiHistoryItem(m.getRole().name().toLowerCase(), m.getContent()))
                .toList();

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(question);
        Message saved = messageRepository.save(userMessage);

        conversation.touch();
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(summarize(question));
        }

        return new PreparedTurn(saved, history);
    }

    @Override
    @Transactional
    public Message completeTurn(Long conversationId, String answer, String citationsJson, Integer latencyMs) {
        Message reply = new Message();
        reply.setConversationId(conversationId);
        reply.setRole(MessageRole.ASSISTANT);
        reply.setContent(answer);
        reply.setCitations(citationsJson);
        reply.setLatencyMs(latencyMs);
        Message saved = messageRepository.save(reply);

        conversationRepository.findById(conversationId).ifPresent(Conversation::touch);
        return saved;
    }

    // the only way to load a conversation, ownership check is baked into the query
    private Conversation requireOwned(String userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    private JsonNode parseCitations(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (RuntimeException e) {
            log.warn("Không parse được citations đã lưu, bỏ qua: {}", e.getMessage());
            return null;
        }
    }

    private static String summarize(String content) {
        String oneLine = content.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= TITLE_MAX ? oneLine : oneLine.substring(0, TITLE_MAX - 1) + "…";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

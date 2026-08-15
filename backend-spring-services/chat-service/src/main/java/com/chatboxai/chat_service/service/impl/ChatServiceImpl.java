package com.chatboxai.chat_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatboxai.chat_service.dao.repository.ConversationMessageCount;
import com.chatboxai.chat_service.dao.repository.ConversationRepository;
import com.chatboxai.chat_service.dao.repository.MessageRepository;
import com.chatboxai.chat_service.dto.request.CreateConversationRequestDTO;
import com.chatboxai.chat_service.dto.response.ConversationDetailResponseDTO;
import com.chatboxai.chat_service.dto.response.ConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.ListConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.MessageResponseDTO;
import com.chatboxai.chat_service.entity.Conversation;
import com.chatboxai.chat_service.entity.Message;
import com.chatboxai.chat_service.entity.MessageRole;
import com.chatboxai.chat_service.exception.ConversationNotFoundException;
import com.chatboxai.chat_service.mapper.ConversationMapper;
import com.chatboxai.chat_service.mapper.MessageMapper;
import com.chatboxai.chat_service.service.ChatService;
import com.chatboxai.chat_service.util.PageableUtils;
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
    public ListConversationResponseDTO list(String userId, int page, int size) {
        Page<Conversation> found = conversationRepository.findByUserId(
                userId, PageableUtils.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));

        Map<Long, Long> counts = messageCounts(found.getContent());
        List<ConversationResponseDTO> conversations = found.getContent().stream()
                .map(c -> ConversationMapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();

        return new ListConversationResponseDTO(conversations, PageableUtils.toPagination(found));
    }

    // một truy vấn cho cả trang, không phải mỗi hội thoại một truy vấn
    private Map<Long, Long> messageCounts(List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = conversations.stream().map(Conversation::getId).toList();
        return messageRepository.countByConversationIdIn(ids).stream()
                .collect(Collectors.toMap(
                        ConversationMessageCount::getConversationId,
                        ConversationMessageCount::getTotal));
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
        // xoá bản ghi con trước, không có @ManyToOne nên jpa sẽ không tự cascade
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public PreparedTurn beginTurn(String userId, Long conversationId, String question) {
        Conversation conversation = requireOwned(userId, conversationId);

        // thu thập lịch sử trước khi lưu câu hỏi mới, nếu không câu hỏi sẽ xuất hiện hai lần trong prompt
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

    // lối duy nhất để nạp một hội thoại, việc kiểm tra quyền sở hữu đã nằm sẵn trong truy vấn
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

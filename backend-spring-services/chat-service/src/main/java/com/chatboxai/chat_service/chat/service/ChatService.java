package com.chatboxai.chat_service.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatboxai.chat_service.chat.dto.ConversationDetailResponse;
import com.chatboxai.chat_service.chat.dto.ConversationResponse;
import com.chatboxai.chat_service.chat.dto.CreateConversationRequest;
import com.chatboxai.chat_service.chat.dto.MessageResponse;
import com.chatboxai.chat_service.chat.dto.PostMessageRequest;
import com.chatboxai.chat_service.chat.entity.Conversation;
import com.chatboxai.chat_service.chat.entity.Message;
import com.chatboxai.chat_service.chat.entity.MessageRole;
import com.chatboxai.chat_service.chat.repository.ConversationRepository;
import com.chatboxai.chat_service.chat.repository.MessageRepository;

/**
 * Nghiệp vụ hội thoại.
 *
 * Quy ước xuyên suốt: MỌI phương thức công khai đều nhận userId làm tham số đầu tiên
 * và không có đường nào truy cập dữ liệu mà bỏ qua nó. userId đến từ claim "sub" của
 * JWT đã verify, do controller truyền xuống.
 */
@Service
public class ChatService {

    private static final String DEFAULT_TITLE = "Hội thoại mới";
    private static final int TITLE_MAX = 60;

    private final ConversationRepository conversations;
    private final MessageRepository messages;

    public ChatService(ConversationRepository conversations, MessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    @Transactional
    public ConversationResponse create(String userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(hasText(request.title()) ? request.title().strip() : DEFAULT_TITLE);
        return ConversationResponse.from(conversations.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse detail(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        return ConversationDetailResponse.from(
                conversation,
                messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId()));
    }

    @Transactional
    public MessageResponse postMessage(String userId, Long conversationId, PostMessageRequest request) {
        Conversation conversation = requireOwned(userId, conversationId);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        // Role do server gán, không lấy từ body — xem chú thích ở PostMessageRequest.
        message.setRole(MessageRole.USER);
        message.setContent(request.content().strip());
        Message saved = messages.save(message);

        conversation.touch();
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(summarize(message.getContent()));
        }
        // Không cần gọi conversations.save(): trong transaction, entity lấy từ repository
        // đang được JPA quản lý nên mọi thay đổi tự được flush xuống DB (dirty checking).

        return MessageResponse.from(saved);
    }

    @Transactional
    public void delete(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        // Xoá con trước rồi mới xoá cha: không có @ManyToOne nên JPA không tự cascade.
        messages.deleteByConversationId(conversation.getId());
        conversations.delete(conversation);
    }

    /** Cửa duy nhất để lấy một hội thoại — đã gộp sẵn việc kiểm tra quyền sở hữu. */
    private Conversation requireOwned(String userId, Long conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /** Lấy câu hỏi đầu tiên làm tiêu đề cho dễ nhận ra trong danh sách. */
    private static String summarize(String content) {
        String oneLine = content.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= TITLE_MAX ? oneLine : oneLine.substring(0, TITLE_MAX - 1) + "…";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

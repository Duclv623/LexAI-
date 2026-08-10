package com.chatboxai.chat_service.chat.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chatboxai.chat_service.chat.ai.AiRagRequest;
import com.chatboxai.chat_service.chat.dto.ConversationDetailResponse;
import com.chatboxai.chat_service.chat.dto.ConversationResponse;
import com.chatboxai.chat_service.chat.dto.CreateConversationRequest;
import com.chatboxai.chat_service.chat.dto.MessageResponse;
import com.chatboxai.chat_service.chat.entity.Conversation;
import com.chatboxai.chat_service.chat.entity.Message;
import com.chatboxai.chat_service.chat.entity.MessageRole;
import com.chatboxai.chat_service.chat.repository.ConversationRepository;
import com.chatboxai.chat_service.chat.repository.MessageRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Nghiệp vụ hội thoại.
 *
 * Quy ước xuyên suốt: MỌI phương thức công khai đều nhận userId làm tham số đầu tiên
 * và không có đường nào truy cập dữ liệu mà bỏ qua nó. userId đến từ claim "sub" của
 * JWT đã verify, do controller truyền xuống.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String DEFAULT_TITLE = "Hội thoại mới";
    private static final int TITLE_MAX = 60;
    /** Số tin nhắn cũ gửi kèm sang ai-service làm ngữ cảnh. */
    private static final int HISTORY_LIMIT = 10;

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ObjectMapper objectMapper;

    public ChatService(
            ConversationRepository conversations,
            MessageRepository messages,
            ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConversationResponse create(String userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setTitle(hasText(request.title()) ? request.title().strip() : DEFAULT_TITLE);
        return ConversationResponse.from(conversations.save(conversation), 0);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(String userId) {
        // N+1: mỗi hội thoại một câu đếm. Chấp nhận được với số hội thoại của một
        // người dùng; nếu thành vấn đề thì gộp lại bằng một truy vấn GROUP BY.
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> ConversationResponse.from(c, messages.countByConversationId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse detail(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        List<MessageResponse> history = messages
                .findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(m -> MessageResponse.from(m, parseCitations(m.getCitations())))
                .toList();
        return ConversationDetailResponse.of(conversation, history);
    }

    @Transactional
    public void delete(String userId, Long conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);
        // Xoá con trước rồi mới xoá cha: không có @ManyToOne nên JPA không tự cascade.
        messages.deleteByConversationId(conversation.getId());
        conversations.delete(conversation);
    }

    // ---------------------------------------------------------------------------
    // Một lượt hỏi đáp được tách làm HAI transaction, ở giữa là lời gọi ai-service.
    //
    // Lý do: gọi LLM mất 3–10 giây. Bọc cả lượt trong một @Transactional nghĩa là
    // giam một connection của pool suốt từng ấy thời gian — vài người dùng đồng thời
    // là cạn pool và cả service đứng, dù DB chẳng phải làm gì.
    //
    // Phần điều phối nằm ở ChatTurnService chứ không phải ở đây cũng là có chủ ý:
    // Spring quản lý @Transactional bằng proxy, nên một method trong cùng một bean
    // tự gọi method @Transactional khác sẽ KHÔNG mở transaction nào cả.
    // ---------------------------------------------------------------------------

    /** Dữ liệu sau bước 1, đủ để gọi ai-service mà không cần giữ transaction. */
    public record PreparedTurn(Message userMessage, List<AiRagRequest.AiHistoryItem> history) {
    }

    /** Bước 1: lưu câu hỏi và gom ngữ cảnh. */
    @Transactional
    public PreparedTurn beginTurn(String userId, Long conversationId, String question) {
        Conversation conversation = requireOwned(userId, conversationId);

        // Gom lịch sử TRƯỚC khi thêm câu hỏi mới, nếu không câu hỏi sẽ xuất hiện
        // hai lần trong prompt gửi sang LLM.
        List<Message> previous = messages.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        List<AiRagRequest.AiHistoryItem> history = previous.stream()
                // Chỉ giữ vài lượt gần nhất: prompt càng dài càng tốn token và càng chậm.
                .skip(Math.max(0, previous.size() - HISTORY_LIMIT))
                .map(m -> new AiRagRequest.AiHistoryItem(m.getRole().name().toLowerCase(), m.getContent()))
                .toList();

        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        // Role do server gán, không lấy từ body — xem chú thích ở PostMessageRequest.
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(question);
        Message saved = messages.save(userMessage);

        conversation.touch();
        if (DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(summarize(question));
        }
        // Không cần gọi conversations.save(): trong transaction, entity lấy từ repository
        // đang được JPA quản lý nên mọi thay đổi tự được flush xuống DB (dirty checking).

        return new PreparedTurn(saved, history);
    }

    /** Bước 2: lưu câu trả lời của ai-service. */
    @Transactional
    public Message completeTurn(Long conversationId, String answer, String citationsJson, Integer latencyMs) {
        Message reply = new Message();
        reply.setConversationId(conversationId);
        reply.setRole(MessageRole.ASSISTANT);
        reply.setContent(answer);
        reply.setCitations(citationsJson);
        reply.setLatencyMs(latencyMs);
        Message saved = messages.save(reply);

        conversations.findById(conversationId).ifPresent(Conversation::touch);
        return saved;
    }

    /** Cửa duy nhất để lấy một hội thoại — đã gộp sẵn việc kiểm tra quyền sở hữu. */
    private Conversation requireOwned(String userId, Long conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /** citations lưu dạng chuỗi JSON thô; parse lại để API trả ra mảng thật. */
    private JsonNode parseCitations(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (RuntimeException e) {
            // Dữ liệu hỏng không được làm sập cả cuộc hội thoại — chỉ mất phần trích dẫn.
            log.warn("Không parse được citations đã lưu, bỏ qua: {}", e.getMessage());
            return null;
        }
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

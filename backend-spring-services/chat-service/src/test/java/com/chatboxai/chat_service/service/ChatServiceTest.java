package com.chatboxai.chat_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.chatboxai.chat_service.dao.repository.ConversationRepository;
import com.chatboxai.chat_service.dao.repository.MessageRepository;
import com.chatboxai.chat_service.dto.request.CreateConversationRequestDTO;
import com.chatboxai.chat_service.entity.Conversation;
import com.chatboxai.chat_service.entity.Message;
import com.chatboxai.chat_service.entity.MessageRole;
import com.chatboxai.chat_service.exception.ConversationNotFoundException;
import com.chatboxai.chat_service.service.impl.ChatServiceImpl;

import tools.jackson.databind.ObjectMapper;

class ChatServiceTest {

    private static final String OWNER = "42";

    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final ChatService service = new ChatServiceImpl(conversations, messages, objectMapper);

    private static Conversation conversation(String title) {
        var conversation = new Conversation();
        conversation.setUserId(OWNER);
        conversation.setTitle(title);
        ReflectionTestUtils.setField(conversation, "id", 7L);
        return conversation;
    }

    private static Message message(MessageRole role, String content) {
        var message = new Message();
        message.setConversationId(7L);
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private void echoSavedMessage() {
        when(messages.save(any(Message.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Message captureSavedMessage() {
        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(captor.capture());
        return captor.getValue();
    }

    private void ownedConversation(Conversation conversation) {
        when(conversations.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(conversation));
    }

    @Test
    @DisplayName("Gửi tin nhắn: role do server gán USER, client không quyết định được")
    void serverAssignsUserRole() {
        ownedConversation(conversation("Có sẵn"));
        echoSavedMessage();

        service.beginTurn(OWNER, 7L, "Luật lao động quy định gì?");

        Message saved = captureSavedMessage();
        assertThat(saved.getRole()).isEqualTo(MessageRole.USER);
        assertThat(saved.getConversationId()).isEqualTo(7L);
        assertThat(saved.getContent()).isEqualTo("Luật lao động quy định gì?");
    }

    @Test
    @DisplayName("BẢO MẬT: hội thoại của người khác -> 404, và KHÔNG ghi được gì")
    void cannotTouchSomeoneElsesConversation() {
        when(conversations.findByIdAndUserId(7L, "999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.beginTurn("999", 7L, "cho tôi xem"))
                .isInstanceOf(ConversationNotFoundException.class);

        verify(messages, never()).save(any());
    }

    @Test
    @DisplayName("Lịch sử gửi sang AI KHÔNG chứa chính câu hỏi vừa gửi")
    void historyExcludesTheNewQuestion() {
        ownedConversation(conversation("Có sẵn"));
        when(messages.findByConversationIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(
                        message(MessageRole.USER, "câu cũ"),
                        message(MessageRole.ASSISTANT, "trả lời cũ")));
        echoSavedMessage();

        var prepared = service.beginTurn(OWNER, 7L, "câu hỏi MỚI");

        assertThat(prepared.history()).hasSize(2);
        assertThat(prepared.history()).noneMatch(h -> h.content().equals("câu hỏi MỚI"));
        assertThat(prepared.history().get(0).role()).isEqualTo("user");
        assertThat(prepared.history().get(1).role()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("Tin nhắn đầu tiên trở thành tiêu đề, thay cho tên mặc định")
    void firstMessageBecomesTitle() {
        var conversation = conversation("Hội thoại mới");
        ownedConversation(conversation);
        echoSavedMessage();

        service.beginTurn(OWNER, 7L, "Nghỉ thai sản   được\n bao nhiêu tháng?");

        assertThat(conversation.getTitle()).isEqualTo("Nghỉ thai sản được bao nhiêu tháng?");
    }

    @Test
    @DisplayName("Tiêu đề người dùng tự đặt thì không bị tin nhắn ghi đè")
    void customTitleIsKept() {
        var conversation = conversation("Hỏi về hợp đồng");
        ownedConversation(conversation);
        echoSavedMessage();

        service.beginTurn(OWNER, 7L, "câu hỏi khác hẳn");

        assertThat(conversation.getTitle()).isEqualTo("Hỏi về hợp đồng");
    }

    @Test
    @DisplayName("Lưu câu trả lời: role ASSISTANT kèm citations và thời gian phản hồi")
    void completeTurnStoresAssistantMessage() {
        echoSavedMessage();
        when(conversations.findById(7L)).thenReturn(Optional.of(conversation("Có sẵn")));

        service.completeTurn(7L, "Theo Bộ luật Lao động...", "[{\"law_name\":\"BLLĐ\"}]", 1200);

        Message saved = captureSavedMessage();
        assertThat(saved.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(saved.getContent()).isEqualTo("Theo Bộ luật Lao động...");
        assertThat(saved.getCitations()).contains("BLLĐ");
        assertThat(saved.getLatencyMs()).isEqualTo(1200);
    }

    @Test
    @DisplayName("Tạo hội thoại không có title -> dùng tên mặc định")
    void defaultTitleWhenBlank() {
        when(conversations.save(any(Conversation.class))).thenAnswer(call -> call.getArgument(0));

        service.create(OWNER, new CreateConversationRequestDTO("   "));

        var captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Hội thoại mới");
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER);
    }

    @Test
    @DisplayName("Xoá hội thoại: xoá message TRƯỚC rồi mới xoá hội thoại")
    void deleteRemovesMessagesFirst() {
        var conversation = conversation("Có sẵn");
        ownedConversation(conversation);

        service.delete(OWNER, 7L);

        var order = inOrder(messages, conversations);
        order.verify(messages).deleteByConversationId(7L);
        order.verify(conversations).delete(conversation);
    }

    @Test
    @DisplayName("Danh sách chỉ lấy hội thoại của chính user đó, kèm số tin nhắn")
    void listIsScopedToUser() {
        when(conversations.findByUserIdOrderByUpdatedAtDesc(OWNER))
                .thenReturn(List.of(conversation("Của tôi")));
        when(messages.countByConversationId(7L)).thenReturn(3L);

        var result = service.list(OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Của tôi");
        assertThat(result.get(0).messageCount()).isEqualTo(3);
        verify(conversations).findByUserIdOrderByUpdatedAtDesc(OWNER);
    }
}

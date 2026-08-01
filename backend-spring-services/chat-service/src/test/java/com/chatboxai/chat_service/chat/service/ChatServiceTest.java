package com.chatboxai.chat_service.chat.service;

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

import com.chatboxai.chat_service.chat.dto.CreateConversationRequest;
import com.chatboxai.chat_service.chat.dto.PostMessageRequest;
import com.chatboxai.chat_service.chat.entity.Conversation;
import com.chatboxai.chat_service.chat.entity.Message;
import com.chatboxai.chat_service.chat.entity.MessageRole;
import com.chatboxai.chat_service.chat.repository.ConversationRepository;
import com.chatboxai.chat_service.chat.repository.MessageRepository;

/** Khoá lại nghiệp vụ hội thoại. Repository được mock nên không cần DB. */
class ChatServiceTest {

    private static final String OWNER = "42";

    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final ChatService service = new ChatService(conversations, messages);

    private static Conversation conversation(String title) {
        var conversation = new Conversation();
        conversation.setUserId(OWNER);
        conversation.setTitle(title);
        ReflectionTestUtils.setField(conversation, "id", 7L);
        return conversation;
    }

    /** save() trả lại chính đối tượng vừa nhận, như JPA vẫn làm. */
    private void echoSavedMessage() {
        when(messages.save(any(Message.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Message captureSavedMessage() {
        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messages).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Gửi tin nhắn: role do server gán USER, client không quyết định được")
    void serverAssignsUserRole() {
        when(conversations.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(conversation("Có sẵn")));
        echoSavedMessage();

        service.postMessage(OWNER, 7L, new PostMessageRequest("Luật lao động quy định gì?"));

        Message saved = captureSavedMessage();
        assertThat(saved.getRole()).isEqualTo(MessageRole.USER);
        assertThat(saved.getConversationId()).isEqualTo(7L);
        assertThat(saved.getContent()).isEqualTo("Luật lao động quy định gì?");
    }

    @Test
    @DisplayName("BẢO MẬT: hội thoại của người khác -> 404, và KHÔNG ghi được gì")
    void cannotTouchSomeoneElsesConversation() {
        // Truy vấn có kèm userId nên hội thoại của người khác trả về rỗng.
        when(conversations.findByIdAndUserId(7L, "999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postMessage("999", 7L, new PostMessageRequest("cho tôi xem")))
                .isInstanceOf(ConversationNotFoundException.class);

        verify(messages, never()).save(any());
    }

    @Test
    @DisplayName("Tin nhắn đầu tiên trở thành tiêu đề, thay cho tên mặc định")
    void firstMessageBecomesTitle() {
        var conversation = conversation("Hội thoại mới");
        when(conversations.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(conversation));
        echoSavedMessage();

        service.postMessage(OWNER, 7L, new PostMessageRequest("Nghỉ thai sản   được\n bao nhiêu tháng?"));

        // Xuống dòng và khoảng trắng thừa bị gộp lại thành một dòng gọn.
        assertThat(conversation.getTitle()).isEqualTo("Nghỉ thai sản được bao nhiêu tháng?");
    }

    @Test
    @DisplayName("Tiêu đề người dùng tự đặt thì không bị tin nhắn ghi đè")
    void customTitleIsKept() {
        var conversation = conversation("Hỏi về hợp đồng");
        when(conversations.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(conversation));
        echoSavedMessage();

        service.postMessage(OWNER, 7L, new PostMessageRequest("câu hỏi khác hẳn"));

        assertThat(conversation.getTitle()).isEqualTo("Hỏi về hợp đồng");
    }

    @Test
    @DisplayName("Tạo hội thoại không có title -> dùng tên mặc định")
    void defaultTitleWhenBlank() {
        when(conversations.save(any(Conversation.class))).thenAnswer(call -> call.getArgument(0));

        service.create(OWNER, new CreateConversationRequest("   "));

        var captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversations).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Hội thoại mới");
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER);
    }

    @Test
    @DisplayName("Xoá hội thoại: xoá message TRƯỚC rồi mới xoá hội thoại")
    void deleteRemovesMessagesFirst() {
        var conversation = conversation("Có sẵn");
        when(conversations.findByIdAndUserId(7L, OWNER)).thenReturn(Optional.of(conversation));

        service.delete(OWNER, 7L);

        // Ngược thứ tự sẽ để lại message mồ côi vì không có cascade.
        var order = inOrder(messages, conversations);
        order.verify(messages).deleteByConversationId(7L);
        order.verify(conversations).delete(conversation);
    }

    @Test
    @DisplayName("Danh sách chỉ lấy hội thoại của chính user đó")
    void listIsScopedToUser() {
        when(conversations.findByUserIdOrderByUpdatedAtDesc(OWNER))
                .thenReturn(List.of(conversation("Của tôi")));

        var result = service.list(OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Của tôi");
        verify(conversations).findByUserIdOrderByUpdatedAtDesc(OWNER);
    }
}

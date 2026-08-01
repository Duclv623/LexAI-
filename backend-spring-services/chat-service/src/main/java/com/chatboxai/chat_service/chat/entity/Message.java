package com.chatboxai.chat_service.chat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Một tin nhắn trong hội thoại.
 *
 * Không có cột user_id: chủ sở hữu đã nằm ở Conversation, lưu thêm ở đây sẽ thành
 * hai nguồn sự thật và sớm muộn lệch nhau. Ngoài ra tin nhắn của ASSISTANT thì
 * "user nào gửi" cũng không có nghĩa gì.
 */
@Entity
@Table(name = "messages", indexes = {
        // Đọc message luôn là "của hội thoại X, theo thứ tự thời gian".
        @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Cố ý để kiểu Long thay vì @ManyToOne Conversation: quan hệ thật sẽ kéo theo
     * lazy loading và LazyInitializationException khi map sang DTO ngoài transaction.
     * Ở quy mô này khoá ngoại dạng số đơn giản và dễ đoán hơn nhiều.
     */
    @Column(nullable = false, name = "conversation_id")
    private Long conversationId;

    /**
     * EnumType.STRING chứ tuyệt đối không để mặc định ORDINAL: ORDINAL lưu số thứ tự,
     * chỉ cần chèn thêm một hằng số vào giữa enum là toàn bộ dữ liệu cũ hiểu sai vai trò.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

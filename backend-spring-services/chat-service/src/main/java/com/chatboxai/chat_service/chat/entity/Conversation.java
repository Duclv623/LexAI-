package com.chatboxai.chat_service.chat.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Một phiên hội thoại, thuộc sở hữu của đúng một user.
 *
 * Tên bảng không cần tiền tố: chat-service có database riêng (chatbox_chat) nên
 * không thể trùng tên với bảng của service khác.
 */
@Entity
@Table(name = "conversations", indexes = {
        // Danh sách hội thoại luôn truy vấn theo user và sắp xếp theo lần cập nhật gần nhất.
        @Index(name = "idx_conv_user_updated", columnList = "user_id, updated_at")
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lấy từ claim "sub" của JWT — KHÔNG bao giờ lấy từ body request. */
    @Column(nullable = false, name = "user_id", length = 80)
    private String userId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Đánh dấu hội thoại vừa có hoạt động mới.
     *
     * Cần gọi tay khi thêm message: Message là entity riêng, việc insert nó KHÔNG
     * làm Conversation "bẩn", nên @PreUpdate ở trên sẽ không hề chạy. Đây là chỗ
     * rất dễ tưởng nhầm là JPA tự lo.
     */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.chatboxai.chat_service.entity;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at")
})
@Getter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, name = "conversation_id")
    private Long conversationId;

    // STRING not ORDINAL, ordinal stores positions and breaks when the enum changes
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Setter
    @Column(columnDefinition = "text")
    private String citations;

    @Setter
    @Column(name = "latency_ms")
    private Integer latencyMs;

    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

package com.chatboxai.chat_service.entity;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_user_updated", columnList = "user_id, updated_at")
})
@Getter
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, name = "user_id", length = 80)
    private String userId;

    @Setter
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

    // must be called by hand, inserting a Message does not make this entity dirty
    public void touch() {
        this.updatedAt = Instant.now();
    }
}

package com.chatboxai.auth_service.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Setter
    @Column(nullable = false, name = "password_hash", length = 255)
    private String passwordHash;

    @Setter
    @Column(name = "full_name", length = 120)
    private String fullName;

    @SuppressWarnings("FieldMayBeFinal")
    @Column(nullable = false, length = 30)
    private String role = "USER";

    @SuppressWarnings("FieldMayBeFinal")
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @PrePersist
    @SuppressWarnings("unused")
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

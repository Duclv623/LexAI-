package com.chatboxai.chat_service.dao.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.chat_service.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Page<Conversation> findByUserId(String userId, Pageable pageable);

    // userId in the WHERE clause, so no code path can forget the ownership check
    Optional<Conversation> findByIdAndUserId(Long id, String userId);
}

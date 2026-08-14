package com.chatboxai.chat_service.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.chat_service.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);

    // userId in the WHERE clause, so no code path can forget the ownership check
    Optional<Conversation> findByIdAndUserId(Long id, String userId);
}

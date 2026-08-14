package com.chatboxai.chat_service.dao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.chat_service.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationId(Long conversationId);

    void deleteByConversationId(Long conversationId);
}

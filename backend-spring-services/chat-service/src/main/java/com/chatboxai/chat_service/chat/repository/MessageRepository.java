package com.chatboxai.chat_service.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatboxai.chat_service.chat.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Xoá theo hội thoại. Vì Message không khai báo quan hệ @ManyToOne nên JPA
     * không tự cascade — xoá hội thoại mà quên dòng này sẽ để lại message mồ côi.
     */
    void deleteByConversationId(Long conversationId);
}

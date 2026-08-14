package com.chatboxai.chat_service.dao.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chatboxai.chat_service.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationId(Long conversationId);

    // one grouped query for a whole page instead of one COUNT per conversation
    @Query("""
            select m.conversationId as conversationId, count(m) as total
            from Message m
            where m.conversationId in :ids
            group by m.conversationId
            """)
    List<ConversationMessageCount> countByConversationIdIn(@Param("ids") Collection<Long> ids);

    void deleteByConversationId(Long conversationId);
}

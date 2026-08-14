package com.chatboxai.chat_service.dao.repository;

// projection for the grouped count query, avoids one COUNT per conversation
public interface ConversationMessageCount {

    Long getConversationId();

    long getTotal();
}

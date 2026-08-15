package com.chatboxai.chat_service.dao.repository;

// projection cho truy vấn đếm theo nhóm, tránh phải chạy một COUNT cho mỗi hội thoại
public interface ConversationMessageCount {

    Long getConversationId();

    long getTotal();
}

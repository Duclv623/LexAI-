package com.chatboxai.chat_service.exception;

// also thrown when the conversation belongs to someone else, a 403 would confirm the id exists
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long id) {
        super("Không tìm thấy hội thoại " + id);
    }
}

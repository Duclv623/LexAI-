package com.chatboxai.chat_service.exception;

// cũng ném ra khi hội thoại thuộc về người khác, trả 403 là vô tình xác nhận id đó có tồn tại
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long id) {
        super("Không tìm thấy hội thoại " + id);
    }
}

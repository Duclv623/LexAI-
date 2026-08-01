package com.chatboxai.chat_service.chat.service;

/**
 * Ném ra cho CẢ hai trường hợp: hội thoại không tồn tại, và hội thoại của người khác.
 *
 * Cố ý không tách thành 403 riêng. Trả 403 cho "của người khác" tức là xác nhận với
 * kẻ dò rằng id đó có thật — chỉ cần quét id là biết được hệ thống có bao nhiêu hội
 * thoại và của ai. Trả 404 đồng nhất thì không lộ gì cả.
 */
public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long id) {
        super("Không tìm thấy hội thoại " + id);
    }
}

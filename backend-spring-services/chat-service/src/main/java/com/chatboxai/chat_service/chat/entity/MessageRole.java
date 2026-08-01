package com.chatboxai.chat_service.chat.entity;

/**
 * Vai trò của một tin nhắn trong hội thoại.
 *
 * Dùng enum thay cho String để compiler chặn ngay những giá trị sai chính tả
 * ("assistan", "Assistant"...) — thứ mà kiểu String chỉ phát hiện được lúc chạy.
 */
public enum MessageRole {
    USER,
    ASSISTANT
}

package com.chatboxai.chat_service.chat.dto;

import tools.jackson.databind.JsonNode;

/**
 * Kết quả một lượt hỏi đáp: câu hỏi vừa lưu và câu trả lời vừa sinh.
 *
 * Trả cả hai trong một response vì client cần id thật của tin nhắn user để thay thế
 * tin nhắn tạm nó đang hiển thị lạc quan trên màn hình.
 *
 * retrievedChunks là các đoạn văn bản luật mà RAG lấy ra — chat-service chỉ chuyển
 * tiếp nguyên vẹn, không diễn giải.
 */
public record TurnResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        JsonNode retrievedChunks
) {
}

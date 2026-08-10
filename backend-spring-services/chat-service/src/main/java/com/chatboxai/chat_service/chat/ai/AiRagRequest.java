package com.chatboxai.chat_service.chat.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body gửi sang ai-service. Phải khớp RagRequest trong AIservice/api/schemas.py.
 *
 * Bên Python đặt tên kiểu snake_case nên phải khai báo @JsonProperty tường minh,
 * thay vì đổi naming strategy toàn cục của chat-service (sẽ ảnh hưởng cả API ra FE).
 */
public record AiRagRequest(
        String question,
        List<AiHistoryItem> history,
        @JsonProperty("top_k") int topK
) {
    /** Một lượt hội thoại cũ. role bắt buộc chữ thường: Python khai Literal["user","assistant"]. */
    public record AiHistoryItem(String role, String content) {
    }
}

package com.chatboxai.chat_service.util.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// phải khớp với RagRequest trong AIservice/api/schemas.py, phía python dùng snake_case
public record AiRagRequest(
        String question,
        List<AiHistoryItem> history,
        @JsonProperty("top_k") int topK
) {
    // role phải viết thường, phía python khai báo Literal["user","assistant"]
    public record AiHistoryItem(String role, String content) {
    }
}

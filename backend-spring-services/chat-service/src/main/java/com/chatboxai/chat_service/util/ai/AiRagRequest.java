package com.chatboxai.chat_service.util.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// must match RagRequest in AIservice/api/schemas.py, python side is snake_case
public record AiRagRequest(
        String question,
        List<AiHistoryItem> history,
        @JsonProperty("top_k") int topK
) {
    // role must be lowercase, python declares Literal["user","assistant"]
    public record AiHistoryItem(String role, String content) {
    }
}

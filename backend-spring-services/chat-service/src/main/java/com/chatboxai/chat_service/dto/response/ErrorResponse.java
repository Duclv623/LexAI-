package com.chatboxai.chat_service.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        Map<String, String> fields
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null);
    }
}

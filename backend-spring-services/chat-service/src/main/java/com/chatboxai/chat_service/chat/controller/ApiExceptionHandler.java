package com.chatboxai.chat_service.chat.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import com.chatboxai.chat_service.chat.dto.ApiError;
import com.chatboxai.chat_service.chat.service.ConversationNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ConversationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    /**
     * ai-service lỗi hoặc không phản hồi kịp.
     *
     * Trả 502 chứ không phải 500: lỗi nằm ở dịch vụ phía sau, không phải ở chat-service.
     * Phân biệt được hai loại này giúp lúc đọc log biết ngay phải đi sửa ở đâu.
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleAiFailure(RestClientException e) {
        log.error("Gọi ai-service thất bại", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(HttpStatus.BAD_GATEWAY.value(),
                        "Dịch vụ AI đang không phản hồi. Câu hỏi của bạn đã được lưu, thử lại sau nhé."));
    }

    /** Lỗi từ @Valid: gom theo tên trường để client biết ô nào sai. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "Dữ liệu gửi lên không hợp lệ", fields));
    }
}

package com.chatboxai.chat_service.chat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chatboxai.chat_service.chat.dto.ConversationDetailResponse;
import com.chatboxai.chat_service.chat.dto.ConversationResponse;
import com.chatboxai.chat_service.chat.dto.CreateConversationRequest;
import com.chatboxai.chat_service.chat.dto.PostMessageRequest;
import com.chatboxai.chat_service.chat.dto.TurnResponse;
import com.chatboxai.chat_service.chat.service.ChatService;
import com.chatboxai.chat_service.chat.service.ChatTurnService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat/conversations")
public class ChatController {

    private final ChatService chatService;
    private final ChatTurnService chatTurnService;

    public ChatController(ChatService chatService, ChatTurnService chatTurnService) {
        this.chatService = chatService;
        this.chatTurnService = chatTurnService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateConversationRequest request) {
        return chatService.create(userId(jwt), request);
    }

    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return chatService.list(userId(jwt));
    }

    @GetMapping("/{id}")
    public ConversationDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return chatService.detail(userId(jwt), id);
    }

    /**
     * Gửi câu hỏi và nhận luôn câu trả lời.
     *
     * TẠM THỜI ĐỒNG BỘ: request bị giữ suốt thời gian LLM chạy (3–10 giây). Đây là
     * bước trung gian để có bản chạy được; hướng đi tiếp là trả 202 rồi đẩy câu trả
     * lời về sau qua SSE hoặc hàng đợi.
     */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnResponse postMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody PostMessageRequest request) {
        // getTokenValue() là chuỗi token GỐC, truyền tiếp sang ai-service để nó tự
        // verify — chat-service không tự phong cho mình quyền nói thay người dùng.
        return chatTurnService.send(userId(jwt), jwt.getTokenValue(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        chatService.delete(userId(jwt), id);
    }

    /**
     * Danh tính user lấy từ claim "sub" của token ĐÃ ĐƯỢC VERIFY.
     *
     * Gateway cũng có bơm header X-User-Id xuống, nhưng ở đây cố tình không đụng
     * tới nó: header thì ai gọi thẳng cổng 8082 cũng đặt được, còn chữ ký RS256
     * thì không giả nổi nếu không có private key. Đây chính là ý nghĩa của
     * zero-trust — chat-service không tin gateway, nó tự kiểm.
     */
    private static String userId(Jwt jwt) {
        return jwt.getSubject();
    }
}

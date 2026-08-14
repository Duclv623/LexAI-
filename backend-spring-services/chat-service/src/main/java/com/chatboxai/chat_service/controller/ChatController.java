package com.chatboxai.chat_service.controller;

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

import com.chatboxai.chat_service.dto.request.CreateConversationRequestDTO;
import com.chatboxai.chat_service.dto.request.PostMessageRequestDTO;
import com.chatboxai.chat_service.dto.response.ConversationDetailResponseDTO;
import com.chatboxai.chat_service.dto.response.ConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.CustomResponse;
import com.chatboxai.chat_service.dto.response.TurnResponseDTO;
import com.chatboxai.chat_service.service.ChatService;
import com.chatboxai.chat_service.service.ChatTurnService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatTurnService chatTurnService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomResponse<ConversationResponseDTO> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateConversationRequestDTO request) {
        return new CustomResponse<>(true, chatService.create(userId(jwt), request), "Tạo hội thoại thành công");
    }

    @GetMapping
    public CustomResponse<List<ConversationResponseDTO>> list(@AuthenticationPrincipal Jwt jwt) {
        return new CustomResponse<>(true, chatService.list(userId(jwt)), "Lấy danh sách hội thoại thành công");
    }

    @GetMapping("/{id}")
    public CustomResponse<ConversationDetailResponseDTO> detail(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return new CustomResponse<>(true, chatService.detail(userId(jwt), id), "Lấy hội thoại thành công");
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomResponse<TurnResponseDTO> postMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody PostMessageRequestDTO request) {
        // forward the raw token so ai-service can verify it itself
        return new CustomResponse<>(true,
                chatTurnService.send(userId(jwt), jwt.getTokenValue(), id, request),
                "Gửi tin nhắn thành công");
    }

    // 204 has no body by definition, so this one stays unwrapped
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        chatService.delete(userId(jwt), id);
    }

    // identity comes from the verified token, never from the X-User-Id header
    private static String userId(Jwt jwt) {
        return jwt.getSubject();
    }
}

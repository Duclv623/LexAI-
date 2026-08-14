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
    public ConversationResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateConversationRequestDTO request) {
        return chatService.create(userId(jwt), request);
    }

    @GetMapping
    public List<ConversationResponseDTO> list(@AuthenticationPrincipal Jwt jwt) {
        return chatService.list(userId(jwt));
    }

    @GetMapping("/{id}")
    public ConversationDetailResponseDTO detail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return chatService.detail(userId(jwt), id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnResponseDTO postMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody PostMessageRequestDTO request) {
        // forward the raw token so ai-service can verify it itself
        return chatTurnService.send(userId(jwt), jwt.getTokenValue(), id, request);
    }

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

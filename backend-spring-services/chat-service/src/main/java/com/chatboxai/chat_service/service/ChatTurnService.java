package com.chatboxai.chat_service.service;

import com.chatboxai.chat_service.dto.request.PostMessageRequestDTO;
import com.chatboxai.chat_service.dto.response.TurnResponseDTO;

public interface ChatTurnService {

    TurnResponseDTO send(String userId, String bearerToken, Long conversationId, PostMessageRequestDTO request);
}

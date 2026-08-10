package com.chatboxai.chat_service.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chatboxai.chat_service.chat.service.ChatService;
import com.chatboxai.chat_service.chat.service.ChatTurnService;
import com.chatboxai.chat_service.chat.service.ConversationNotFoundException;
import com.chatboxai.chat_service.config.SecurityConfig;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatTurnService chatTurnService;

    /** SecurityConfig cần một JwtDecoder; test dùng post-processor jwt() nên nó không bị gọi. */
    @MockitoBean
    @SuppressWarnings("unused") // Spring injects this mock into the test ApplicationContext.
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Không có token -> 401, không chạm tới service")
    void withoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Có token -> service nhận đúng userId từ claim sub")
    void userIdComesFromSubjectClaim() throws Exception {
        when(chatService.list("42")).thenReturn(List.of());

        mvc.perform(get("/api/chat/conversations").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        verify(chatService).list("42");
    }

    @Test
    @DisplayName("CHỐNG GIẢ MẠO: header X-User-Id do client gửi bị bỏ qua, vẫn dùng sub của token")
    void spoofedHeaderIsIgnored() throws Exception {
        when(chatService.list("42")).thenReturn(List.of());

        mvc.perform(get("/api/chat/conversations")
                        .header("X-User-Id", "999")
                        .with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        // Nếu controller đọc header thay vì token thì dòng này sẽ đỏ.
        verify(chatService).list("42");
    }

    @Test
    @DisplayName("Nội dung rỗng -> 400 kèm tên trường sai")
    void blankContentIsRejected() throws Exception {
        mvc.perform(post("/api/chat/conversations/7/messages")
                        .with(jwt().jwt(j -> j.subject("42")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.content").exists());
    }

    @Test
    @DisplayName("Gửi tin nhắn: token GỐC được chuyển tiếp xuống để gọi ai-service")
    void rawTokenIsForwarded() throws Exception {
        mvc.perform(post("/api/chat/conversations/7/messages")
                        .with(jwt().jwt(j -> j.subject("42").tokenValue("token-goc-cua-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Nghỉ thai sản mấy tháng?\",\"provider\":\"gemini\"}"))
                .andExpect(status().isCreated());

        // Nếu chat-service tự ký token mới hoặc bỏ trống, ai-service sẽ không biết ai hỏi.
        verify(chatTurnService).send(eq("42"), eq("token-goc-cua-user"), eq(7L), any());
    }

    @Test
    @DisplayName("Hội thoại không thuộc về mình -> 404 với thân JSON thống nhất")
    void notFoundIsMappedTo404() throws Exception {
        when(chatService.detail(eq("42"), any()))
                .thenThrow(new ConversationNotFoundException(7L));

        mvc.perform(get("/api/chat/conversations/7").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}

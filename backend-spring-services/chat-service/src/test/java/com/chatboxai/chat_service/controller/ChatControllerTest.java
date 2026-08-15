package com.chatboxai.chat_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.chatboxai.chat_service.config.SecurityConfig;
import com.chatboxai.chat_service.dto.response.ListConversationResponseDTO;
import com.chatboxai.chat_service.dto.response.PaginationDTO;
import com.chatboxai.chat_service.exception.ApiExceptionHandler;
import com.chatboxai.chat_service.exception.ConversationNotFoundException;
import com.chatboxai.chat_service.service.ChatService;
import com.chatboxai.chat_service.service.ChatTurnService;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ChatTurnService chatTurnService;

    // SecurityConfig có bật oauth2ResourceServer, mà @WebMvcTest lại không tự cấu hình decoder
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static ListConversationResponseDTO emptyPage() {
        return new ListConversationResponseDTO(List.of(), new PaginationDTO(0, 0, 0, 20));
    }

    @Test
    @DisplayName("Không có token -> 401, không chạm tới service")
    void withoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtDecoder, chatService);
    }

    @Test
    @DisplayName("Có token -> service nhận đúng userId từ claim sub")
    void userIdComesFromSubjectClaim() throws Exception {
        when(chatService.list(eq("42"), anyInt(), anyInt())).thenReturn(emptyPage());

        mvc.perform(get("/api/chat/conversations").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        verify(chatService).list(eq("42"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Không truyền page/size -> dùng mặc định trang 0, cỡ 20")
    void paginationDefaultsAreApplied() throws Exception {
        when(chatService.list(eq("42"), anyInt(), anyInt())).thenReturn(emptyPage());

        mvc.perform(get("/api/chat/conversations").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        verify(chatService).list("42", 0, 20);
    }

    @Test
    @DisplayName("Truyền page/size -> chuyển đúng xuống service")
    void paginationParamsArePassedThrough() throws Exception {
        when(chatService.list(eq("42"), anyInt(), anyInt())).thenReturn(emptyPage());

        mvc.perform(get("/api/chat/conversations?page=2&size=5")
                        .with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        verify(chatService).list("42", 2, 5);
    }

    @Test
    @DisplayName("CHỐNG GIẢ MẠO: header X-User-Id do client gửi bị bỏ qua, vẫn dùng sub của token")
    void spoofedHeaderIsIgnored() throws Exception {
        when(chatService.list(eq("42"), anyInt(), anyInt())).thenReturn(emptyPage());

        mvc.perform(get("/api/chat/conversations")
                        .header("X-User-Id", "999")
                        .with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(status().isOk());

        verify(chatService).list(eq("42"), anyInt(), anyInt());
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
                        .content("{\"content\":\"Nghỉ thai sản mấy tháng?\"}"))
                .andExpect(status().isCreated());

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

package com.chatboxai.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import jakarta.servlet.http.HttpServletRequest;

class JwtAuthFilterTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtDecoder);

    private static Jwt validJwt() {
        return Jwt.withTokenValue("good-token")
                .header("alg", "RS256")
                .subject("1")
                .claim("role", "USER")
                .claim("email", "levan@example.com")
                .build();
    }

    @Test
    @DisplayName("Path được bảo vệ, không có token -> 401 và KHÔNG đi tiếp")
    void protectedPathWithoutToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/chat/messages");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        // a null chain.getRequest() means the filter blocked and did not forward
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("Token rác -> 401")
    void protectedPathWithBadToken() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("chữ ký sai"));

        var request = new MockHttpServletRequest("GET", "/api/chat/messages");
        request.addHeader("Authorization", "Bearer rubbish");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("CHỐNG GIẢ MẠO: client tự khai X-User-Id bị thay bằng giá trị lấy từ token")
    void spoofedIdentityHeaderIsOverwritten() throws Exception {
        when(jwtDecoder.decode("good-token")).thenReturn(validJwt());

        var request = new MockHttpServletRequest("GET", "/api/chat/messages");
        request.addHeader("Authorization", "Bearer good-token");
        request.addHeader("X-User-Id", "999");       // giả mạo
        request.addHeader("X-User-Role", "ADMIN");   // giả mạo
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader("X-User-Id")).isEqualTo("1");        // không phải 999
        assertThat(forwarded.getHeader("X-User-Role")).isEqualTo("USER");   // không phải ADMIN
        assertThat(forwarded.getHeader("X-User-Email")).isEqualTo("levan@example.com");
    }

    @Test
    @DisplayName("Path public: qua được không cần token, nhưng header giả vẫn bị XOÁ")
    void publicPathStripsSpoofedHeaders() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("X-User-Id", "999"); // giả mạo
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded).isNotNull();               // được đi tiếp
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(forwarded.getHeader("X-User-Id")).isNull(); // đã bị xoá sạch
    }

    @Test
    @DisplayName("So khớp path public là EXACT: /api/auth/login/x không được coi là public")
    void publicPathMatchIsExact() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/login/extra");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401); // fail-closed
    }
}

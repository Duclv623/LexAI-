package com.chatboxai.chat_service.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check nhẹ, không thuộc feature nào nên nằm ở web/ chứ không ở chat/.
 *
 * Giữ đường dẫn /api/chat/health vì gateway đã whitelist nó làm public path
 * (xem publicPaths trong JwtAuthFilter) và rate limit miễn trừ mọi path kết thúc
 * bằng /health.
 */
@RestController
public class HealthController {

    @GetMapping("/api/chat/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

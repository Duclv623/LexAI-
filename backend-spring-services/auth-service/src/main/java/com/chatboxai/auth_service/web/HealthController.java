package com.chatboxai.auth_service.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check nhẹ, không thuộc feature nào nên nằm ở web/ chứ không ở auth/.
 *
 * Giữ đường dẫn /api/auth/health vì SecurityConfig đã cho phép nó không cần token,
 * và rate limit ở gateway miễn trừ mọi path kết thúc bằng /health.
 */
@RestController
public class HealthController {

    @GetMapping("/api/auth/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

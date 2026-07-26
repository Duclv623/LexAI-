package com.chatboxai.api_gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS cho gateway — bắt buộc phải có nếu frontend (Next.js :3000) gọi thẳng vào :8080.
 *
 * Vì sao phải chạy TRƯỚC hai filter kia:
 * Browser gửi preflight OPTIONS trước mỗi request có Authorization header, mà preflight
 * KHÔNG mang theo Authorization. Nếu JwtAuthFilter chạy trước, nó thấy thiếu token và
 * trả 401 không kèm Access-Control-Allow-Origin → browser chặn, không request nào đi được.
 * CorsFilter tự trả lời preflight và dừng chain tại đó, nên OPTIONS không bao giờ chạm
 * tới rate limit (+10) hay JWT (+20) — vừa đúng, vừa không tốn budget rate limit.
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Đúng những header frontend gửi: Content-Type (JSON) và Authorization (Bearer).
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Token nằm ở localStorage và đi qua header Authorization, KHÔNG dùng cookie,
        // nên không cần allowCredentials. Để false cũng an toàn hơn.
        config.setAllowCredentials(false);
        // Cache kết quả preflight 1 giờ, đỡ phải OPTIONS lại trước mỗi request.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // trước RedisRateLimitFilter (+10)
        return registration;
    }
}

package com.chatboxai.api_gateway.config;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // chặn flood trước, rồi mới tới JwtAuthFilter
public class RedisRateLimitFilter extends OncePerRequestFilter {

    /**
     * INCR và EXPIRE phải nằm trong MỘT lệnh atomic.
     *
     * Nếu tách thành 2 round trip, chỉ request nào thấy count == 1 mới set TTL.
     * Tiến trình chết (Ctrl+C khi dev) đúng khoảng giữa 2 lệnh → key sống với TTL = -1:
     * count không bao giờ quay lại 1 nên không request nào set TTL nữa, và IP đó
     * bị 429 vĩnh viễn cho tới khi DEL thủ công trong redis-cli.
     *
     * Lua chạy nguyên khối trong Redis nên không thể đứt giữa chừng — và tiết kiệm 1 round trip.
     */
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return c
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RedisRateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitProperties.Rule rule = resolveRule(path);

        if (rule == null || isHealthCheck(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "rate-limit:%s:%s".formatted(rule.getName(), clientIp(request));

        Long requests;
        try {
            requests = redisTemplate.execute(
                    INCR_WITH_TTL,
                    List.of(key),
                    String.valueOf(rule.getWindowSeconds())
            );
        } catch (DataAccessException e) {
            // Redis chết KHÔNG được phép làm sập gateway: rate limit là lớp bảo vệ,
            // không phải nghiệp vụ. Chọn fail-open một cách CÓ Ý THỨC + log cảnh báo,
            // thay vì để exception bay ra ngoài thành 500 khó hiểu cho mọi route.
            logger.warn("Redis không khả dụng — tạm bỏ qua rate limit cho " + path, e);
            filterChain.doFilter(request, response);
            return;
        }

        if (requests != null && requests > rule.getLimit()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Too many requests"}""");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Khớp prefix DÀI NHẤT, không phải rule đầu tiên trong file config.
     * Nhờ vậy có thể đặt rule cụ thể (/api/auth/login) cạnh rule chung (/api/auth/)
     * mà kết quả không phụ thuộc thứ tự khai báo trong YAML.
     */
    private RateLimitProperties.Rule resolveRule(String path) {
        return properties.getRules().stream()
                .filter(rule -> path.startsWith(rule.getPathPrefix()))
                .max(Comparator.comparingInt(rule -> rule.getPathPrefix().length()))
                .orElse(null);
    }

    /**
     * Health check bị monitor / docker healthcheck poll liên tục (12 req/phút là bình thường).
     * Không cho nó ăn budget chung, nếu không chính health probe sẽ tự đẩy user vào 429.
     */
    private boolean isHealthCheck(String path) {
        return path.endsWith("/health");
    }

    /**
     * Gateway này CHÍNH LÀ edge — không có reverse proxy nào đứng trước, nên
     * X-Forwarded-For là dữ liệu client tự khai và không được tin: đổi header là
     * đổi bucket (né sạch limit), hoặc điền IP người khác để khoá họ khỏi login.
     *
     * Khi nào thật sự có LB/nginx phía trước thì ĐỪNG tự parse header ở đây. Bật:
     *   server.forward-headers-strategy: native
     *   server.tomcat.remoteip.internal-proxies: <CIDR của LB>
     * Tomcat sẽ lo phần đó và getRemoteAddr() dưới đây trả về IP thật của client.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}

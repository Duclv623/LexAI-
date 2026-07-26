package com.chatboxai.api_gateway.config;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Gắn cho mỗi request một mã định danh duy nhất (correlation ID) để lần vết xuyên hệ thống.
 *
 * Ba việc nó làm:
 *  1. Đặt ID vào MDC → MỌI dòng log phát ra trong request này tự động mang theo ID,
 *     không phải truyền biến qua từng hàm (xem logging.pattern.level trong application.yaml).
 *  2. Ghi ID vào request header → downstream (auth/chat/ai service) nhận cùng một ID,
 *     nên log của 4 service ghép lại được thành một dòng thời gian duy nhất.
 *  3. Trả ID về client qua response header → user báo lỗi kèm ID là tra log ra ngay.
 *
 * Thứ tự filter (+5): sau CorsFilter (preflight OPTIONS là chuyện của browser, không cần ID)
 * nhưng TRƯỚC rate limit (+10) và JWT (+20) — để chính những request bị chặn 429/401
 * cũng có ID, vì đó mới là loại request hay phải đi tra.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Khoá trong MDC — phải khớp %X{requestId} ở logging.pattern.level. */
    private static final String MDC_KEY = "requestId";

    /**
     * Chỉ nhận lại ID client gửi nếu nó "sạch".
     * Giá trị client tự khai đi thẳng vào file log, nên một chuỗi có ký tự xuống dòng
     * có thể NGUỴ TẠO nguyên một dòng log giả (log injection). Chặn ngay từ đây.
     */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startNs = System.nanoTime();

        try {
            MDC.put(MDC_KEY, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(
                    new MutatedHeadersRequest(request, Map.of(REQUEST_ID_HEADER, requestId)),
                    response
            );
        } finally {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("%s %s -> %d (%d ms)".formatted(
                    request.getMethod(), request.getRequestURI(), response.getStatus(), ms));

            // BẮT BUỘC. MDC gắn theo thread, mà Tomcat trả thread về pool để dùng lại.
            // Không clear thì thread mang theo ID cũ như rác, và mọi dòng log phát ra
            // NGOÀI phạm vi filter này sẽ đeo nhầm ID của request đã kết thúc từ lâu:
            //   - preflight OPTIONS bị CorsFilter (order 0) chặn trước, không bao giờ vào đây;
            //   - bất cứ filter nào chạy trước order +5;
            //   - công việc async/scheduled chạy trên cùng pool.
            // Log trông vẫn bình thường, chỉ là gán sai request — loại bug tốn cả buổi để tìm.
            MDC.clear();
        }
    }

    /**
     * Có ID hợp lệ từ upstream thì giữ nguyên (để nối được trace xuyên hệ thống),
     * không thì sinh mới. Gateway là điểm đầu tiên nên phần lớn là sinh mới.
     */
    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}

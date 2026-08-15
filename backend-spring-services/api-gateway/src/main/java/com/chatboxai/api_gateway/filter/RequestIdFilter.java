package com.chatboxai.api_gateway.filter;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    // phải trùng với %X{requestId} khai báo trong logging.pattern.level
    private static final String MDC_KEY = "requestId";

    // từ chối mọi giá trị khác, một ký tự xuống dòng do client gửi có thể giả mạo trọn một dòng log
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

            // bắt buộc, tomcat tái sử dụng thread mà MDC lại gắn theo thread
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}

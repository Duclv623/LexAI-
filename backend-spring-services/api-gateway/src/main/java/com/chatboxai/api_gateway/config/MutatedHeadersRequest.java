package com.chatboxai.api_gateway.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Bọc request để ép giá trị một số header TRƯỚC KHI gateway forward xuống downstream:
 *  - value == null  → coi như header không tồn tại (xoá header client tự gửi).
 *  - value != null  → trả về đúng giá trị ta đặt (ghi đè client).
 * So khớp tên header không phân biệt hoa/thường (đúng chuẩn HTTP).
 *
 * Dùng chung bởi:
 *  - JwtAuthFilter   → xoá/ghi đè X-User-Id, X-User-Role, X-User-Email
 *  - RequestIdFilter → đặt X-Request-Id
 *
 * Hai filter lồng nhau vẫn đúng: wrapper ngoài chỉ chặn những header nó khai,
 * còn lại rơi xuống super — tức là wrapper bên trong. Nên X-Request-Id do
 * RequestIdFilter (order +5) đặt vẫn sống sót qua JwtAuthFilter (order +20).
 */
class MutatedHeadersRequest extends HttpServletRequestWrapper {

    private final Map<String, String> overrides; // key đã lowercase

    MutatedHeadersRequest(HttpServletRequest request, Map<String, String> overrides) {
        super(request);
        this.overrides = new HashMap<>();
        overrides.forEach((k, v) -> this.overrides.put(k.toLowerCase(), v));
    }

    @Override
    public String getHeader(String name) {
        String key = name.toLowerCase();
        if (overrides.containsKey(key)) {
            return overrides.get(key); // có thể null → header bị xoá
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String key = name.toLowerCase();
        if (overrides.containsKey(key)) {
            String value = overrides.get(key);
            return value == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(value));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // Bỏ các header bị override khỏi danh sách gốc, rồi thêm lại những cái có giá trị.
        List<String> names = new ArrayList<>();
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            String n = original.nextElement();
            if (!overrides.containsKey(n.toLowerCase())) {
                names.add(n);
            }
        }
        overrides.forEach((k, v) -> {
            if (v != null) {
                names.add(k);
            }
        });
        return Collections.enumeration(names);
    }
}

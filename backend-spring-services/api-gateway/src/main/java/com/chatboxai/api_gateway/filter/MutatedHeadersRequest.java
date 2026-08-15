package com.chatboxai.api_gateway.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

// giá trị null = xoá header đi, khác null = ghi đè lên giá trị client gửi lên
class MutatedHeadersRequest extends HttpServletRequestWrapper {

    private final Map<String, String> overrides;

    MutatedHeadersRequest(HttpServletRequest request, Map<String, String> overrides) {
        super(request);
        this.overrides = new HashMap<>();
        overrides.forEach((k, v) -> this.overrides.put(k.toLowerCase(), v));
    }

    @Override
    public String getHeader(String name) {
        String key = name.toLowerCase();
        if (overrides.containsKey(key)) {
            return overrides.get(key);
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

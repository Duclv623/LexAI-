package com.chatboxai.chat_service.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.chatboxai.chat_service.dto.response.PaginationDTO;

@Component
public class PageableUtils {

    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 20;

    // clamp the page size, size comes from the query string and a client could ask for millions
    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }

    public static PaginationDTO toPagination(Page<?> page) {
        return new PaginationDTO(
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize());
    }
}

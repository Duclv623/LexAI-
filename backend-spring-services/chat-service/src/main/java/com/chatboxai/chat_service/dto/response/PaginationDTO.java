package com.chatboxai.chat_service.dto.response;

public record PaginationDTO(
        int totalPages,
        long totalElements,
        int currentPage,
        int pageSize
) {
}

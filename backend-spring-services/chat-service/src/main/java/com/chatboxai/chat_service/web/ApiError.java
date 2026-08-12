package com.chatboxai.chat_service.web;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Thân phản hồi cho mọi lỗi, để client luôn parse được một hình dạng duy nhất
 * thay vì lúc thì HTML trang lỗi, lúc thì JSON mặc định của Spring.
 *
 * fields chỉ có mặt khi lỗi validate; NON_NULL để nó biến mất hẳn khỏi JSON
 * ở các lỗi khác chứ không trả về "fields": null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String message,
        Map<String, String> fields
) {
    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null);
    }
}

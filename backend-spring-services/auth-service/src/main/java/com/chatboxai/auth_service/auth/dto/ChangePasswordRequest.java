package com.chatboxai.auth_service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đổi mật khẩu.
 *
 * Bắt buộc gửi kèm mật khẩu hiện tại dù người dùng đã đăng nhập: token có thể bị
 * lấy cắp (nó nằm trong localStorage), nên phải xác nhận lại bằng thứ chỉ chủ tài
 * khoản mới biết trước khi cho đổi.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
        String newPassword
) {
}

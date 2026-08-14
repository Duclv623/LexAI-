package com.chatboxai.auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
        String newPassword
) {
}

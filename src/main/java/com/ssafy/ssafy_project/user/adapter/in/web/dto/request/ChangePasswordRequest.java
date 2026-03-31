package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호 입력은 필수입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호 입력은 필수입니다.")
        @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상 100자 이하여야 합니다.")
        String newPassword
) {
}

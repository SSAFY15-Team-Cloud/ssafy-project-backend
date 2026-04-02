package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest (
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하여야 합니다.")
        String nickname,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 15, message = "이름은 15자 이하여야 합니다.")
        String name
) {
}

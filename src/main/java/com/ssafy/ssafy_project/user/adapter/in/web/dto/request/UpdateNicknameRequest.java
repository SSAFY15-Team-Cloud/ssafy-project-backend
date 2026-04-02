package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 15, message = "닉네임은 2자 이상 15자 이하여야 합니다.")
        String nickname
) {
}

package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeProfileImageRequest(
        @NotBlank(message = "Object Key는 필수입니다")
        String objectKey
) {
}

package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}

package com.ssafy.ssafy_project.user.application.port.in;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {
}

package com.ssafy.ssafy_project.user.application.port.in;

public record ChangeProfileImageCommand(
        Long userId,
        String objectKey
) {
}

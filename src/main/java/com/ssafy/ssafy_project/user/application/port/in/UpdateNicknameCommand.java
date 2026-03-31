package com.ssafy.ssafy_project.user.application.port.in;

public record UpdateNicknameCommand(
        Long userId,
        String nickname
) {
}

package com.ssafy.ssafy_project.user.application.port.in;

public record MyInfoResult(
        Long userId,
        String email,
        String nickname,
        String name,
        String profileImageUrl
) {
}

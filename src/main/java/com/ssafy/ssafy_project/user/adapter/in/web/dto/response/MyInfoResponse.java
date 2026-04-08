package com.ssafy.ssafy_project.user.adapter.in.web.dto.response;

public record MyInfoResponse(
    Long userId,
    String email,
    String nickname,
    String name,
    String profileImageKey
) {
}

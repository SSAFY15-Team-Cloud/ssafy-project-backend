package com.ssafy.ssafy_project.user.adapter.in.web.dto.request;

public record SignUpRequest (
        String email,
        String password,
        String nickname,
        String name
) {
}

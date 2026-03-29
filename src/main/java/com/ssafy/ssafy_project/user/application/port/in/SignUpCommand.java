package com.ssafy.ssafy_project.user.application.port.in;

public record SignUpCommand(
    String email,
    String password,
    String nickname,
    String name
) {
}

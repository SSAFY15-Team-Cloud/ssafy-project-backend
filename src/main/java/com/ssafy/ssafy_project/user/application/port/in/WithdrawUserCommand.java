package com.ssafy.ssafy_project.user.application.port.in;

public record WithdrawUserCommand(
        Long userId,
        String password
) {
}

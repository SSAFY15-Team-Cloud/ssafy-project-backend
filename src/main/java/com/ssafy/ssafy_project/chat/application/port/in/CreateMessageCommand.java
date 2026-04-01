package com.ssafy.ssafy_project.chat.application.port.in;

public record CreateMessageCommand(
        Long roomId,
        String message,
        Long userId
) {
}

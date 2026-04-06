package com.ssafy.ssafy_project.chat.application.port.in;

public record DeleteMessageCommand(
        Long messageId,
        Long userId
) {
}

package com.ssafy.ssafy_project.chat.application.port.in;

public record GetMessagesCommand(
        Long roomId,
        Long userId
) {
}

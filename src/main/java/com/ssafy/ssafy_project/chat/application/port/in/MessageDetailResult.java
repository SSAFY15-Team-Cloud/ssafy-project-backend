package com.ssafy.ssafy_project.chat.application.port.in;

import java.time.LocalDateTime;

public record MessageDetailResult(
        Long messageId,
        Long senderId,
        String senderNickname,
        String message,
        LocalDateTime createdTime
) {
}

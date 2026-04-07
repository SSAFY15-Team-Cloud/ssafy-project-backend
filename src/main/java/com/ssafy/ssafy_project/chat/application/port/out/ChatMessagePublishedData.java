package com.ssafy.ssafy_project.chat.application.port.out;

import java.time.LocalDateTime;

public record ChatMessagePublishedData(
        Long roomId,
        Long messageId,
        Long senderId,
        String senderNickname,
        String message,
        LocalDateTime createdTime
) {
}

package com.ssafy.ssafy_project.chat.adapter.out.messaging.dto.response;

import java.time.LocalDateTime;

public record ChatMessagePayload(
        Long messageId,
        Long senderId,
        String senderNickname,
        String message,
        LocalDateTime createdTime
) {
}

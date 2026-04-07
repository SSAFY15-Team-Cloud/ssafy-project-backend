package com.ssafy.ssafy_project.chat.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record MessageDetailResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        String message,
        LocalDateTime createdTime
) {
}

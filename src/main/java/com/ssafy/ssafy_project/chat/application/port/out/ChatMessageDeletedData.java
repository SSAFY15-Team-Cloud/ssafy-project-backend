package com.ssafy.ssafy_project.chat.application.port.out;

public record ChatMessageDeletedData(
        Long roomId,
        Long messageId
) {
}

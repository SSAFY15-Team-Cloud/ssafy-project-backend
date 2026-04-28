package com.ssafy.ssafy_project.notification.application.port.in;

import com.ssafy.ssafy_project.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record GetNotificationResult(
        Long id,
        NotificationType type,
        String content,
        LocalDateTime createdTime,
        boolean isRead,
        NotificationPayload payload
) {
}

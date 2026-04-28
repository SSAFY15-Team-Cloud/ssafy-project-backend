package com.ssafy.ssafy_project.notification.application.port.in;

public record GetNotificationCommand(
        Long userId,
        boolean isRead
) {
}

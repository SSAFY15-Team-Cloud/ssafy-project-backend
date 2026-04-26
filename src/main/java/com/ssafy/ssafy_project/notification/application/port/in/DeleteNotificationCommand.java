package com.ssafy.ssafy_project.notification.application.port.in;

public record DeleteNotificationCommand(
        Long userId,
        Long notificationId
) {
}

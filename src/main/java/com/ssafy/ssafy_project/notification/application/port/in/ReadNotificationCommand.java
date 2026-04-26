package com.ssafy.ssafy_project.notification.application.port.in;

public record ReadNotificationCommand(
        Long userId,
        Long notificationId
) {
}

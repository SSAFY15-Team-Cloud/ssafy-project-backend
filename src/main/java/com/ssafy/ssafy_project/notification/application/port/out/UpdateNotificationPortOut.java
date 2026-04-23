package com.ssafy.ssafy_project.notification.application.port.out;

import java.time.LocalDateTime;

public interface UpdateNotificationPortOut {
    void markAsRead(Long notificationId, Long toId, LocalDateTime readAt);
    void markAllAsRead(Long toId, LocalDateTime readAt);
}

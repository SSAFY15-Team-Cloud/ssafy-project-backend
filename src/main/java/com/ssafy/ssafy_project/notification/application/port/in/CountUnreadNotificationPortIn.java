package com.ssafy.ssafy_project.notification.application.port.in;

public interface CountUnreadNotificationPortIn {
    long countUnreadNotifications(Long userId);
}

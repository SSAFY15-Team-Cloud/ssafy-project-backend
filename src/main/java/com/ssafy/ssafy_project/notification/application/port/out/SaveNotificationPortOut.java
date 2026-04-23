package com.ssafy.ssafy_project.notification.application.port.out;

import com.ssafy.ssafy_project.notification.domain.Notification;

public interface SaveNotificationPortOut {
    Notification save(Notification notification);
}

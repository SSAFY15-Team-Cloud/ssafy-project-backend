package com.ssafy.ssafy_project.notification.application.port.out;

import com.ssafy.ssafy_project.notification.domain.Notification;

import java.util.List;
import java.util.Optional;

public interface LoadNotificationPortOut {
    List<Notification> loadAllByToId(Long toId);
    List<Notification> loadUnreadByToId(Long toId);
    long countUnreadByToId(Long toId);
    Optional<Notification> loadByIdAndToId(Long notificationId, Long toId);
}

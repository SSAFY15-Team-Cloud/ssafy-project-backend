package com.ssafy.ssafy_project.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Notification {
    private final Long id;
    private NotificationType type;
    private LocalDateTime readAt;
    private String content;
    private Long toId;
    private Long fromId;
    private LocalDateTime createdTime;
    private Long roomId;
    private Long reportId;
    private String roomCode;

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead(LocalDateTime readTime) {
        if(this.readAt == null) {
            this.readAt = readTime;
        }
    }
}

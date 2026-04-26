package com.ssafy.ssafy_project.notification.application.port.in;

public interface ReadNotificationPortIn {
    void read(ReadNotificationCommand command);
    void readAll(Long userId);
}

package com.ssafy.ssafy_project.notification.application.port.in;

import java.util.List;

public interface GetNotificationPortIn {
    List<GetNotificationResult> getNotifications(GetNotificationCommand command);
}

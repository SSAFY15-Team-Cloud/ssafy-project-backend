package com.ssafy.ssafy_project.notification.application.port.in;

public record ReportDonePayload(
        Long reportId
) implements NotificationPayload{
}
